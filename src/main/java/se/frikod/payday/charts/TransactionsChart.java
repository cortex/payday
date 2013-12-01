package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.frikod.payday.Transaction;

public class TransactionsChart {

    private static final String TAG = "Payday.TransactionsChart";
    public float mxOffset = 0;
    public float scale = 1.0f;
    public float translateX = 0;

    int barWidth = 100;
    int barMargin = 10                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                ;

    float captionFontSize = 24f;
    private Map<DateTime, List<Transaction>> transactionsPerDate;
    private BigDecimal maxTrans = BigDecimal.ZERO;
    private BigDecimal minTrans = BigDecimal.ZERO;

    private Paint captionStyle;

    private Paint tickStyle;
    private Paint positiveBarStyle;
    private Paint negativeBarStyle;
    private Paint barDividerStyle;

    private Days days;
    private Scale yScale;
    private DateTime startDate;
    private DateTime endDate;
    private List<Transaction> transactions;

    public TransactionsChart(List<Transaction> transactions) {
        this.transactions = transactions;

        positiveBarStyle = new Paint();
        positiveBarStyle.setColor(Color.HSVToColor(new float[]{150f, 0.6f, 1f}));

        negativeBarStyle = new Paint();
        negativeBarStyle.setColor(Color.HSVToColor(new float[]{340f, 0.6f, 1f}));

        tickStyle = new Paint();
        tickStyle.setColor(Color.HSVToColor(new float[]{0f, 0.0f, .95f}));

        barDividerStyle = new Paint();
        barDividerStyle.setColor(Color.HSVToColor(new float[]{0f, 0.0f, .95f}));
        barDividerStyle.setStyle(Paint.Style.STROKE);
        barDividerStyle.setStrokeWidth(10.0f);
        barDividerStyle.setPathEffect(new DashPathEffect(new float[]{10.0f, 20.0f}, 0));

        captionStyle = new Paint();
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
            if (ts == null) {
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

    public void render(Canvas canvas) {

        if (transactions.size() == 0) {
            return;
        }

        float x = 0;
        float graphHeight = canvas.getHeight() / 2f;

        yScale.update(0, Math.max(Math.abs(minTrans.doubleValue()), maxTrans.doubleValue()), 0, graphHeight / 2.0f);

        //barWidth = canvas.getWidth() / (days.getDays() + 1);
        //barWidth = 100;
        captionFontSize = graphHeight / 20f;
        captionStyle.setTextSize(captionFontSize);
        canvas.drawRect(mxOffset - 10, 0, mxOffset + 10, graphHeight, tickStyle);

        List<Transaction> selectedTransactions = null;
        DateTime selectedDay = null;

        canvas.save();

        canvas.translate(translateX, graphHeight / 2.0f);
        canvas.scale(1, scale);
        canvas.scale(1, -1);

        for (int i = 0; i < days.getDays(); i++) {
            DateTime day = startDate.plus(Days.days(i));
            List<Transaction> dayTransactions = transactionsPerDate.get(day);
            canvas.drawLine(x, (graphHeight / 2f), x, -(graphHeight / 2f), tickStyle);

            float positiveHeight = 0;
            float negativeHeight = 0;

            if (dayTransactions != null) {
                double top, bottom;
                if (mxOffset > x && mxOffset < x + barWidth) {
                    selectedTransactions = dayTransactions;
                    selectedDay = day;
                }
                for (Transaction t : dayTransactions) {
                    Paint barPaint;
                    double val = yScale.apply(t.amount);

                    if (val < 0) {
                        barPaint = negativeBarStyle;
                        top = negativeHeight;
                        bottom = val + negativeHeight;
                        negativeHeight += val;
                    } else {
                        barPaint = positiveBarStyle;
                        top = val + positiveHeight;
                        bottom = positiveHeight;
                        positiveHeight += val;
                    }

                    //Log.i(TAG, String.format("%f %f |  %f, %f", t.amount.doubleValue(),
                    //            val, top, bottom));
                    //Log.i(TAG, String.format("%f", bw));


                    canvas.drawRect(
                            x, (float) bottom,
                            x + barWidth, (float) top,
                            barPaint);


                    canvas.drawLine(x, (float) bottom,x + barWidth, (float) bottom, barDividerStyle);
                    canvas.drawLine(x, (float) top,x + barWidth, (float) top, barDividerStyle);
                }
                x += (barWidth + barMargin);
            }

        }

        canvas.restore();

        int captionRow = 0;
        if (selectedDay != null) {
            canvas.drawText(String.format("%tF", selectedDay.toDate()), 10, captionRow * 1.5f * captionFontSize + graphHeight, captionStyle);
            captionRow++;
            for (Transaction t : selectedTransactions) {
                canvas.drawText(String.format("%-32s %10.2f", t.description, t.amount), 10, captionRow * 1.5f * captionFontSize + graphHeight, captionStyle);
                captionRow++;
            }
        }
    }
}
