package se.frikod.payday;

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
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.TableRow;
import android.widget.TextView;

public class PaydayActivity extends Activity {
	static final int DIALOG_BANKDROID_NOT_INSTALLED = 0;
	private static final int DIALOG_BANKDROID_NOT_CONNECTED = 1;
	private static final int DIALOG_BANKDROID_ACCOUNT_NOT_FOUND = 2;
	private Budget budget;
	private BankdroidProvider bank = null;
	Typeface numberFont;
	Typeface labelFont;
	Typeface detailsLabelFont;
	Typeface budgetLabelFont;
	Typeface budgetNumberFont;
	SharedPreferences prefs;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		//numberFont = Typeface.createFromAsset(getAssets(), "fonts/ArbutusSlab-Regular.ttf");
		//numberFont = Typeface.createFromAsset(getAssets(), "fonts/Alegreya-Regular.otf");
		//numberFont = Typeface.createFromAsset(getAssets(), "fonts/ITC American Typewriter LT Medium.ttf");
		//numberFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Thin.ttf");
		
		//budgetLabelFont = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");
		budgetLabelFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
		//budgetNumberFont = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");
		//budgetNumberFont = Typeface.createFromAsset(getAssets(), "fonts/ArbutusSlab-Regular.ttf");
		budgetNumberFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Medium.ttf");
		
		detailsLabelFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
		//detailsLabelFont = Typeface.createFromAsset(getAssets(), "fonts/OpenSans-Bold.ttf");
		
		numberFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Regular.ttf");
		//labelFont = Typeface.createFromAsset(getAssets(), "fonts/ArbutusSlab-Regular.ttf");
		//labelFont = Typeface.createFromAsset(getAssets(), "fonts/Alegreya-Regular.otf");
		labelFont = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
		
		bank = new BankdroidProvider(this);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		budget = new Budget(bank, prefs, new Holidays(this));
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.payday_activity);
		TextView bv = (TextView) findViewById(R.id.budgetNumber);
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
	public void onResume(){
		super.onResume();
		update();
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
									Intent intent = new Intent(PaydayActivity.this,
											SettingsActivity.class);
									startActivity(intent);
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
									Intent intent = new Intent(PaydayActivity.this,
											SettingsActivity.class);
									startActivity(intent);
								}
							});
			dialog = builder.create();
			break;
		default:
			dialog = null;
		}
		return dialog;
	}



	private void renderBudget(double dailyBudget) {
		

		TableRow spentTodayRow = (TableRow) findViewById(R.id.spentTodayRow);

		if (!this.prefs.getBoolean(SettingsActivity.KEY_PREF_USE_SPENT_TODAY, true)){
			Log.d("Payday", "spent today hidden");
			spentTodayRow.setVisibility(View.GONE);
		}else{
			spentTodayRow.setVisibility(View.VISIBLE);
		}
		
		TextView budgetView = (TextView) findViewById(R.id.budgetNumber);
				
		TextView daysToPaydayView = (TextView) findViewById(R.id.daysToPaydayNumber);
		
		
		((TextView) findViewById(R.id.budgetLabel)).setTypeface(budgetLabelFont);
		budgetView.setTypeface(budgetNumberFont );
		
		((TextView) findViewById(R.id.detailsLabel)).setTypeface(detailsLabelFont);
		
		((TextView) findViewById(R.id.daysToPaydayLabel)).setTypeface(labelFont);
		((TextView) findViewById(R.id.spentTodayLabel)).setTypeface(labelFont);
		((TextView) findViewById(R.id.balanceLabel)).setTypeface(labelFont);
		((TextView) findViewById(R.id.goalLabel)).setTypeface(labelFont);
		
		
		TextView spentView = (TextView) findViewById(R.id.spentTodayNumber);
		TextView balanceView = (TextView) findViewById(R.id.balanceNumber);
		TextView goalView = (TextView) findViewById(R.id.goalNumber);

		spentView.setTypeface(numberFont);
		balanceView.setTypeface(numberFont);
		goalView.setTypeface(numberFont);
		daysToPaydayView.setTypeface(numberFont);
		
		balanceView.setText(budget.formatter.format(budget.balance));		
		spentView.setText(budget.formatter.format(budget.spentToday));
		goalView.setText(budget.formatter.format(budget.savingsGoal));

		daysToPaydayView.setText(Integer.toString(budget.daysUntilPayday) + " ");
		
		budgetView.setText(budget.formatter.format(dailyBudget));
		

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
		try {
			budget.update();
		} catch (WrongAPIKeyException e) {
			showDialog(DIALOG_BANKDROID_NOT_CONNECTED);
			return;

		} catch (AccountNotFoundException e) {
			showDialog(DIALOG_BANKDROID_ACCOUNT_NOT_FOUND);
			return;
		}
	    AppWidgetManager man = AppWidgetManager.getInstance(this);
	    int[] ids = man.getAppWidgetIds(
	            new ComponentName(this,PaydayWidget.class));

	    Intent updateIntent = new Intent();
	    updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
	    updateIntent.putExtra(PaydayWidget.WIDGET_IDS_KEY, ids);
	    this.sendBroadcast(updateIntent);

		
		
		if (android.os.Build.VERSION.SDK_INT >= 11) {
			renderBudgetAnimated();
		} else {
			renderBudget(budget.dailyBudget);
		}
	}
}
