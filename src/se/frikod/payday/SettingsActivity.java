	package se.frikod.payday;

import java.util.ArrayList;

import com.liato.bankdroid.provider.IBankTransactionsProvider;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import se.frikod.payday.R;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements
		IBankTransactionsProvider, OnSharedPreferenceChangeListener {
	public static String KEY_PREF_PAIRED_WITH_BANKDROID = "pref_paired_with_bankdroid";
	public static String KEY_PREF_API_KEY = "pref_API_key";
	public static String KEY_PREF_ACCOUNT = "pref_account";
	public static String KEY_PREF_PAYDAY = "pref_payday";
	public static String KEY_PREF_GOAL = "pref_goal";
	public static String KEY_PREF_USE_SPENT_TODAY = "pref_use_spent_today";


	private BankdroidProvider bank;
	
	private static final String TAG = "Payday.settings";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		bank = new BankdroidProvider(this);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);	     
	}

	
	void pairWithBankdroid(){
			Intent i = new Intent("com.liato.bankroid.PAIR_APPLICATION_ACTION");
			i.putExtra("com.liato.bankdroid.PAIR_APP_NAME", "Payday");		
			this.startActivityForResult(i, 0);
	}
	
	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);		
		
		final SharedPreferences.Editor editor = prefs.edit();
		if (resultCode == RESULT_OK) {
			final String apiKey = data
					.getStringExtra(IBankTransactionsProvider.API_KEY);
			Log.d(TAG, "User accepted pairing. Got an API key back: " + apiKey);
			editor.putString(KEY_PREF_API_KEY, apiKey);
			editor.putBoolean(KEY_PREF_PAIRED_WITH_BANKDROID, true);
		} else if (resultCode == RESULT_CANCELED) {
			Log.d(TAG, "User did not accept pairing.");
			editor.putString(KEY_PREF_API_KEY, null);
			editor.putBoolean(KEY_PREF_PAIRED_WITH_BANKDROID, false);
		}
		editor.commit();
		check();

	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences()
				.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences()
				.unregisterOnSharedPreferenceChangeListener(this);
	}

	// End of crap


	
	private void check() {
		
		ListPreference lp = (ListPreference) findPreference(KEY_PREF_ACCOUNT);
		

		if (bank.verifyAPIKey()) {
			ArrayList<Account> accounts = bank.getAccounts();
			if (accounts.size() > 0) {
				String[] accountNames = new String[accounts.size()];
				String[] accountIds = new String[accounts.size()];

				for (int i = 0; i < accounts.size(); i++) {
					accountNames[i] = accounts.get(i).name;
					accountIds[i] = accounts.get(i).id;
				}
				lp.setEntries(accountNames);
				lp.setEntryValues(accountIds);
				lp.setEnabled(true);
			} else {
				lp.setEnabled(false);
			}

		} else {
			lp.setEnabled(false);			
		}

	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
			String key) {
		if (key.equals(KEY_PREF_API_KEY)) {
			check();
		}
	}
}
