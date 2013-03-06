package se.frikod.payday;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.*;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {

        BankdroidProvider bank = new BankdroidProvider(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        budget = new Budget(bank, prefs, new Holidays(this));

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

            budgetItemView.setLongClickable(true);
            budgetItemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d("Payday", "longclick");
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(500);
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
        ValueAnimator animation = ValueAnimator.ofFloat(0f,
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

    }

    public void addBudgetItem(final View view) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        final View dialogView = inflater.inflate(R.layout.payday_dialog_add_budget_item, null);

        builder.setTitle("Add new budget item");
        builder.setPositiveButton(R.string.add_budget_item, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {


                EditText amountEdit = (EditText) dialogView.findViewById(R.id.new_budget_item_amount);
                EditText titleEdit = (EditText) dialogView.findViewById(R.id.new_budget_item_title);
                Spinner itemType = (Spinner) dialogView.findViewById(R.id.new_budget_item_type);



                int amount = Integer.parseInt(amountEdit.getText().toString());

                if (itemType.getSelectedItemId() == 0) amount = -amount;

                String title = titleEdit.getText().toString();

                BudgetItem newItem = new BudgetItem(title, amount);

                budget.budgetItems.add(newItem);
                budget.saveBudgetItems();
                updateBudgetItems();
            }
        });

        builder.setNegativeButton(R.string.cancel_add_budget_item, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // FIRE ZE MISSILES!
            }
        });

        AlertDialog dialog = builder.create();


        dialog.setView(dialogView);


        dialog.show();


    }

}
