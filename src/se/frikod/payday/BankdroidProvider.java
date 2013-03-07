package se.frikod.payday;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.text.TextUtils;
import org.joda.time.DateTime;

import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.liato.bankdroid.provider.IBankTransactionsProvider;

class Account {
	String name;
	String id;

	public Account(String mid, String mname) {
		name = mname;
		id = mid;
	}

    public String toString(){
        return name;
    }
}

public class BankdroidProvider implements IBankTransactionsProvider,
OnSharedPreferenceChangeListener{
    private static String TAG = "Payday.BankdroidProvider";
	private Context context;
	private SharedPreferences prefs;
	private String apiKey;

	public BankdroidProvider(Context ctx) {
		context = ctx;
		reload();
	}
	
	
	public boolean bankdroidInstalled() {
		try {
			context.getPackageManager().getApplicationInfo(
					"com.liato.bankdroid", 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	private void reload(){
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
		apiKey = prefs.getString(SetupActivity.KEY_PREF_BANKDROID_API_KEY, null);
		
	}
	
	public boolean verifySetup(){
		return verifyAPIKey();
	}
	
	public boolean verifyAPIKey() {
		apiKey = prefs.getString(SetupActivity.KEY_PREF_BANKDROID_API_KEY, null);
        if (apiKey == null){
            return false;
        }
		Log.i("se.frikod.payday", "API KEY:" + apiKey);
		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "name", "balance" };
		Cursor c = null;
		try{
			c = r.query(uri, fields, null, null, null);			
			return !(c == null);
		}catch (IllegalArgumentException e){
			if (!(c==null))c.close();
			return false;
		}
	}
    
    public boolean verifyAccount(){
        try{
            getBalance();
        } catch (AccountNotFoundException e){
            return false;
        } catch (WrongAPIKeyException e){
            return false;
        }
       return true;
    }

	public ArrayList<Account> getAccounts() {
        ArrayList<Account> accounts = new ArrayList<Account>();
        if (apiKey == null) return accounts;
		final Uri uri = Uri.parse("content://" + AUTHORITY
				+ "/bankaccounts/API_KEY=" + apiKey);

		ContentResolver r = context.getContentResolver();
		String[] fields = { "id", "name", "balance" };
		Cursor c = r.query(uri, fields, null, null, null);

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

		String defaultAccount = prefs.getString(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNT, "");
        Set<String> accounts = prefs.getStringSet(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNTS, new HashSet<String>());

        if (accounts.isEmpty()) accounts.add(defaultAccount);

        String namesIn = "(\"" + TextUtils.join("\", \"", accounts) + "\")";
        Log.e(TAG, defaultAccount);
        Log.e(TAG, accounts.toString());


        Cursor c;

		try {
            c = r.query(uri, fields, "id in " + namesIn, null, null);
        } catch (IllegalArgumentException e) {
            throw new WrongAPIKeyException();
        }

        if (c == null) {
            throw new WrongAPIKeyException();
        }
        if (c.getCount() == 0) {
            throw new AccountNotFoundException();
        }

        double balance = 0.0;
        while(c.moveToNext()){
            balance += c.getDouble(2);
        }

        return balance;
	}

	public double getSpentToday() {
		final Uri uri = Uri.parse("content://" + AUTHORITY + '/'
				+ TRANSACTIONS_CAT + '/' + "API_KEY=" + apiKey);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "amount" };
		String name = prefs.getString(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNT, "");

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

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		reload();
	}
}