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
            Log.d(TAG, "User accepted pairing. Got an API key back: " + apiKey);
            editor.putString(KEY_PREF_BANKDROID_API_KEY, apiKey);
            editor.putBoolean(KEY_PREF_BANKDROID_PAIRED, true);
        } else if (resultCode == RESULT_CANCELED) {
            Log.d(TAG, "User did not accept pairing.");
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
