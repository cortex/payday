package se.frikod.payday;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.frikod.payday.charts.Scale;

public class TransactionsGraphView extends View {

    private List<Transaction> transactions;
    private static final String TAG = "Payday.GraphRenderer";

    int barWidth = 36;
    int barMargin = 1;
    float captionFontSize = 24f;
    float mxOffset = 0;

    private Paint tickPaint;
    private Paint positiveBar;
    private Paint negativeBar;
    private Days days;
    private Scale yScale;
    private DateTime startDate;
    private DateTime endDate;
    private Map<DateTime,List<Transaction>> transactionsPerDate;
    private BigDecimal maxTrans = BigDecimal.ZERO;
    private BigDecimal minTrans = BigDecimal.ZERO;
    private Paint captionStyle;

    public TransactionsGraphView(Context context) {
        super(context);
        transactions = new ArrayList<Transaction>();
    }
    public TransactionsGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        transactions = new ArrayList<Transaction>();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        setup();
        invalidate();
    }

    protected void setup(){

        tickPaint = new Paint();
        positiveBar = new Paint();
        negativeBar = new Paint();
        captionStyle = new Paint();

        positiveBar.setColor(Color.HSVToColor(new float[]{150f,0.6f,1f}));
        negativeBar.setColor(Color.HSVToColor(new float[]{340f,0.6f,1f}));
        tickPaint.setColor(Color.HSVToColor(new float[]{0f,0.0f,.95f}));

        captionStyle.setColor(Color.BLACK);
        captionStyle.setTextSize(captionFontSize);
        captionStyle.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        captionStyle.setAntiAlias(true);
        startDate = transactions.get(0).date;
        endDate = transactions.get(0).date;

        transactionsPerDate = new HashMap<DateTime, List<Transaction>>();

        int maxDailyTransactions = 3;

        for (Transaction t : transactions) {
            if (t.amount.compareTo(maxTrans) > 0)
                maxTrans = t.amount;

            if (t.amount.compareTo(minTrans) < 0)
                minTrans = t.amount;

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

        days = Days.daysBetween(startDate, endDate);

        Log.i(TAG, String.format("%f, %f", maxTrans.doubleValue(), minTrans.doubleValue()));
        yScale = new Scale();
    }

    protected void onDraw(Canvas canvas) {

        if (transactions.size() == 0){
            return;
        }

        float x = 0;
        float graphHeight = canvas.getHeight() / 2f;
        float y = graphHeight / 2f;

        yScale.update(minTrans, maxTrans, -graphHeight / 4f, graphHeight / 4f);
        barWidth = canvas.getWidth() / (days.getDays() +1 );
        captionFontSize = graphHeight / 20f;
        captionStyle.setTextSize(captionFontSize);
        canvas.drawRect(mxOffset - 10, 0, mxOffset + 10, graphHeight, tickPaint);

        List<Transaction> selectedTransactions = null;
        DateTime selectedDay = null;

        for (int i = 0; i < days.getDays(); i++){
            DateTime day = startDate.plus(Days.days(i));
            List<Transaction> dayTransactions = transactionsPerDate.get(day);
            canvas.drawLine(x, y - (graphHeight / 2f), x, y + (graphHeight / 2f), tickPaint);

            float positiveHeight = 0;
            float negativeHeight = 0;
            if (dayTransactions != null){
                double top, bottom;
                if (mxOffset > x && mxOffset < x + barWidth){
                    selectedTransactions = dayTransactions;
                    selectedDay = day;
                }
                for(Transaction t:dayTransactions){
                    //Log.i(TAG, day.toString() + " " + x + " " + t.amount);
                    Paint barPaint;
                    double val = yScale.apply(t.amount);

                    if (val < 0){
                        barPaint = negativeBar;
                        top = 0 + negativeHeight;
                        bottom = negativeHeight  -val;
                        negativeHeight += (-val + barMargin);
                    }else{
                        barPaint = positiveBar;
                        top = -val - positiveHeight;
                        bottom = 0 - positiveHeight;
                        positiveHeight += (val + barMargin);
                    }

                    //Log.i(TAG, String.format("%f %f |  %f, %f", t.amount.doubleValue(),
                    //            val, top, bottom));
                    //Log.i(TAG, String.format("%f", bw));
                    canvas.drawRect(
                            x,   y + (float) top,
                            x + barWidth,     y + (float) bottom,
                            barPaint) ;

                }
            }
            x += (barWidth + barMargin);
        }

        int captionRow = 0;
        if (selectedDay != null){
            canvas.drawText(String.format("%tF", selectedDay.toDate()), 10, captionRow * 1.5f*captionFontSize + graphHeight, captionStyle);
            captionRow++;
            for(Transaction t:selectedTransactions){
                canvas.drawText(String.format("%-32s %10.2f", t.description,t.amount ), 10, captionRow * 1.5f*captionFontSize + graphHeight, captionStyle);
                captionRow++;
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mxOffset = event.getX();
        invalidate();
        this.getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }
}
