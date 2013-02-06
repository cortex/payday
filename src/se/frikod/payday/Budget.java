package se.frikod.payday;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;

import org.joda.time.DateTime;
import org.joda.time.Days;

import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

import android.content.SharedPreferences;

public class Budget {
	int daysUntilPayday;
	double dailyBudget;
	double balance;
	double savingsGoal;
	double spentToday;
	BankdroidProvider bank;
	SharedPreferences prefs;
	DecimalFormat formatter;
	Holidays holidays;
	
	Budget(BankdroidProvider bank, SharedPreferences prefs, Holidays holidays) {
		
		this.bank = bank;
		this.prefs = prefs;
		this.holidays = holidays;
		

		Currency currency = Currency.getInstance("SEK");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setCurrency(currency);
		//dfs.setGroupingSeparator('\u2006');
		dfs.setGroupingSeparator(' ');
		dfs.setCurrencySymbol("kr");
		
		formatter = new DecimalFormat("#,###,### ¤", dfs);
		formatter.setMaximumFractionDigits(0);
		
	}

	public void update() throws WrongAPIKeyException, AccountNotFoundException {

		double balance;
		double spentToday;
		int payday;
		int savingsGoal;
		
        balance = this.bank.getBalance();
		spentToday = this.bank.getSpentToday();

		String paydayStr = prefs.getString(SettingsActivity.KEY_PREF_PAYDAY,
				"25");
		String goalStr = prefs.getString(SettingsActivity.KEY_PREF_GOAL, "0");

		try {
			payday = Integer.parseInt(paydayStr);
		} catch (NumberFormatException e) {
			payday = 25;
		}

		try {
			savingsGoal = Integer.parseInt(goalStr);
		} catch (NumberFormatException e) {
			savingsGoal = 0;
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
		this.savingsGoal = savingsGoal;

		if (this.prefs.getBoolean(SettingsActivity.KEY_PREF_USE_SPENT_TODAY, true)){
			this.spentToday = spentToday;	
		}else
		{
			this.spentToday = 0;
			spentToday = 0;
		}
		
		this.dailyBudget = (balance + spentToday - savingsGoal)
				/ this.daysUntilPayday;

	}
}
