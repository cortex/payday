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

import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FontUtils {

	public static interface FontTypes {
		public static String LIGHT = "Light";
		public static String BOLD = "Bold";
	}

	/**
	 * map of font types to font paths in assets
	 */
	private static Map<String, String> fontMap = new HashMap<String, String>();
	static {
		fontMap.put(FontTypes.LIGHT, "fonts/Roboto-Light.ttf");
		fontMap.put(FontTypes.BOLD, "fonts/Roboto-Regular.ttf");
	}

	/* cache for loaded Roboto typefaces */
	private static Map<String, Typeface> typefaceCache = new HashMap<String, Typeface>();

	/**
	 * Creates Roboto typeface and puts it into cache
	 * 
	 * @param context
	 * @param fontType
	 * @return
	 */
	private static Typeface getRobotoTypeface(Context context, String fontType) {
		String fontPath = fontMap.get(fontType);
		if (!typefaceCache.containsKey(fontType)) {
			typefaceCache.put(fontType,
					Typeface.createFromAsset(context.getAssets(), fontPath));
		}
		return typefaceCache.get(fontType);
	}

	/**
	 * Gets roboto typeface according to passed typeface style settings. Will
	 * get Roboto-Bold for Typeface.BOLD etc
	 * 
	 * @param context
	 * @param typefaceStyle
	 * @return
	 */
	private static Typeface getRobotoTypeface(Context context,
			Typeface originalTypeface) {
		String robotoFontType = FontTypes.LIGHT; // default Light Roboto font
		if (originalTypeface != null) {
			int style = originalTypeface.getStyle();
			switch (style) {
			case Typeface.BOLD:
				robotoFontType = FontTypes.BOLD;
			}
		}
		return getRobotoTypeface(context, robotoFontType);
	}

	/**
	 * Walks ViewGroups, finds TextViews and applies Typefaces taking styling in
	 * consideration
	 * 
	 * @param context
	 *            - to reach assets
	 * @param view
	 *            - root view to apply typeface to
	 */
	public static void setRobotoFont(Context context, View view) {
		if (view instanceof ViewGroup) {
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
				setRobotoFont(context, ((ViewGroup) view).getChildAt(i));
			}
		} else if (view instanceof TextView) {
			Typeface currentTypeface = ((TextView) view).getTypeface();
			((TextView) view).setTypeface(getRobotoTypeface(context,
					currentTypeface));
		} 
	}
}
