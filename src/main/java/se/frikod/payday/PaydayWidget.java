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

import se.frikod.payday.exceptions.AccountNotFoundException;
import se.frikod.payday.exceptions.WrongAPIKeyException;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.RemoteViews;

public class PaydayWidget extends AppWidgetProvider {
    public static final String WIDGET_IDS_KEY = "mywidgetproviderwidgetids";
    public static final String WIDGET_DATA_KEY = "mywidgetproviderwidgetdata";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.hasExtra(WIDGET_IDS_KEY)) {
            int[] ids = intent.getExtras().getIntArray(WIDGET_IDS_KEY);
            if (intent.hasExtra(WIDGET_DATA_KEY)) {
                Object data = intent.getExtras().getParcelable(WIDGET_DATA_KEY);
                this.update(context, AppWidgetManager.getInstance(context), ids, data);
            } else {
                this.onUpdate(context, AppWidgetManager.getInstance(context), ids);
            }
        } else super.onReceive(context, intent);
    }

    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        update(context, appWidgetManager, appWidgetIds, null);
    }

    //This is where we do the actual updating
    public void update(Context context, AppWidgetManager manager, int[] ids, Object data) {
        BankdroidProvider bank = new BankdroidProvider(context);
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        Holidays holidays = new Holidays(context);

        Budget budget = new Budget(bank, context, holidays);
        try {
            budget.update();
        } catch (WrongAPIKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (AccountNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //data will contain some predetermined data, but it may be null
        for (int widgetId : ids) {

            Intent intent = new Intent(context, PaydayActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    intent, 0);

            RemoteViews views = new RemoteViews(context.getPackageName(),
                    R.layout.widget);
            views.setOnClickPendingIntent(R.id.widgetLayout, pendingIntent);

            views.setTextViewText(R.id.paydayWidgetBudget,
                    budget.formatter.format(budget.dailyBudget));

            manager.updateAppWidget(widgetId, views);

        }
    }

}
