package se.frikod.payday;

import android.os.Bundle;
import android.app.Activity;
import android.text.util.Linkify;
import android.widget.TextView;

public class AboutPaydayActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        setContentView(R.layout.payday_about);
		TextView noteView = (TextView) findViewById(R.id.aboutView);
		
		Linkify.addLinks(noteView, Linkify.ALL);

        FontUtils.setRobotoFont(this, this.getWindow()
                .getDecorView());

    }

}
