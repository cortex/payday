package se.frikod.payday;

import se.frikod.payday.R;

import android.os.Bundle;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.TextView;

import org.joda.time.DateTime;
import org.joda.time.Days;

import com.liato.bankdroid.provider.IBankTransactionsProvider;

class Budget {
	int daysUntilPayday;
	double dailyBudget;
}

public class Payday extends Activity implements IBankTransactionsProvider {
	private static final String TAG = "Payday";
	private Budget budget = new Budget();
	private BankdroidProvider bank = new BankdroidProvider(this);

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_budget);
		TextView bv = (TextView) findViewById(R.id.budgetTextView);
		bv.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				update();
			}
		});
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

	public void updateBudget() {

		double balance = this.bank.getBalance();
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

	public void update() {
		updateBudget();

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

		Resources res = getResources();
		TextView bv = (TextView) findViewById(R.id.budgetTextView);
		TextView dv = (TextView) findViewById(R.id.daysToPaydayView);

		bv.setText(String.format(res.getString(R.string.daily_budget), 0.0f));
		dv.setText(String.format(res.getString(R.string.days_until_payday),
				budget.daysUntilPayday));
	}
}
