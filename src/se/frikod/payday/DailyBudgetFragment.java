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
import android.support.v4.app.Fragment;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

public class DailyBudgetFragment extends Fragment implements OnClickListener {

    SharedPreferences prefs;
    private Budget budget;
    private double currentBudget = 0;
    private View V;
    private PaydayActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        V = inflater.inflate(R.layout.daily_budget_fragment, container, false);
        activity =  (PaydayActivity) this.getActivity();
        Context ctx = activity.getApplicationContext();

        FontUtils.setRobotoFont(ctx, activity.getWindow().getDecorView());

        prefs = activity.getPreferences(Context.MODE_MULTI_PROCESS);
        TextView bv = (TextView)V.findViewById(R.id.budgetNumber);
        bv.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                updateBudget();
            }
        });

        budget = new Budget(activity.bank, ctx, new Holidays(ctx));
        updateBudget();
        return V;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateBudget();
    }


    private void updateBudgetItems() {

        TableLayout itemsTable = (TableLayout) V.findViewById(R.id.budgetItems);
        itemsTable.removeAllViews();

        for (int i = 0; i < budget.budgetItems.size(); i++) {
            BudgetItem bi = budget.budgetItems.get(i);
            final int currentIndex = i;
            LayoutInflater inflater = activity.getLayoutInflater();
            TableRow budgetItemView = (TableRow) inflater.inflate(R.layout.payday_budget_item, itemsTable, false);


            TextView amount = (TextView) budgetItemView.findViewById(R.id.budgetItemAmount);
            TextView title = (TextView) budgetItemView.findViewById(R.id.budgetItemLabel);

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
                    Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
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

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());

                    builder.setTitle(getString(R.string.delete_budget_item_title));
                    builder.setMessage(getString(R.string.delete_budget_item_message));

                    builder.setPositiveButton(getString(R.string.delete_budget_item_yes), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Vibrator vibrator = (Vibrator) activity.getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(100);
                            budget.budgetItems.remove(currentIndex);
                            budget.saveBudgetItems();
                            updateBudgetItems();

                            dialog.dismiss();
                        }

                    });

                    builder.setNegativeButton(getString(R.string.delete_budget_item_no), new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing
                            dialog.dismiss();
                        }
                    });

                    builder.create().show();

                    return true;
                }
            });

            itemsTable.addView(budgetItemView);

        }

        FontUtils.setRobotoFont(activity, itemsTable);
        updateBudget();
    }

    public void updateBudget() {

        try {
            budget.update();
        } catch (WrongAPIKeyException e) {
            activity.runSetup();
            return;
        } catch (AccountNotFoundException e) {
            activity.runSetup();
            return;
        }

        AppWidgetManager man = AppWidgetManager.getInstance(activity);
        int[] ids = man.getAppWidgetIds(
                new ComponentName(activity, PaydayWidget.class));

        Intent updateIntent = new Intent();
        updateIntent.setClass(activity, PaydayWidget.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        updateIntent.putExtra(PaydayWidget.WIDGET_IDS_KEY, ids);
        activity.sendBroadcast(updateIntent);

        if (android.os.Build.VERSION.SDK_INT >= 11) {
            renderBudgetAnimated();
        } else {
            renderBudget(budget.dailyBudget);
        }
        currentBudget = budget.dailyBudget;

    }

    private void renderBudget(double dailyBudget) {

        TableRow spentTodayRow = (TableRow) V.findViewById(R.id.spentTodayRow);

        if (!this.prefs.getBoolean(PreferenceKeys.KEY_PREF_USE_SPENT_TODAY, true)) {
            spentTodayRow.setVisibility(View.GONE);
        } else {
            spentTodayRow.setVisibility(View.VISIBLE);
        }


        TextView budgetView = (TextView) V.findViewById(R.id.budgetNumber);
        TextView daysToPaydayView = (TextView) V.findViewById(R.id.daysToPaydayNumber);
        TextView spentView = (TextView) V.findViewById(R.id.spentTodayNumber);
        TextView balanceView = (TextView) V.findViewById(R.id.balanceNumber);
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

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.addBudgetItem:
                addBudgetItem(v);
                break;
        }
    }

    public void addBudgetItem(final View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.payday_dialog_add_budget_item, null);

        builder.setTitle(getString(R.string.add_budget_item_dialog_title));
        builder.setPositiveButton(R.string.add_budget_item, null);
        builder.setNegativeButton(R.string.cancel_add_budget_item, null);

        final AlertDialog d = builder.create();

        d.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
        assert b != null;
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

                            Toast.makeText(activity.getApplicationContext(),
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
