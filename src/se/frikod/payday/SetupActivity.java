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

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.*;
import com.liato.bankdroid.provider.IBankTransactionsProvider;

import java.util.ArrayList;

import static android.widget.AdapterView.OnItemSelectedListener;

public class SetupActivity extends Activity
    implements OnItemSelectedListener
{
    private static String TAG = "Payday.SetupActivity";
    public static String KEY_PREF_BANKDROID_PAIRED = "pref_bankdroid_paired";
    public static String KEY_PREF_BANKDROID_API_KEY = "pref_API_key";
    public static String KEY_PREF_BANKDROID_ACCOUNT = "pref_account";
    private static BankdroidProvider bank;
    Spinner accountSpinner;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.payday_setup);

        bank = new BankdroidProvider(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        accountSpinner = (Spinner) findViewById(R.id.setupAccountSpinner);
        accountSpinner.setOnItemSelectedListener(this);

        FontUtils.setRobotoFont(this, this.getWindow().getDecorView());
        check();

    }

    private void check() {


        if (bank.bankdroidInstalled()){
            ImageView bankdroidInstalledStatusIcon = (ImageView) findViewById(R.id.bankdroidInstalledStatusIcon);
            bankdroidInstalledStatusIcon.setImageResource(R.drawable.ic_ok);

            Button installBankdroidButton = (Button) findViewById(R.id.installBankdroidButton);
            installBankdroidButton.setVisibility(View.GONE);

        }

        if (bank.verifyAPIKey()) {
            ImageView bankdroidConnectedStatusIcon = (ImageView) findViewById(R.id.bankdroidConnectedStatusIcon);
            bankdroidConnectedStatusIcon.setImageResource(R.drawable.ic_ok);

            Button connectBankdroidButton = (Button) findViewById(R.id.connectBankdroidButton);
            connectBankdroidButton.setVisibility(View.GONE);

            ArrayList<Account> accounts = bank.getAccounts();
            if (accounts.size() > 0) {
                CustomFontArrayAdapter<Account> adapter = new CustomFontArrayAdapter<Account>(this,
                        android.R.layout.simple_dropdown_item_1line,
                        accounts);

                accountSpinner.setAdapter(adapter);
                accountSpinner.setSelection(adapter.getPosition(prefs.getString(KEY_PREF_BANKDROID_ACCOUNT, "")));
                accountSpinner.setEnabled(true);
            } else {
                accountSpinner.setEnabled(false);
            }

        } else {
            accountSpinner.setEnabled(false);
        }

        checkAccount();

    }

    private void checkAccount(){
        ImageView accountPickedStatusIcon = (ImageView) findViewById(R.id.accountPickedStatusIcon);
        if (bank.verifyAccount()){
            accountPickedStatusIcon.setImageResource(R.drawable.ic_ok);
            Button doneButton = (Button) findViewById(R.id.setupDoneButton);
            doneButton.setEnabled(true);
        }else{
            accountPickedStatusIcon.setImageResource(R.drawable.ic_notok);
            Button doneButton = (Button) findViewById(R.id.setupDoneButton);
            doneButton.setEnabled(false);
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id){
        Account pickedAccount = (Account) parent.getItemAtPosition(pos);
        Log.i(TAG, pickedAccount.id);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_PREF_BANKDROID_ACCOUNT, pickedAccount.id);
        editor.commit();
        checkAccount();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void doneClicked(View view) {
        Intent intent = new Intent(this, PaydayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void installBankdroidClicked(View view){
        Intent goToMarket = new Intent(
                Intent.ACTION_VIEW).setData(Uri
                .parse("market://details?id=com.liato.bankdroid"));
        startActivity(goToMarket);

    }

    @Override
    protected void onActivityResult(final int requestCode,
                                    final int resultCode, final Intent data) {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        final SharedPreferences.Editor editor = prefs.edit();
        if (resultCode == RESULT_OK) {
            final String apiKey = data
                    .getStringExtra(IBankTransactionsProvider.API_KEY);
            editor.putString(KEY_PREF_BANKDROID_API_KEY, apiKey);
            editor.putBoolean(KEY_PREF_BANKDROID_PAIRED, true);
        } else if (resultCode == RESULT_CANCELED) {
            editor.putString(KEY_PREF_BANKDROID_API_KEY, null);
            editor.putBoolean(KEY_PREF_BANKDROID_PAIRED, false);
        }
        editor.commit();
        check();

    }


    public void connectToBankdroidClicked(View view){
        Intent i = new Intent("com.liato.bankroid.PAIR_APPLICATION_ACTION");
        i.putExtra("com.liato.bankdroid.PAIR_APP_NAME", "Payday");
        this.startActivityForResult(i, 0);
        check();
    }
}
