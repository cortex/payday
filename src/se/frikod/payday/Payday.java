package se.frikod.payday;

import se.frikod.payday.R;
import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;

import android.net.Uri;
import android.os.Bundle;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
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
}

public class Payday extends Activity {
	private static final String TAG = "Payday";
	static final int DIALOG_BANKDROID_NOT_INSTALLED = 0;
	private static final int DIALOG_WRONG_API_KEY = 1;
	private static final int DIALOG_BANKDROID_ACCOUNT_NOT_FOUND = 2;
	private Budget budget = new Budget();
	private BankdroidProvider bank = new BankdroidProvider(this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
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

		case DIALOG_WRONG_API_KEY:
			builder.setTitle("API key not set")
					.setMessage(
							"You need to configure the API key for bankdroid")
					.setCancelable(false)
					.setPositiveButton("Go to settings",
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
					.setPositiveButton("Go to settings",
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
		try {
			balance = this.bank.getBalance();
		} catch (WrongAPIKeyException e) {
			showDialog(DIALOG_WRONG_API_KEY);
			return;

		} catch (AccountNotFoundException e) {
			showDialog(DIALOG_BANKDROID_ACCOUNT_NOT_FOUND);
			return;
		}
		int payday = 25;

		DateTime now = new DateTime();
		DateTime nextPayday;

		if (now.getDayOfMonth() < payday) {
			nextPayday = now.withDayOfMonth(payday);
		} else {
			nextPayday = now.plusMonths(1).withDayOfMonth(payday);
		}

		budget.daysUntilPayday = Days.daysBetween(now, nextPayday).getDays();
		budget.dailyBudget = balance / budget.daysUntilPayday;
	}

	private void renderBudget() {
		Resources res = getResources();
		TextView bv = (TextView) findViewById(R.id.budgetTextView);
		TextView dv = (TextView) findViewById(R.id.daysToPaydayView);
		bv.setText(String.format(res.getString(R.string.daily_budget),
				budget.dailyBudget));
		dv.setText(String.format(res.getString(R.string.days_until_payday),
				budget.daysUntilPayday));
	}

	@TargetApi(11)
	private void renderBudgetAnimated() {
		Resources res = getResources();
		TextView bv = (TextView) findViewById(R.id.budgetTextView);
		TextView dv = (TextView) findViewById(R.id.daysToPaydayView);
		bv.setText(String.format(res.getString(R.string.daily_budget), 0.0f));
		dv.setText(String.format(res.getString(R.string.days_until_payday),
				budget.daysUntilPayday));

		ValueAnimator animation = ValueAnimator.ofFloat(0f,
				(float) budget.dailyBudget);
		animation.setDuration(500);

		animation.addUpdateListener(new AnimatorUpdateListener() {
			public void onAnimationUpdate(ValueAnimator a) {
				Resources res = getResources();
				TextView bv = (TextView) findViewById(R.id.budgetTextView);
				bv.setText(String.format(res.getString(R.string.daily_budget),
						(Float) a.getAnimatedValue()));
			}
		});

		animation.start();

	}

	public void update() {
		updateBudget();
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			renderBudgetAnimated();
		} else {
			renderBudget();
		}
	}
}
