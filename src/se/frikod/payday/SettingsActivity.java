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

public class SettingsActivity extends PreferenceActivity {
    private BankdroidProvider bank;
	
	private static final String TAG = "Payday.settings";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		bank = new BankdroidProvider(this);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
        getAccounts();
	}


	private void getAccounts() {
		ListPreference lp = (ListPreference) findPreference(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNT);

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
    }
}
