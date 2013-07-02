package se.frikod.payday;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionsGraphView extends View {

    private List<Transaction> transactions;
    private static final String TAG = "Payday.GraphRenderer";
    int barWidth = 50;
    int barMargin = 1;
    float mxOffset = 0;


    public TransactionsGraphView(Context context) {
        super(context);

    }
    public TransactionsGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        Paint tickPaint = new Paint();
        tickPaint.setColor(Color.BLUE);

        canvas.drawLine(0, 0, 20, 20, tickPaint);
        canvas.drawLine(20, 0, 0, 20, tickPaint);
        canvas.drawRect(0,0, 100,10, tickPaint);

        DateTime startDate = transactions.get(0).date;
        DateTime endDate = transactions.get(0).date;

        Map<DateTime, List<Transaction>> transactionsPerDate = new HashMap<DateTime, List<Transaction>>();

        int maxDailyTransactions = 1;

        for (Transaction t : transactions) {
            if (startDate.isAfter(t.date))
                startDate = t.date;
            if (endDate.isBefore(t.date))
                endDate = t.date;

            List<Transaction> ts = transactionsPerDate.get(t.date);
            if (ts == null){
                ts = new ArrayList<Transaction>();
                transactionsPerDate.put(t.date, ts);
            }
            ts.add(t);
            if (ts.size() > maxDailyTransactions) maxDailyTransactions = ts.size();
        }

        Days days = Days.daysBetween(startDate, endDate);

        float x = 0;


        Paint positiveBar = new Paint();
        Paint negativeBar = new Paint();
        positiveBar.setColor(0x660000);
        negativeBar.setColor(0x006600);

        for (int i = 0; i < days.getDays(); i++){
            DateTime day = startDate.plus(Days.days(i));
            List<Transaction> dayTransactions = transactionsPerDate.get(day);
            canvas.drawLine(x,-50, x, 50, tickPaint);

            if (dayTransactions != null){
                float xoff = 0;
                float bw = barWidth / (maxDailyTransactions - 1);

                for(Transaction t:dayTransactions){
                    Log.i(TAG, day.toString() + " " + x);
                    Paint barPaint;
                    if (t.amount.intValue() < 0){
                        barPaint = negativeBar;
                    }else{
                        barPaint = positiveBar;
                    }

                    xoff += bw;
                    canvas.drawRect(x + xoff, 0, x + bw, 0 + t.amount.toBigInteger().intValue(), barPaint);
                }
            }

            x += (barWidth + barMargin);
        }



    }

}
