package se.frikod.payday;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import se.frikod.payday.R;
import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;

class Budget {
	int daysUntilPayday;
	double dailyBudget;
	double balance;
	double savingsGoal;
	double spentToday;
}

public class Payday extends Activity {
	private static final String TAG = "Payday";
	static final int DIALOG_BANKDROID_NOT_INSTALLED = 0;
	private static final int DIALOG_BANKDROID_NOT_CONNECTED = 1;
	private static final int DIALOG_BANKDROID_ACCOUNT_NOT_FOUND = 2;
	private Budget budget = new Budget();
	private BankdroidProvider bank = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		bank = new BankdroidProvider(this);
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_payday);
		TextView bv = (TextView) findViewById(R.id.budgetTextView);
		bv.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				update();
			}
		});

		if (bank.verifySetup()) {
			update();
		} else {
			showDialog(DIALOG_BANKDROID_NOT_INSTALLED);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_budget, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		switch (id) {
		case DIALOG_BANKDROID_NOT_INSTALLED:
			builder.setTitle(R.string.bankdroid_not_installed_title)
					.setMessage(R.string.bankdroid_is_not_installed)
					.setCancelable(false)
					.setPositiveButton(R.string.install_bankdroid,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent goToMarket = new Intent(
											Intent.ACTION_VIEW).setData(Uri
											.parse("market://details?id=com.liato.bankdroid"));
									startActivity(goToMarket);
									Payday.this.finish();
								}
							});
			dialog = builder.create();
			break;

		case DIALOG_BANKDROID_NOT_CONNECTED:
			builder.setTitle(R.string.connect_to_bankdroid)
					.setMessage(
							R.string.connect_to_bankdroid_dialog_body)
					.setCancelable(false)
					.setPositiveButton(R.string.connect_to_bankdroid_settings,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent intent = new Intent(Payday.this,
											SettingsActivity.class);
									startActivity(intent);
									Payday.this.finish();
								}
							});
			dialog = builder.create();
			break;
		case DIALOG_BANKDROID_ACCOUNT_NOT_FOUND:
			builder.setTitle("Account missing")
					.setMessage(
							"You need to configure the account for bankdroid")
					.setCancelable(false)
					.setPositiveButton(R.string.connect_to_bankdroid_settings,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent intent = new Intent(Payday.this,
											SettingsActivity.class);
									startActivity(intent);
									Payday.this.finish();
								}
							});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	public void updateBudget() {

		double balance;
		double spentToday;
		int payday;
		int savingsGoal;
		try {
			balance = this.bank.getBalance();
			spentToday = this.bank.getSpentToday();
		} catch (WrongAPIKeyException e) {
			showDialog(DIALOG_BANKDROID_NOT_CONNECTED);
			return;

		} catch (AccountNotFoundException e) {
			showDialog(DIALOG_BANKDROID_ACCOUNT_NOT_FOUND);
			return;
		}
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		String paydayStr = prefs.getString(SettingsActivity.KEY_PREF_PAYDAY,
				"25");
		String goalStr = prefs.getString(SettingsActivity.KEY_PREF_GOAL, "0");

		try {
			payday = Integer.parseInt(paydayStr);
		} catch (NumberFormatException e) {
			payday = 25;
		}

		try {
			savingsGoal = Integer.parseInt(goalStr);
		} catch (NumberFormatException e) {
			savingsGoal = 0;
		}

		DateTime now = new DateTime();
		DateTime nextPayday;

		if (now.getDayOfMonth() < payday) {
			nextPayday = now.withDayOfMonth(payday);
		} else {
			nextPayday = now.plusMonths(1).withDayOfMonth(payday);
		}
		budget.balance = balance;
		budget.daysUntilPayday = Days.daysBetween(now, nextPayday).getDays();
		budget.savingsGoal = savingsGoal;
		budget.spentToday = spentToday;
		budget.dailyBudget = (balance - spentToday - savingsGoal) / budget.daysUntilPayday;
		
	}

	private void renderBudget(double dailyBudget) {
		Resources res = getResources();
		TextView budgetView = (TextView) findViewById(R.id.budgetTextView);
		TextView daysToPaydayView = (TextView) findViewById(R.id.daysToPaydayView);
		TextView spentView = (TextView) findViewById(R.id.spentTodayView);
		TextView balanceView = (TextView) findViewById(R.id.balanceView);
		TextView goalView = (TextView) findViewById(R.id.goalView);
				
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();						
		dfs.setGroupingSeparator('\u2006');
		//dfs.setCurrencySymbol("kr");
		DecimalFormat formatter = new DecimalFormat("#,###,### ¤", dfs);
		
		balanceView.setText(formatter.format(budget.balance));		
		spentView.setText(formatter.format(budget.spentToday));
		goalView.setText(formatter.format(budget.savingsGoal));

		daysToPaydayView.setText(Integer.toString(budget.daysUntilPayday));

		budgetView.setText(formatter.format(dailyBudget));
	}

	@TargetApi(11)
	private void renderBudgetAnimated() {
		ValueAnimator animation = ValueAnimator.ofFloat(0f,
				(float) budget.dailyBudget);
		animation.setDuration(500);

		animation.addUpdateListener(new AnimatorUpdateListener() {
			public void onAnimationUpdate(ValueAnimator a) {
				renderBudget((Float) a.getAnimatedValue());
			}
		});

		animation.start();

	}

	public void update() {
		updateBudget();
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			renderBudgetAnimated();
		} else {
			renderBudget(budget.dailyBudget);
		}
	}
}
