package se.frikod.payday;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.text.TextUtils;
import org.joda.time.DateTime;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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

import com.liato.bankdroid.provider.IBankTransactionsProvider;

class Account {
	String name;
	String id;

	public Account(String id, String name) {
		this.name = name;
		this.id = id;
	}

    public String toString(){
        return name;
    }
}

class Transaction{
    public String date_string;
    public BigDecimal amount;
    public String currency;
    public String description;
    public DateTime date;

    public Transaction(String date, BigDecimal amount, String currency, String description){
        this.date_string = date;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
        this.date = format.parseDateTime(date_string);
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
        Uri uri = getUri(BANK_ACCOUNTS_CAT);
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
        Uri uri = getUri(BANK_ACCOUNTS_CAT);
		ContentResolver r = context.getContentResolver();
		String[] fields = { "id", "name", "balance" };
		Cursor c = r.query(uri, fields, null, null, null);

		while (c.getCount() > 0 && !c.isLast()) {
			c.moveToNext();
			accounts.add(new Account(c.getString(0), c.getString(1)));
		}
		return accounts;
	}

    private Uri getUri(String category){
        Uri uri = Uri.parse("content://" + AUTHORITY + "/" + category + "/" + API_KEY + apiKey);
        return uri;
    }

    private String getChosenAccountsFilter(String field){
        String defaultAccount = prefs.getString(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNT, "");

        Set<String> accounts;
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            accounts = prefs.getStringSet(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNTS, new HashSet<String>());

        } else {
            accounts = new HashSet<String>();
            accounts.add(defaultAccount);
        }

        if (accounts.isEmpty()) accounts.add(defaultAccount);

        String namesIn = "(\"" + TextUtils.join("\", \"", accounts) + "\")";

        return field + " IN " + namesIn;

    }

	public double getBalance() throws WrongAPIKeyException, AccountNotFoundException {

        ContentResolver r = context.getContentResolver();
		Uri uri = getUri(BANK_ACCOUNTS_CAT);
        String[] fields = {ACC_ID, ACC_NAME, ACC_BALANCE};
        String chosenAccounts = getChosenAccountsFilter(ACC_ID);
        Cursor c;

		try {
            c = r.query(uri, fields, chosenAccounts, null, null);
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


    public List<Transaction> getTransactions(){
        Uri uri = getUri(TRANSACTIONS_CAT);

        ContentResolver r = context.getContentResolver();

        String[] fields = { TRANS_DATE, TRANS_AMT, TRANS_CUR, TRANS_DESC };
        String chosenAccounts = getChosenAccountsFilter(TRANS_ACCNT);

        Cursor c = r.query(uri, fields, chosenAccounts, null, TRANS_DATE);

        List<Transaction> transactions = new ArrayList<Transaction>(c.getCount());
        while (!c.isLast()) {
            c.moveToNext();
            transactions.add(new Transaction(
                    c.getString(0),
                    new BigDecimal(c.getDouble(1)),
                    c.getString(2),
                    c.getString(3)));
        }
        return transactions;
    }


	public double getSpentToday() {
        Uri uri = getUri(TRANSACTIONS_CAT);
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