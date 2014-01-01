package se.frikod.payday.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.Log;
import android.view.animation.BounceInterpolator;

import com.nineoldandroids.animation.ValueAnimator;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.frikod.payday.Transaction;
import se.frikod.payday.TransactionsGraphView;

public class TransactionsChart {

    private static final String TAG = "Payday.TransactionsChart";
    private final float selectorY = 300f;
    private final Caption caption;
    public float mxOffset = 0;
    int barWidth = 50;
    int barMargin = 10;
    private float zoom = 5f;
    private float translateX = 0;
    private float translateY = 0;
    private float width;

    private static Paint selectorStyle;

    private List<Transaction> transactions;
    private List<Bar> bars;
    private Bar lastSelected;
    private TransactionsGraphView view;
    private Bar selected = null;
    private RectF selectedDeviceRect = null;
    private Matrix graphCoords;
    private Axis axis;

    static {
        selectorStyle = new Paint();
        selectorStyle.setColor(Color.HSVToColor(new float[]{220f, 0.0f, .70f}));
        selectorStyle.setAlpha(220);
        selectorStyle.setStrokeWidth(2f);
        selectorStyle.setTextSize(30f);
        selectorStyle.setAntiAlias(true);
    }

    public TransactionsChart(TransactionsGraphView view, List<Transaction> transactions) {
        this.view = view;
        this.transactions = transactions;

        Days days;
        Scale yScale;
        DateTime startDate;
        DateTime endDate;

        Map<DateTime, List<Transaction>> transactionsPerDate;

        BigDecimal maxTrans = BigDecimal.ZERO;
        BigDecimal minTrans = BigDecimal.ZERO;

        startDate = transactions.get(0).date;
        endDate = transactions.get(0).date;

        transactionsPerDate = new HashMap<DateTime, List<Transaction>>();

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
        }

        days = Days.daysBetween(startDate, endDate);

        yScale = new Scale();
        yScale.update(0, Math.max(Math.abs(minTrans.doubleValue()), maxTrans.doubleValue()), 0, zoom * 100.0f);

        this.caption = new Caption(this.view);
        this.axis = new Axis(zoom, yScale);
        this.graphCoords = new Matrix();

        float x = 0;

        bars = new ArrayList<Bar>();

        Path p = new Path();
        for (int i = days.getDays(); i >= 0; i--) {
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
                                Direction.POSITIVE);
                        positiveHeight -= val;
                    } else {
                        bar = new Bar(
                                new RectF(
                                        x,  negativeHeight,
                                        x + barWidth, negativeHeight - val),
                                Direction.NEGATIVE
                        );
                        negativeHeight -= val;
                    }

                    bar.dayTransactions = dayTransactions;
                    bar.date = day;
                    bars.add(bar);
                }
                x += (barWidth + barMargin);
            }

        }
        resize(view.getWidth(), view.getHeight());
        setZoom(zoom);
        updateMatrix();
    }

    public float getZoom(){
        return this.zoom;
    }

    public void setZoom(float zoom){
        this.zoom = zoom;
        this.axis.zoom = zoom;
        Bar.setBorderWidth(5f/zoom);
        updateMatrix();
    }

    public float getTranslateY(){
        return this.translateY;
    }

    public void setTranslateY(float y){
        this.translateY = y;
        updateSelected();
        updateMatrix();
    }

    public void setWidth(float width){
        this.width = width;
    }

    public void updateMatrix(){
        graphCoords.reset();
        graphCoords.preTranslate(width / 2f + translateX, translateY);
        graphCoords.preScale(zoom, 1);
        graphCoords.preRotate(90f);
        view.invalidate();
    }

    public void scaleToFit(){
        RectF r = new RectF();
        for (final Bar bar: bars){
            if (bar.selected){
                r.union(bar.rect);
            }
        }

        float maxh = Math.max(-r.top, r.bottom);
        if (maxh != 0){
            float newZoom = (width / 2.0f) / (1.4f * maxh);
            Log.d(TAG, (String.format("Max %s", maxh)));
            Log.d(TAG, (String.format("New zoom %s", newZoom)));
            Log.d(TAG, (String.format("Current zoom %s", zoom)));
            zoomAnimation(newZoom);
        }
    }

    public void zoomAnimation(float newZoom){
        ValueAnimator animation = ValueAnimator.ofFloat(getZoom(), newZoom);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setZoom( (Float) valueAnimator.getAnimatedValue());
                view.invalidate();
            }
        });
        animation.setDuration(700);
        animation.start();
    }

    public void initialAnimation(){
        int i = 0;
        for (final Bar bar: bars){
            bar.scaleHeight(0);
            ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
            animation.setStartDelay(i);
            i+=20;
            animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    bar.scaleHeight((Float) valueAnimator.getAnimatedValue());
                    view.invalidate();
                }
            });
            animation.setDuration(700);
            animation.setInterpolator(new BounceInterpolator());
            animation.start();
        }
    }

    public void snapAnimation(float targetY){

        ValueAnimator animation = ValueAnimator.ofFloat(translateY, targetY);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setTranslateY((Float) valueAnimator.getAnimatedValue());
                view.invalidate();
            }
        });
        animation.setDuration(300);
        animation.setInterpolator(new BounceInterpolator());
        animation.start();
    }

    public void snap(){
        if (selectedDeviceRect != null)
            snapAnimation(translateY + selectorY - selectedDeviceRect.centerY());
    }

    public void resize(int w, int h){
        this.width = w;
        caption.resize(w, h);
        updateMatrix();
     }

    public void updateSelected(){
        for (Bar bar:bars){
            RectF deviceRect = new RectF();
            graphCoords.mapRect(deviceRect, bar.rect);

            bar.selected  = deviceRect.contains(deviceRect.centerX(), selectorY);

            if (bar.selected){
                selected = bar;
                selectedDeviceRect = deviceRect;
            }
        }

        if (lastSelected != selected){
            Vibrator vibrator = (Vibrator) view.context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(20);
            lastSelected = selected;
        }
    }

    public void render(Canvas canvas) {
        float tickHeight = 30;

        canvas.save();
        canvas.concat(graphCoords);

        if (transactions.size() == 0) {
            return;
        }

        for (Bar bar:bars){
            bar.draw(canvas);
        }

        canvas.restore();
        axis.draw(canvas, graphCoords);

        Path p = new Path();
        p.moveTo(0, selectorY - tickHeight);
        p.lineTo(tickHeight, selectorY);
        p.lineTo(0, selectorY + tickHeight);
        canvas.drawPath(p, selectorStyle);

        p.reset();
        p.moveTo(canvas.getWidth(), selectorY - tickHeight);
        p.lineTo(canvas.getWidth() - tickHeight, selectorY);
        p.lineTo(canvas.getWidth(), selectorY + tickHeight);

        canvas.drawPath(p, selectorStyle);
        canvas.drawLine(0, selectorY, canvas.getWidth(), selectorY, selectorStyle);

        if (selected != null) {
            caption.draw(canvas, selected);
        }
    }
}
