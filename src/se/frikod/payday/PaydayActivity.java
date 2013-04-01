/*
 *
 *   Copyright (C) 2012-2013 Joakim Lundborg <joakim,lundborg@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package se.frikod.payday;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

public class PaydayActivity extends FragmentActivity {

    SharedPreferences prefs;
    private Budget budget;
    private double currentBudget = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        BankdroidProvider bank = new BankdroidProvider(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        budget = new Budget(bank, this, new Holidays(this));

        super.onCreate(savedInstanceState);

        setContentView(R.layout.payday_activity);

        FontUtils.setRobotoFont(this, this.getWindow().getDecorView());

        if (bank.verifySetup()) {
            Log.i("Payday", "Verify setup failed");
            update();
        } else {
            runSetup();
        }

        TextView bv = (TextView) findViewById(R.id.budgetNumber);
        bv.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                update();
            }
        });

        updateBudgetItems();


    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_budget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void runSetup() {
        Intent intent;
        intent = new Intent(this, SetupActivity.class);
        startActivity(intent);
        finish();
    }

    private void updateBudgetItems() {

        TableLayout itemsTable = (TableLayout) findViewById(R.id.budgetItems);
        itemsTable.removeAllViews();

        for (int i = 0; i < budget.budgetItems.size(); i++) {
            BudgetItem bi = budget.budgetItems.get(i);
            final int currentIndex = i;
            LayoutInflater inflater = this.getLayoutInflater();
            TableRow budgetItemView = (TableRow) inflater.inflate(R.layout.payday_budget_item, itemsTable, false);


            TextView amount = (TextView) budgetItemView.findViewById(R.id.budgetItemAmount);
            TextView title = (TextView) budgetItemView.findViewById(R.id.budgetItemLabel);

            Log.e("Payday", amount.toString());
            amount.setText(budget.formatter.format(bi.amount));

            title.setText(bi.title);

            if (bi.exclude){
                  amount.setTextColor(0xffCCCCCC);
                  amount.setPaintFlags(amount.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                  title.setTextColor(0xffCCCCCC);
                  title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }


            budgetItemView.setClickable(true);
            budgetItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(10);

                    BudgetItem bi = budget.budgetItems.get(currentIndex);
                    bi.exclude = !bi.exclude;
                    budget.saveBudgetItems();
                    updateBudgetItems();
                }
            });


            budgetItemView.setLongClickable(true);
            budgetItemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(100);
                    budget.budgetItems.remove(currentIndex);
                    budget.saveBudgetItems();
                    updateBudgetItems();
                    return true;
                }
            });

            itemsTable.addView(budgetItemView);

        }

        FontUtils.setRobotoFont(this, itemsTable);
        update();
    }

    private void renderBudget(double dailyBudget) {

        TableRow spentTodayRow = (TableRow) findViewById(R.id.spentTodayRow);

        if (!this.prefs.getBoolean(PreferenceKeys.KEY_PREF_USE_SPENT_TODAY, true)) {
            spentTodayRow.setVisibility(View.GONE);
        } else {
            spentTodayRow.setVisibility(View.VISIBLE);
        }


        TextView budgetView = (TextView) findViewById(R.id.budgetNumber);
        TextView daysToPaydayView = (TextView) findViewById(R.id.daysToPaydayNumber);
        TextView spentView = (TextView) findViewById(R.id.spentTodayNumber);
        TextView balanceView = (TextView) findViewById(R.id.balanceNumber);
        //TextView goalView = (TextView) findViewById(R.id.goalNumber);

        balanceView.setText(budget.formatter.format(budget.balance));
        spentView.setText(budget.formatter.format(budget.spentToday));
        // goalView.setText(budget.formatter.format(budget.savingsGoal));
        daysToPaydayView.setText(Integer.toString(budget.daysUntilPayday) + " ");
        budgetView.setText(budget.formatter.format(dailyBudget));
    }

    @TargetApi(11)
    private void renderBudgetAnimated() {
        ValueAnimator animation = ValueAnimator.ofFloat((float) currentBudget,
                (float) budget.dailyBudget);
        animation.setDuration(500);

        animation.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator a) {
                renderBudget((Float) a.getAnimatedValue());
            }
        });

        animation.start();

    }

    public void update() {
        try {
            budget.update();
        } catch (WrongAPIKeyException e) {
            runSetup();
            return;
        } catch (AccountNotFoundException e) {
            runSetup();
            return;
        }

        AppWidgetManager man = AppWidgetManager.getInstance(this);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(this, PaydayWidget.class));

        Intent updateIntent = new Intent();
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(PaydayWidget.WIDGET_IDS_KEY, ids);
        this.sendBroadcast(updateIntent);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            renderBudgetAnimated();
        } else {
            renderBudget(budget.dailyBudget);
        }
        currentBudget = budget.dailyBudget;

    }

    public void addBudgetItem(final View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.payday_dialog_add_budget_item, null);

        builder.setTitle(getString(R.string.add_budget_item_dialog_title));
        builder.setPositiveButton(R.string.add_budget_item, null);
        builder.setNegativeButton(R.string.cancel_add_budget_item, null);

        final AlertDialog d = builder.create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        EditText amountEdit = (EditText) dialogView.findViewById(R.id.new_budget_item_amount);
                        EditText titleEdit = (EditText) dialogView.findViewById(R.id.new_budget_item_title);
                        Spinner itemType = (Spinner) dialogView.findViewById(R.id.new_budget_item_type);

                        int amount;
                        try{
                            amount = Integer.parseInt(amountEdit.getText().toString());
                            if (itemType.getSelectedItemId() == 0) amount = -amount;
                        }catch (NumberFormatException e) {

                            Toast.makeText(getApplicationContext(),
                                    getString(R.string.new_budget_item_no_amount_specified),
                                    Toast.LENGTH_SHORT).show();

                            return;
                        }

                        String title = titleEdit.getText().toString();

                        BudgetItem newItem = new BudgetItem(title, amount);

                        budget.budgetItems.add(newItem);
                        budget.saveBudgetItems();
                        updateBudgetItems();
                        d.dismiss();
                    }
                });
            }
        });

        d.setView(dialogView);
        d.show();


    }

}
