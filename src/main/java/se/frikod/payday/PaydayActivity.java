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

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class PaydayActivity extends FragmentActivity {
    private static final int NUM_PAGES = 2;
    private String TAG  = "Payday.PaydayActivity";
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    BankdroidProvider bank;
    private Context ctx;
    private TransactionHistoryFragment transactionsHistoryFragment;
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ctx = this.getApplicationContext();
        bank = new BankdroidProvider(this);
        if (!bank.verifySetup()) {
            runSetup();
        }

        //FontUtils.setRobotoFont(this, this.getWindow().getDecorView());

        // Instantiate a ViewPager and a PagerAdapter.
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                if(i==1){
                    if (transactionsHistoryFragment != null)
                        transactionsHistoryFragment.onSelected();
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    public void runSetup() {
        Intent intent;
        intent = new Intent(this, SetupActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_budget, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void budgetHelpClicked(View v) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.budget_help_title).setMessage(R.string.budget_help_message)
                .setPositiveButton(R.string.budget_help_ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                )
                .show();
    }

    public void transactionsHelpClicked(View v) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.transactions_help_title).setMessage(R.string.transactions_help_message)
                .setPositiveButton(R.string.transactions_help_ok,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }
                )
                .show();
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter{
        public ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0){
                return new DailyBudgetFragment();
            }
            else{
                transactionsHistoryFragment = new TransactionHistoryFragment();
                return transactionsHistoryFragment;
            }
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }
    }


}
