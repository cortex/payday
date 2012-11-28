package se.frikod.payday;

import java.util.ArrayList;

import org.joda.time.DateTime;

import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.liato.bankdroid.provider.IBankTransactionsProvider;

class Account {
	String name;
	String id;

	public Account(String mid, String mname) {
		name = mname;
		id = mid;
	}
}

public class BankdroidProvider implements IBankTransactionsProvider {

	public static String KEY_PREF_ACCOUNT = "pref_account";
	private Context context;
	private SharedPreferences prefs;
	private String apiKey;

	public BankdroidProvider(Context ctx) {
		context = ctx;
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		apiKey = prefs.getString(SettingsActivity.KEY_PREF_API_KEY, "");
	}

	public boolean verifySetup() {
		try {
			context.getPackageManager().getApplicationInfo(
					"com.liato.bankdroid", 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	public boolean verifyAPIKey() {
		apiKey = prefs.getString(SettingsActivity.KEY_PREF_API_KEY, "");
		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "name", "balance" };
		Cursor c = r.query(uri, fields, null, null, null);
		return !(c == null);
	}

	public ArrayList<Account> getAccounts() {
		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "id", "name", "balance" };
		Cursor c = r.query(uri, fields, null, null, null);
		ArrayList<Account> accounts = new ArrayList<Account>();
		while (c.getCount() > 0 && !c.isLast()) {
			c.moveToNext();
			accounts.add(new Account(c.getString(0), c.getString(1)));
		}
		return accounts;
	}

	public double getBalance() throws WrongAPIKeyException,
			AccountNotFoundException {
		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "id", "name", "balance" };
		String name = prefs.getString(SettingsActivity.KEY_PREF_ACCOUNT, "");
		try {
			Cursor c = r.query(uri, fields, "id = '" + name + "'", null, null);
			if (c == null) {
				throw new WrongAPIKeyException();
			}
			if (c.getCount() == 0) {
				throw new AccountNotFoundException();
			}
			c.moveToNext();
			return c.getDouble(2);
		} catch (IllegalArgumentException e) {
			throw new WrongAPIKeyException();
		}

	}

	public double getSpentToday() {
		final Uri uri = Uri.parse("content://" + AUTHORITY + '/'
				+ TRANSACTIONS_CAT + '/' + "API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "amount" };
		String name = prefs.getString(SettingsActivity.KEY_PREF_ACCOUNT, "");

		DateTime now = new DateTime();
		String today = now.toString("YYYY-MM-dd");
		String tomorrow = now.plusDays(1).toString("YYYY-MM-dd");
		Cursor c = r.query(uri, fields, String.format(
				"account = '%s' and transdate >= '%s' and transdate < '%s'",
				name, today, tomorrow), null, "transdate");
		float total = 0;
		if (c.getCount() == 0) {
			return 0.0;
		}
		while (!c.isLast()) {
			c.moveToNext();
			float amount = c.getFloat(0);
			if (amount < 0) {
				total -= amount;
			}
		}
		return total;

	}
}