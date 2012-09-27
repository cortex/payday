package se.frikod.payday;

import android.app.Dialog;
import android.content.Context;
import android.preference.EditTextPreference;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class APIKeyPreference extends EditTextPreference{
	private static final String TAG = "Payday";

	
	    public APIKeyPreference(final Context context, AttributeSet attrs) {
			super(context, attrs);

			Spanned s = Html.fromHtml(context.getString(R.string.api_key_dialog_message));
			//apiKeyEntry.setDialogMessage(s);
			//EditText apiEditText = apiKeyEntry.getEditText();
			Spannable span = Spannable.Factory.getInstance().newSpannable(s); 
			span.setSpan(new ClickableSpan() {
			  @Override
			  public void onClick(View v) {
			      Log.d("main", "link clicked");
			      Toast.makeText(context, "link clicked", Toast.LENGTH_SHORT).show();
			  }
			});
			
			this.setDialogMessage(span);
			//this.set
			//this.set
			//this.getDialog().setMovementMethod(LinkMovementMethod.getInstance());

			//apiKeyEntry.setOnPreferenceClickListener(openBankdroidSettings)
			//apiKeyEntry.setDialogMessage(s);

	    }

	    	    
}
