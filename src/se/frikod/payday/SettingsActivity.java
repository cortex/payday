package se.frikod.payday;

import java.util.ArrayList;

import com.liato.bankdroid.provider.IBankTransactionsProvider;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import se.frikod.payday.R;
import android.util.Log;

public class SettingsActivity extends PreferenceActivity implements
		IBankTransactionsProvider, OnSharedPreferenceChangeListener {
	public static String KEY_PREF_PAIRED = "pref_paired_with_bankdroid";
	public static String KEY_PREF_API_KEY = "pref_API_key";
	public static String KEY_PREF_ACCOUNT = "pref_account";
	public static String KEY_PREF_PAYDAY = "pref_payday";
	public static String KEY_PREF_GOAL = "pref_goal";


	private BankdroidProvider bank = new BankdroidProvider(this);
	
	private static final String TAG = "Payday.settings";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		Log.i(TAG, "Settings created");
		check();
	}

	// This crap is needed to make the OnSharedPreferenceChangeListener
	// actually work

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

	private void pairWithBankdroid() {
		Intent i = new Intent("com.liato.bankroid.PAIR_APPLICATION_ACTION");
		i.putExtra("com.liato.bankdroid.PAIR_APP_NAME", "Payday");

		this.startActivityForResult(i, 0);
		Log.i(TAG, "Requesting pairing");
	}

	@Override
	protected void onActivityResult(final int requestCode,
			final int resultCode, final Intent data) {
		if (resultCode == RESULT_OK) {
			final String apiKey = data
					.getStringExtra(IBankTransactionsProvider.API_KEY);
			Log.d(TAG, "User accepted pairing. Got an API key back: " + apiKey);
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			final SharedPreferences.Editor editor = prefs.edit();
			// Commit to preferences
			editor.putString(KEY_PREF_API_KEY, apiKey);
			editor.commit();			
			check();
		} else if (resultCode == RESULT_CANCELED) {
			Log.d(TAG, "User did not accept pairing.");
		}
	}
	
	private void check() {
		CheckBoxPreference apiKeyEntry = (CheckBoxPreference) findPreference(KEY_PREF_PAIRED);
		apiKeyEntry.setOnPreferenceClickListener(new OnPreferenceClickListener(){
			@Override
			public boolean onPreferenceClick(Preference pref) {
				pairWithBankdroid();
				return true;
			}
		});
		
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
		Log.i(TAG, "Preferences changed");
		if (key.equals(KEY_PREF_API_KEY)) {
			check();
		}

	}
}
