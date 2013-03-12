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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class Holidays {

	private Map<DateTime, String> holidays;
	private List<Integer> weekend;

	public static String convertStreamToString(InputStream is) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			sb.append(line);
		}

		is.close();

		return sb.toString();
	}

	Holidays(Context context) {
		InputStream holidaysFile;
		holidays = new HashMap<DateTime, String>();

		weekend = Arrays.asList(DateTimeConstants.SATURDAY,
				DateTimeConstants.SUNDAY);

		try {
			holidaysFile = context.getResources().getAssets()
					.open("Holidays_SE.json");
			String holidaysString = convertStreamToString(holidaysFile);
			JSONObject holidaysData = new JSONObject(holidaysString);
			Iterator<?> dates = holidaysData.keys();
			while (dates.hasNext()) {
				String key = (String) dates.next();
				String description = (String) holidaysData.get(key);
				DateTime date = DateTime.parse(key);
				holidays.put(date, description);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		catch (JSONException e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	boolean isHoliday(DateTime date) {

		return (weekend.contains(date.dayOfWeek().get()) || holidays
				.containsKey(date));
	}

}
