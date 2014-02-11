package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.animation.BounceInterpolator;

import com.nineoldandroids.animation.Animator;
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

enum ChartType {STACKED, STACKED_DATE, GROUPED, GROUPED_DATE;
    public ChartType next() {
        return values()[(ordinal() + 1) % values().length];
    }
};

public class TransactionsChart {

    private static final String TAG = "Payday.TransactionsChart";
    private final Caption caption;
    public float mxOffset = 0;

    private float minZoom = 0.001f;
    private float maxZoom = 2000f;

    private float zoom = 5f;
    private float manualZoom = 5f;
    private boolean zoomState = true;

    private float translateX = 0;
    private float translateY = 0;

    private int width;
    private int height;

    private List<Transaction> transactions;
    private List<Bar> bars;
    private Bar lastSelected;
    private TransactionsGraphView view;
    private Bar selected = null;
    private Matrix frameMatrix;
    private Matrix plotMatrix;
    private Axis axis;
    private Selector selector;

    private ChartType chartType;

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
        this.selector = new Selector();
        this.plotMatrix = new Matrix();
        this.frameMatrix = new Matrix();
        this.width = view.getWidth();
        this.height = view.getHeight();
        resize(width, height);

        float stackedX = 0;
        float groupedX = 0;
        float stackedDateX = 0;
        float groupDateX = 0;

        bars = new ArrayList<Bar>();

        for (int i = 0; i<=days.getDays(); i++) {
            DateTime day = startDate.plus(Days.days(i));
            List<Transaction> dayTransactions = transactionsPerDate.get(day);

            float positiveHeight = 0;
            float negativeHeight = 0;

            Bar bar;

            if (dayTransactions != null) {
                assert !dayTransactions.isEmpty();
                for (Transaction t : dayTransactions) {
                    float val = (float) yScale.apply(t.amount);
                    bar = new Bar(stackedX, groupedX, stackedDateX, groupDateX, val, positiveHeight, negativeHeight);
                    bar.dayTransactions = dayTransactions;
                    bar.transaction = t;
                    bar.date = day;

                    positiveHeight = Math.min(positiveHeight, bar.rect.top);
                    negativeHeight = Math.max(negativeHeight, bar.rect.bottom);
                    bars.add(bar);
                    groupedX += (Bar.width + Bar.margin);
                    groupDateX += (Bar.width + Bar.margin);
                }
                stackedX += (Bar.width + Bar.margin);
            }
            stackedDateX += (Bar.width + Bar.margin);
            groupDateX += (Bar.width + Bar.margin);

        }
        chartType = ChartType.STACKED;
        setZoom(zoom);
        updateMatrix();
        updateSelected();
    }

    public float getZoom(){
        return this.zoom;
    }

    private void setZoom(float zoom){
        this.zoom = zoom;
        this.axis.zoom = zoom;
        Bar.setBorderWidth(5f/zoom);
        updateMatrix();
    }

    public void setManualZoom(float zoom){
        this.zoomState = true;
        this.manualZoom = zoom;
        if (this.manualZoom > maxZoom){
            this.manualZoom = maxZoom;
        }
        if (this.manualZoom < minZoom){
            this.manualZoom = minZoom;
        }
        this.setZoom(this.manualZoom);
    }

    public float getTranslateY(){
        return this.translateY;
    }
    public float getTranslateX() { return this.translateX; }

    public void setTranslate(float x, float y){
        this.translateY = y;
        this.translateX = x;
        updateSelected();
        updateMatrix();
    }

    public void setWidth(int width){
        this.width = width;
    }

    public void updateMatrix(){
        frameMatrix.reset();
        //frameMatrix.postScale(zoom, 1);
        frameMatrix.postTranslate(0, translateY);

        plotMatrix.reset();

        plotMatrix.postConcat(frameMatrix);
        plotMatrix.postTranslate(width / 2f + translateX, 0);
        plotMatrix.preScale(1, zoom);

        //graphMatrix.preScale(zoom, 1);
        // graphMatrix.preRotate(90f);
        view.invalidate();
    }

    public void toggleZoom(){
        if ( zoomState){
            zoomState = false;
            scaleToFit(false);
        }else{
            zoomState = true;
            zoomAnimation(manualZoom);
        }
    }

    public void scaleToFit(Boolean scaleSelectedOnly){
        RectF r = new RectF();
        for (final Bar bar: bars){
            if (!scaleSelectedOnly || bar.selected){
                r.union(bar.rect);
            }
        }

        float maxh = Math.max(-r.top, r.bottom);
        if (maxh != 0){
            float newZoom = (width / 2.0f) / (1.2f * maxh);
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

    public void groupedAnimation(){
        chartType = chartType.next();
        for (Bar bar: bars){
             if (chartType == ChartType.STACKED)
                bar.animateTo(view, bar.stackedPos).start();

             if (chartType == ChartType.GROUPED)
                 bar.animateTo(view, bar.groupedPos).start();

            if (chartType == ChartType.STACKED_DATE)
                bar.animateTo(view, bar.stackedDatePos).start();

            if (chartType == ChartType.GROUPED_DATE)
                bar.animateTo(view, bar.groupedDatePos).start();
        }

    }

    public void snapAnimation(float targetX){

        ValueAnimator animation = ValueAnimator.ofFloat(translateX, targetX);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setTranslate((Float) valueAnimator.getAnimatedValue(), translateY);
                view.invalidate();
            }
        });
        animation.setDuration(300);
        animation.setInterpolator(new BounceInterpolator());
        animation.start();
    }

    public void snap(){
        //if (selector.selectedDeviceRect != null)
        //    snapAnimation(translateY + selector.selectorX - selector.selectedDeviceRect.centerY());
    }

    public void resize(int w, int h){
        if (h > w) {
            this.width = w;
            this.height = h;
        }else{
            this.width = w /2;
            this.height = h;
        }

        this.translateY = (this.height / 2);

        this.axis.width = this.width;
        this.axis.height = this.height;

        caption.resize(w, h);
        selector.resize(w, h);
        updateMatrix();
     }

    public void updateSelected(){
        selected = selector.getSelected(bars, plotMatrix);
        if (lastSelected != selected){
            //Vibrator vibrator = (Vibrator) view.context.getSystemService(Context.VIBRATOR_SERVICE);
            //vibrator.vibrate(20);
            lastSelected = selected;
        }
    }

    public void render(Canvas canvas) {
        if (transactions.size() == 0) {
            return;
        }

        canvas.save();
        canvas.concat(plotMatrix);

        for (Bar bar:bars){
            bar.draw(canvas);
        }
        canvas.restore();
        canvas.save();

        canvas.concat(frameMatrix);
        axis.draw(canvas);

        canvas.restore();
        selector.draw(canvas);

        if (selected != null) {
            caption.draw(canvas, selected, chartType);
        }

    }
}
