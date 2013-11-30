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
