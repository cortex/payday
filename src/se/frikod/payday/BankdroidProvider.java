package se.frikod.payday;

import java.util.ArrayList;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.liato.bankdroid.provider.IBankTransactionsProvider;

class AccountData {
	String[] accountNames;
	String[] accountIds;
}

public class BankdroidProvider implements IBankTransactionsProvider {
	private static final String TAG = "Payday.provider";
	public static String KEY_PREF_ACCOUNT = "pref_account";

	private Context context;
	private SharedPreferences prefs;

	public BankdroidProvider(Context ctx) {
		context = ctx;
		// prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

	}

	public boolean verifyAPIKey() {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String apiKey = prefs.getString(SettingsActivity.KEY_PREF_API_KEY, "a");
		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "name", "balance" };
		Cursor c = r.query(uri, fields, null, null, null);
		return !(c == null);
	}

	public String[] getAccounts() {
		Log.i(TAG, "Getting accounts");
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String apiKey = prefs.getString(SettingsActivity.KEY_PREF_API_KEY, "a");

		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();

		String[] fields = { "name", "balance" };
		Cursor c = r.query(uri, fields, null, null, null);

		ArrayList<String> out = new ArrayList<String>();

		while (!c.isLast()) {
			c.moveToNext();
			out.add(c.getString(0));
		}
		String r1[] = new String[out.size()];
		r1 = out.toArray(r1);
		return r1;
	}

	public double getBalance() {
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String apiKey = prefs.getString(SettingsActivity.KEY_PREF_API_KEY, "a");
		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "name", "balance" };
		String name = prefs.getString(SettingsActivity.KEY_PREF_ACCOUNT, "");
		Cursor c = r.query(uri, fields, "name like '" + name + "'", null, null);
		if (c != null) {
			c.moveToNext();
			return c.getDouble(1);
		} else {
			Log.e(TAG, "API key" + apiKey);
			return -1.0;
		}
	}

}
