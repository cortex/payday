package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;

import com.nineoldandroids.animation.ValueAnimator;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.frikod.payday.Transaction;

enum Direction {POSITIVE, NEGATIVE}

class Bar{
    private final Direction direction;
    private Paint fill;
    private Paint selectedFill;
    private Paint border;
    public RectF targetRect;
    public RectF rect;
    public boolean selected;
    public List<Transaction> dayTransactions;

    DateTime date;

    Bar(RectF rect, Paint fill, Paint selectedFill,Direction direction, Paint border){
        this.targetRect = rect;
        this.rect = new RectF(targetRect);
        if (direction == Direction.POSITIVE)
            this.rect = new RectF(rect.left, rect.bottom, rect.right,rect.bottom);
        if (direction == Direction.NEGATIVE)
            this.rect = new RectF(rect.left, rect.top, rect.right,rect.top);

        this.fill = fill;
        this.border = border;
        this.selectedFill = selectedFill;
        this.direction = direction;
    }

    public void scaleHeight(float value){

        if (direction == Direction.POSITIVE)
            rect.top = targetRect.bottom +  (targetRect.top-targetRect.bottom) * value;

         if (direction == Direction.NEGATIVE)
            rect.bottom = targetRect.top + (targetRect.bottom-targetRect.top) * value;

    }

    public void draw(Canvas canvas){
        if (selected)
            canvas.drawRect(rect, selectedFill);
        else
            canvas.drawRect(rect, fill);

        if (direction == Direction.POSITIVE)
            canvas.drawLine(rect.left, rect.top, rect.right, rect.top, border);
        if (direction == Direction.NEGATIVE)
            canvas.drawLine(rect.left, rect.bottom , rect.right, rect.bottom, border);
    }
}

public class TransactionsChart {

    private static final String TAG = "Payday.TransactionsChart";
    public float mxOffset = 0;
    public float scale = 1.0f;
    public float translateX = 0;

    int barWidth = 100;
    int barMargin = 10;

    float captionFontSize = 24f;
    private Map<DateTime, List<Transaction>> transactionsPerDate;
    private BigDecimal maxTrans = BigDecimal.ZERO;
    private BigDecimal minTrans = BigDecimal.ZERO;

    private Paint captionStyle;

    private Paint tickStyle;
    private Paint positiveBarStyle;
    private Paint positiveBarBorderStyle;
    private Paint positiveBarSelectedStyle;

    private Paint negativeBarStyle;
    private Paint negativeBarBorderStyle;
    private Paint negativeBarSelectedStyle;

    private Days days;
    private Scale yScale;
    private DateTime startDate;
    private DateTime endDate;
    private List<Transaction> transactions;
    private List<Bar> bars;

    private View view;

    public TransactionsChart(View view, List<Transaction> transactions) {
        this.view = view;
        this.transactions = transactions;

        positiveBarStyle = new Paint();
        positiveBarStyle.setColor(Color.HSVToColor(new float[]{150f, 0.4f, 1f}));
        positiveBarBorderStyle = new Paint();
        positiveBarBorderStyle.setStrokeWidth(10.0f);
        positiveBarBorderStyle.setColor(Color.HSVToColor(new float[]{150f, 0.6f, 1f}));
        positiveBarSelectedStyle= new Paint();
        positiveBarSelectedStyle.setColor(Color.HSVToColor(new float[]{150f, 0.2f, 1f}));

        negativeBarStyle = new Paint();
        negativeBarStyle.setColor(Color.HSVToColor(new float[]{340f, 0.4f, 1f}));
        negativeBarBorderStyle = new Paint();
        negativeBarBorderStyle.setStrokeWidth(10.0f);
        negativeBarBorderStyle.setColor(Color.HSVToColor(new float[]{340f, 0.6f, 1f}));
        negativeBarSelectedStyle= new Paint();
        negativeBarSelectedStyle.setColor(Color.HSVToColor(new float[]{340f, 0.2§§§f, 1f}));

        tickStyle = new Paint();
        tickStyle.setColor(Color.HSVToColor(new float[]{0f, 0.0f, .95f}));
        tickStyle.setAlpha(128);
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

        makeBars();
    }

