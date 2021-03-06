package se.frikod.payday;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import java.util.Currency;
import java.util.LinkedList;

import android.content.Context;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.joda.time.DateTime;
import org.joda.time.Days;

import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

import android.content.SharedPreferences;

class BudgetItem{
    String title;
    BigDecimal amount;
    boolean exclude;

    public BudgetItem(String mTitle, BigDecimal mAmount){
        title = mTitle;
        amount = mAmount;
        exclude = false;
    }
}

public class Budget {
	int daysUntilPayday;
	double dailyBudget;
	double balance;

	BankdroidProvider bank;
	SharedPreferences prefs;
	DecimalFormat formatter;
	Holidays holidays;
    Context context;

    LinkedList<BudgetItem> budgetItems;

    Budget(BankdroidProvider bank, Context ctx, Holidays holidays) {
        this.context = ctx;
		this.bank = bank;
		this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		this.holidays = holidays;


		Currency currency = Currency.getInstance("SEK");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setCurrency(currency);
		dfs.setGroupingSeparator(' ');
		dfs.setCurrencySymbol("kr");
		
		formatter = new DecimalFormat("#,###,### ¤", dfs);
		formatter.setMaximumFractionDigits(0);
        loadBudgetItems();
        deprecateSavingsGoal();
	}


    private void deprecateSavingsGoal(){
        String goalStr = prefs.getString(PreferenceKeys.KEY_PREF_GOAL, null);
        if (goalStr != null){
            try {
                BigDecimal savingsGoal = new BigDecimal(goalStr);
                BudgetItem savingsGoalItem = new BudgetItem(context.getString(R.string.savings_goal_title), savingsGoal);
                this.budgetItems.add(savingsGoalItem);
                SharedPreferences.Editor editor = prefs.edit();
                editor.remove(PreferenceKeys.KEY_PREF_GOAL);
                editor.commit();
                this.saveBudgetItems();
            } catch (NumberFormatException e) {

            }
        }

    }

    public void saveBudgetItems(){
        SharedPreferences.Editor e = prefs.edit();
        GsonBuilder gsonb = new GsonBuilder();
        Gson gson = gsonb.create();
        String newJson = gson.toJson(budgetItems);
        e.putString(PreferenceKeys.KEY_PREF_BUDGET_ITEMS, newJson);
        e.commit();

    }

    public void loadBudgetItems(){

        String budgetItemsJson = prefs.getString(PreferenceKeys.KEY_PREF_BUDGET_ITEMS, null);

        GsonBuilder gsonb = new GsonBuilder();
        Gson gson = gsonb.create();

        Type collectionType = new TypeToken<LinkedList<BudgetItem>>() {
        }.getType();
        LinkedList<BudgetItem> items = gson.fromJson(budgetItemsJson, collectionType);

        if (items == null) {
            items = new LinkedList<BudgetItem>();
        }

        budgetItems = items;

    }


    public void update() throws WrongAPIKeyException, AccountNotFoundException {

		double balance;
		int payday;

        balance = this.bank.getBalance();

		String paydayStr = prefs.getString(PreferenceKeys.KEY_PREF_PAYDAY,
				"25");


		try {
			payday = Integer.parseInt(paydayStr);
		} catch (NumberFormatException e) {
			payday = 25;
		}


		DateTime now = new DateTime();
		DateTime nextPayday;
		nextPayday = now.withDayOfMonth(payday);
		
		while(holidays.isHoliday(nextPayday) ){
			nextPayday = nextPayday.minusDays(1);
		}
		
		if (nextPayday.isBefore(now) || Days.daysBetween(now, nextPayday).getDays() == 0){
			nextPayday = now.plusMonths(1).withDayOfMonth(payday); 
			while(holidays.isHoliday(nextPayday) ){
				nextPayday = nextPayday.minusDays(1);
			}					
		}

		this.balance = balance;
		this.daysUntilPayday = Days.daysBetween(now, nextPayday).getDays();

        double budgetItemsSum = 0;

        for(BudgetItem bi: budgetItems ){
            if (!bi.exclude){
                budgetItemsSum += bi.amount.floatValue();
            }
        }


		this.dailyBudget = (balance + budgetItemsSum)
				/ this.daysUntilPayday;

	}
}
