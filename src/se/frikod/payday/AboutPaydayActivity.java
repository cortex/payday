package se.frikod.payday;

import android.os.Bundle;
import android.app.Activity;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutPaydayActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.payday_about);
		TextView noteView = (TextView) findViewById(R.id.textView2);
		
		Linkify.addLinks(noteView, Linkify.ALL);

        FontUtils.setRobotoFont(this, this.getWindow()
                .getDecorView());

    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.payday_about, menu);
		return true;
	}

}
