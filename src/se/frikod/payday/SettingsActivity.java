/*
 *
 *   Copyright (C) 2012-2013 Joakim Lundborg <joakim,lundborg@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package se.frikod.payday;

import java.util.ArrayList;
import java.util.Set;

import android.content.SharedPreferences;
import android.preference.*;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity {
    private BankdroidProvider bank;

	private static final String TAG = "Payday.settings";
    private SharedPreferences prefs;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		bank = new BankdroidProvider(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);//deprecated
        getAccounts();
	}

	private void getAccounts() {

        String defaultAccount = prefs.getString(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNT, null);
        MultiSelectListPreference mlp = (MultiSelectListPreference ) findPreference(PreferenceKeys.KEY_PREF_BANKDROID_ACCOUNTS);

        ArrayList<Account> accounts = bank.getAccounts();
        if (accounts.size() > 0) {
            String[] accountNames = new String[accounts.size()];
            String[] accountIds = new String[accounts.size()];

            for (int i = 0; i < accounts.size(); i++) {
                accountNames[i] = accounts.get(i).name;
                accountIds[i] = accounts.get(i).id;
            }
            mlp.setEntries(accountNames);
            mlp.setEntryValues(accountIds);


            Set<String> current = mlp.getValues();
            Log.e("Payday", current.toString());

            if(current.isEmpty()){
                Log.e("Payday", defaultAccount);
                current.add(defaultAccount);
                mlp.setValues(current);
            }

            mlp.setEnabled(true);

            mlp.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    Set<String> v = (Set) newValue;

                    if (v.size() == 0) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.no_accounts_selected_warning),
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                }
            }
            );

        } else {
            mlp.setEnabled(false);
        }
    }
}
