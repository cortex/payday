package se.frikod.payday;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomFontArrayAdapter<T> extends ArrayAdapter {
	Typeface myFont; 
	public CustomFontArrayAdapter(Context context, int textViewResourceId, ArrayList<T> entries ) {
		super(context, textViewResourceId, entries);
		myFont = Typeface.createFromAsset(context.getAssets(), "fonts/Roboto-Light.ttf");

	}

	public TextView getView(int position, View convertView, ViewGroup parent) {
		TextView v = (TextView) super.getView(position, convertView, parent);
		v.setTypeface(myFont);
		return v;
	}

	public TextView getDropDownView(int position, View convertView,
			ViewGroup parent) {
		TextView v = (TextView) super.getView(position, convertView, parent);
		v.setTypeface(myFont);
		return v;
	}

}
