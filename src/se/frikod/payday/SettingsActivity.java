package se.frikod.payday;

import java.util.ArrayList;

import com.liato.bankdroid.provider.IBankTransactionsProvider;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import se.frikod.payday.R;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;

public class SettingsActivity extends PreferenceActivity implements
		IBankTransactionsProvider, OnSharedPreferenceChangeListener {
	public static String KEY_PREF_API_KEY = "pref_API_key";
	public static String KEY_PREF_ACCOUNT = "pref_account";
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

	private void openBankdroidSettings(){
		Intent i = new Intent("com.liato.bankdroid.SettingsActivity");
		startActivity(i);
	}
	
	private void check() {
		EditTextPreference apiKeyEntry = (EditTextPreference) findPreference(KEY_PREF_API_KEY);
		
		Spanned s = Html.fromHtml(getString(R.string.api_key_dialog_message));
		apiKeyEntry.setDialogMessage(s);
		apiKeyEntry.setOnPreferenceClickListener(openBankdroidSettings)
		
		
		apiKeyEntry.setDialogMessage(s);
		
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
