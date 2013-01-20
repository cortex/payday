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
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import android.widget.TableRow;
import android.widget.TextView;

public class PaydayActivity extends Activity {
	
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
		
		bank = new BankdroidProvider(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        budget = new Budget(bank, prefs, new Holidays(this));

		super.onCreate(savedInstanceState);

        setContentView(R.layout.payday_activity);
        FontUtils.setRobotoFont(this, this.getWindow().getDecorView());


        if (bank.verifySetup()) {
            Log.i("Payday", "Verify setup failed");
            update();
        } else {
            runSetup();
        }

		TextView bv = (TextView) findViewById(R.id.budgetNumber);
		bv.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				update();
			}
		});

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
		Intent intent;
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_settings:
			intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
			return true;
		case R.id.menu_setup:
			runSetup();
			return true;		
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void runSetup(){
		Intent intent;
		intent = new Intent(this, SetupActivity.class);
		startActivity(intent);
        finish();
	}
		
	private void renderBudget(double dailyBudget) {

		TableRow spentTodayRow = (TableRow) findViewById(R.id.spentTodayRow);

		if (!this.prefs.getBoolean(SettingsActivity.KEY_PREF_USE_SPENT_TODAY, true)){
			spentTodayRow.setVisibility(View.GONE);
		}else{
			spentTodayRow.setVisibility(View.VISIBLE);
		}
		
		TextView budgetView = (TextView) findViewById(R.id.budgetNumber);				
		TextView daysToPaydayView = (TextView) findViewById(R.id.daysToPaydayNumber);				
		TextView spentView = (TextView) findViewById(R.id.spentTodayNumber);
		TextView balanceView = (TextView) findViewById(R.id.balanceNumber);
		TextView goalView = (TextView) findViewById(R.id.goalNumber);

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
		} catch (WrongAPIKeyException e){ 
			runSetup();
			return;
		}
		catch (AccountNotFoundException  e) {
			runSetup();
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