    public void makeBars(){
        float x = 0;

        //yScale.update(0, Math.max(Math.abs(minTrans.doubleValue()), maxTrans.doubleValue()), 0, scale * graphHeight / 2.0f);
        yScale.update(0, Math.max(Math.abs(minTrans.doubleValue()), maxTrans.doubleValue()), 0, scale * 100.0f);
        //barWidth = canas.getWidth() / (days.getDays() + 1);l
        //barWidth = 100;

        //canvas.drawRect(mxOffset - 10, 0, mxOffset + 10, graphHeight, tickStyle);
        bars = new ArrayList<Bar>();

        Path p = new Path();
        for (int i = 0; i < days.getDays(); i++) {
            DateTime day = startDate.plus(Days.days(i));
            List<Transaction> dayTransactions = transactionsPerDate.get(day);

            float positiveHeight = 0;
            float negativeHeight = 0;
            Bar bar;
            if (dayTransactions != null) {

                for (Transaction t : dayTransactions) {
                    p.reset();

                    float val = (float) yScale.apply(t.amount);

                    if (val > 0) {
                        bar = new Bar(
                                new RectF(
                                        x, positiveHeight - val,
                                        x + barWidth, positiveHeight),
                                positiveBarStyle,
                                positiveBarSelectedStyle,
                                Direction.POSITIVE,
                                positiveBarBorderStyle);
                        positiveHeight -= val;
                    } else {
                        bar = new Bar(
                                new RectF(
                                        x,  negativeHeight,
                                        x + barWidth, negativeHeight - val),
                                negativeBarStyle,
                                negativeBarSelectedStyle,
                                Direction.NEGATIVE,
                                negativeBarBorderStyle);
                        negativeHeight -= val;
                    }

                    bar.dayTransactions = dayTransactions;
                    bar.date = day;
                    bars.add(bar);
                }
                x += (barWidth + barMargin);
            }

        }


    }

    public void findSelected(){
        List<Transaction> selectedTransactions = null;
        DateTime selectedDay = null;
/*
        if (mxOffset > x && mxOffset < x + barWidth) {
            selectedTransactions = dayTransactions;
  l          selectedDay = day;
        }*/
    }



    public void drawCaption(Canvas canvas, float height, Bar selected){
        int captionRow = 0;
        captionFontSize = height / 15f;
        captionStyle.setTextSize(captionFontSize);
        float top = height + 300;
        String format = "%-32s %10.2f";
        if (selected != null) {
            Rect rowBound = new Rect();
            String str = String.format(format, 0f,0f);
            captionStyle.getTextBounds(str, 0, str.length(), rowBound);

            canvas.drawRect(
                    5, top,
                    5+ rowBound.width(),
                    captionStyle.getFontSpacing() * 15,
                    tickStyle);

            canvas.drawText(String.format("%tF", selected.date.toDate()),
                    10, captionRow * 1.5f * captionFontSize + top, captionStyle);
            captionRow++;
            for (Transaction t : selected.dayTransactions) {
                canvas.drawText(String.format(format, t.description, t.amount),
                        10, captionRow * 1.5f * captionFontSize + top,
                        captionStyle);
                captionRow++;
            }
        }
    }

    public void initialAnimation(){
        int i = 0;
        for (final Bar bar: bars){
            ValueAnimator animation = ValueAnimator.ofFloat(0f, 10f);
            animation.setStartDelay(i);
            i+=50;
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    bar.scaleHeight((Float) valueAnimator.getAnimatedValue());
                    view.invalidate();
                }
            });
            animation.setDuration(300);
            animation.start();
        }
    }

    public void render(Canvas canvas) {
        float graphHeight = canvas.getHeight() / 2f;

        canvas.save();

        canvas.translate(translateX, graphHeight / 2.0f);
        canvas.scale(scale, scale);
        Matrix m = canvas.getMatrix();
        if (transactions.size() == 0) {
            return;
        }

        Bar selected = null;

        for (Bar bar:bars){
            RectF localRect = new RectF();
            m.mapRect(localRect, bar.rect);
            bar.selected = localRect.contains(mxOffset, localRect.centerY());
            if (bar.selected) selected = bar;
            bar.draw(canvas);
        }

        canvas.restore();
        drawCaption(canvas, graphHeight/2,  selected);
    }
}
