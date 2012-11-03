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
	
	Budget(BankdroidProvider bank, SharedPreferences prefs){
		this.bank = bank;
		this.prefs = prefs;
		
		Currency currency = Currency.getInstance("SEK");
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        dfs.setCurrency(currency);
		dfs.setGroupingSeparator('\u2006');
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

		if (now.getDayOfMonth() < payday) {
			nextPayday = now.withDayOfMonth(payday);
		} else {
			nextPayday = now.plusMonths(1).withDayOfMonth(payday);
		}
		this.balance = balance;
		this.daysUntilPayday = Days.daysBetween(now, nextPayday).getDays();
		this.savingsGoal = savingsGoal;
		this.spentToday = spentToday;
		this.dailyBudget = (balance + spentToday - savingsGoal)
				/ this.daysUntilPayday;

	}
}