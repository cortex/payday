package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.Log;
import android.view.animation.OvershootInterpolator;
import android.widget.OverScroller;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ValueAnimator;

import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.frikod.payday.Transaction;
import se.frikod.payday.TransactionsGraphView;

enum ChartType {STACKED, STACKED_DATE, GROUPED, GROUPED_DATE;
    public ChartType next() {
        //return values()[(ordinal() + 1) % values().length];
        if (this == STACKED_DATE){
            return GROUPED_DATE;
        }else{
            return STACKED_DATE;
        }

    }
}

class Transactions{
    // Holds and calculates stats for transactions
    private List<Transaction> transactions;
    LocalDate startDate;
    LocalDate endDate;
    BigDecimal maxTrans = BigDecimal.ZERO;
    BigDecimal minTrans = BigDecimal.ZERO;
    Map<LocalDate, List<Transaction>> transactionsPerDate;


    Transactions(List<Transaction> transactions){
        startDate = transactions.get(0).date;
        endDate = transactions.get(0).date;

        transactionsPerDate = new HashMap<LocalDate, List<Transaction>>();

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
    }
}

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

    private List<Bar> bars;
    private Bar lastSelected;
    private TransactionsGraphView mView;
    private Bar selected = null;
    private Matrix frameMatrix;
    private Matrix plotMatrix;
    private Axis yAxis;
    private DateTicks dateTicks;
    private Selector selector;

    private ChartType chartType;


    private Transactions transactions;
    Scale yScale;
    float screenDensity;

    private OverScroller mScroller;
    private ValueAnimator mScrollAnimator;

    Map<ChartType, Float> chartWidths;

    public TransactionsChart(TransactionsGraphView view, List<Transaction> transactions) {
        this.mView = view;
        this.transactions = new Transactions(transactions);
        this.screenDensity = view.getResources().getDisplayMetrics().density;

        this.dateTicks = new DateTicks(transactions.size(), screenDensity);

        this.caption = new Caption(this.mView);
        this.selector = new Selector();
        this.plotMatrix = new Matrix();
        this.frameMatrix = new Matrix();
        this.width = view.getWidth();
        this.height = view.getHeight();


        this.yScale = new Scale();
        this.yAxis = new Axis(zoom, yScale, screenDensity);

        resize(width, height);
        updateMatrix();
        initGraph();

        chartType = ChartType.STACKED;
        setZoom(zoom);
        updateSelected();
        mScroller = new OverScroller(view.context, new OvershootInterpolator(100f));
        //mScroller = new OverScroller(view.context, new BounceInterpolator());

        //mScroller = new Scroller(view.context, null, true);
        mScrollAnimator = ValueAnimator.ofFloat(0, 1);
        mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
               // Log.i(TAG, "Scrolling " + mScrollAnimator.getAnimatedValue());

                if (!mScroller.isFinished()) {
                    mScroller.computeScrollOffset();
                    Log.i(TAG, "Scrolling " + mScroller.getCurrX());

                    setTranslate(mScroller.getCurrX(), getTranslateY());

                } else {
                    mScrollAnimator.cancel();
                   // Log.i(TAG, "Scrolling done");
                }
                //mView.invalidate();
            }
        });

    }



    private void initGraph() {

        Days days;
        LocalDate axisStartDate = transactions.startDate.minusDays(5);
        LocalDate axisEndDate = transactions.endDate.plusDays(5);
        days = Days.daysBetween(axisStartDate, axisEndDate);

        EnumMap<ChartType, Float> currentX = new EnumMap<ChartType, Float>(ChartType.class);
        EnumMap<ChartType, Float> prevX = new EnumMap<ChartType, Float>(ChartType.class);

        prevX.put(ChartType.STACKED, 0f);
        prevX.put(ChartType.GROUPED, 0f);
        prevX.put(ChartType.GROUPED_DATE, 0f);
        prevX.put(ChartType.STACKED_DATE, 0f);

        currentX.put(ChartType.STACKED, 0f);
        currentX.put(ChartType.GROUPED, 0f);
        currentX.put(ChartType.GROUPED_DATE, 0f);
        currentX.put(ChartType.STACKED_DATE, 0f);

        bars = new ArrayList<Bar>();

        float barWidth = 20 * screenDensity;
        float barMargin = 2 * screenDensity;

        float inc = (barWidth + barMargin);
        for (int i = 0; i <= days.getDays(); i++) {
            LocalDate day = axisStartDate.plusDays(i);
            List<Transaction> dayTransactions = transactions.transactionsPerDate.get(day);

            float positiveHeight = 0;
            float negativeHeight = 0;

            Bar bar;
            if (dayTransactions == null) {
                currentX.put(ChartType.STACKED_DATE, currentX.get(ChartType.STACKED_DATE) + inc);
                currentX.put(ChartType.GROUPED_DATE, currentX.get(ChartType.GROUPED_DATE) + inc);
            } else {
                currentX.put(ChartType.STACKED, currentX.get(ChartType.STACKED) + inc);
                currentX.put(ChartType.STACKED_DATE, currentX.get(ChartType.STACKED_DATE) + inc);

                for (Transaction t : dayTransactions) {
                    currentX.put(ChartType.GROUPED, currentX.get(ChartType.GROUPED) + inc);
                    currentX.put(ChartType.GROUPED_DATE, currentX.get(ChartType.GROUPED_DATE) + inc);

                    double val = t.amount.doubleValue();
                    bar = new Bar(currentX, barWidth, (float) val, positiveHeight, negativeHeight);
                    bar.dayTransactions = dayTransactions;
                    bar.transaction = t;
                    bar.date = day;

                    positiveHeight = Math.min(positiveHeight, bar.rect.top);
                    negativeHeight = Math.max(negativeHeight, bar.rect.bottom);
                    bars.add(bar);
                }
            }

            DateTick dateTick = new DateTick(day);
            dateTick.coords.put(ChartType.GROUPED, new RectF(prevX.get(ChartType.GROUPED) + inc, 0, currentX.get(ChartType.GROUPED) + inc, 0));
            dateTick.coords.put(ChartType.STACKED, new RectF(prevX.get(ChartType.STACKED) + inc, 0, currentX.get(ChartType.STACKED) + inc, 0));
            dateTick.coords.put(ChartType.GROUPED_DATE, new RectF(prevX.get(ChartType.GROUPED_DATE) + inc, 0, currentX.get(ChartType.GROUPED_DATE) + inc, 0));
            dateTick.coords.put(ChartType.STACKED_DATE, new RectF(prevX.get(ChartType.STACKED_DATE) + inc, 0, currentX.get(ChartType.STACKED_DATE) + inc, 0));

            dateTick.currentRect = new RectF(prevX.get(ChartType.STACKED), 0, currentX.get(ChartType.STACKED), 0);

            dateTicks.ticks.add(dateTick);

            prevX = currentX.clone();

            /*dateTicks.coords.get(ChartType.GROUPED).add();
            dateTicks.coords.get(ChartType.STACKED).add(new PointF(stackedX, 0));
            dateTicks.coords.get(ChartType.GROUPED_DATE).add(new PointF(groupDateX, 0));
            dateTicks.coords.get(ChartType.STACKED_DATE).add(new PointF(stackedDateX, 0));
            dateTicks.dates.add(day);*/

        }
        Log.i(TAG, "Size:" + dateTicks.ticks.size());
        chartWidths = new EnumMap<ChartType, Float>(ChartType.class);
        for (Map.Entry<ChartType, RectF> entry: dateTicks.ticks.get(dateTicks.ticks.size()-1).coords.entrySet()){
            chartWidths.put(entry.getKey(), entry.getValue().right);
        }

    }

    public float getZoom() {
        return this.zoom;
    }

    private void setZoom(float zoom) {
        this.zoom = zoom;
        this.yAxis.zoom = zoom;
        Log.i(TAG, "Zoom: " + zoom);
        yScale.update(0,
                Math.max(Math.abs(transactions.minTrans.doubleValue()), transactions.maxTrans.doubleValue()),
                0, zoom *Math.max(Math.abs(transactions.minTrans.doubleValue()), transactions.maxTrans.doubleValue()));
        Log.i(TAG, "" + yScale);
        this.yAxis.calcStep(this.height);


        Bar.setBorderWidth(5f / zoom);
        updateMatrix();
    }

    public void setManualZoom(float zoom) {
        this.zoomState = true;
        this.manualZoom = zoom;
        if (this.manualZoom > maxZoom) {
            this.manualZoom = maxZoom;
        }
        if (this.manualZoom < minZoom) {
            this.manualZoom = minZoom;
        }
        this.setZoom(this.manualZoom);
    }

    public float getTranslateY() {
        return this.translateY;
    }

    public float getTranslateX() {
        return this.translateX;
    }

    public void setTranslate(float x, float y) {
        this.translateY = y;
        this.translateX = x;
        updateSelected();
        updateMatrix();
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void updateMatrix() {
        frameMatrix.reset();
        frameMatrix.postTranslate(0, translateY);

        plotMatrix.reset();
        plotMatrix.postConcat(frameMatrix);
        plotMatrix.postTranslate(width / 2f + translateX, 0);
        plotMatrix.preScale(1, zoom);

        mView.invalidate();
    }

    public void toggleZoom() {
        if (zoomState) {
            zoomState = false;
            scaleToFit(false);
        } else {
            zoomState = true;
            zoomAnimation(manualZoom);
        }
    }

    public void scaleToFit(Boolean scaleSelectedOnly) {
        RectF r = new RectF();
        for (final Bar bar : bars) {
            if (!scaleSelectedOnly || bar.selected) {
                r.union(bar.rect);
            }
        }

        float maxh = Math.max(-r.top, r.bottom);
        if (maxh != 0) {
            float newZoom = (width / 2.0f) / (1.2f * maxh);
            Log.d(TAG, (String.format("Max %s", maxh)));
            Log.d(TAG, (String.format("New zoom %s", newZoom)));
            Log.d(TAG, (String.format("Current zoom %s", zoom)));
            zoomAnimation(newZoom);
        }
    }

    public void zoomAnimation(float newZoom) {
        ValueAnimator animation = ValueAnimator.ofFloat(getZoom(), newZoom);
        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setZoom((Float) valueAnimator.getAnimatedValue());
                mView.invalidate();
            }
        });
        animation.setDuration(700);
        animation.start();
    }

    public void chartTypeAnimation() {
        //this.translateX = 0;
        //this.updateMatrix();


        float mindx = Float.POSITIVE_INFINITY;
        PointF targetX = new PointF();
        chartType = chartType.next();
        Bar closest = null;

        boolean direction = true;

        for (Bar bar : bars) {
            PointF targetPos = new PointF();

            if (chartType == ChartType.STACKED) {
                direction = true;
                targetPos = bar.stackedPos;
            }
            if (chartType == ChartType.GROUPED) {
                direction = false;
                targetPos = bar.groupedPos;
            }
            if (chartType == ChartType.STACKED_DATE) {
                direction = true;
                targetPos = bar.stackedDatePos;
            }
            if (chartType == ChartType.GROUPED_DATE) {
                direction = false;
                targetPos = bar.groupedDatePos;
            }
            RectF deviceBarRect = new RectF();
            plotMatrix.mapRect(deviceBarRect, bar.rect);
            float dx = deviceBarRect.centerX() - this.selector.selectorX;

            if (Math.abs(dx) < mindx) {
                mindx = Math.abs(dx);
                float[] pts = new float[1];
                pts[0] = targetPos.x;
                //pts[1] = targetPos.y;

                plotMatrix.mapPoints(pts);
                targetX.x = pts[0] - dx;
                closest = bar;
                //Log.i(TAG,String.format("Tx %s, targetxd %s", translateX, pts[0]) );
                //targetX.x += dx;
            }
            direction = !direction;
            bar.animateTo(mView, targetPos, direction).start();
        }
        //assert closest != null;
        //Log.i(TAG, String.format("Closest bar %s %s", closest.transaction.description, mindx));
        dateTicks.animateTo(chartType).start();
        //Animator translateAnim = snapAnimation(targetX.x);
        //translateAnim.start();
    }

    public Animator snapAnimation(float targetX) {

        ValueAnimator animation = ValueAnimator.ofFloat(translateX, targetX);

        animation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                setTranslate((Float) valueAnimator.getAnimatedValue(), translateY);
                mView.invalidate();
            }
        });
        return animation;
    }

    public void flingAnimation(float velocityX) {

        //Log.i(TAG, "Start x" + getTranslateX());
        float SCALE = 1;
        int minX = -chartWidths.get(chartType).intValue();
        int minY = 0;
        int maxX = 0;
        int maxY = 0;
        int velX = (int)(velocityX/SCALE);
        int duration = 1000;
        mScroller.fling(
                (int)getTranslateX(),
                0,
                velX,0,
        minX,maxX,minY,maxY);
        mScrollAnimator.setDuration(duration);
        mScrollAnimator.start();

        mView.invalidate();
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
            this.width = w / 2;
            this.height = h;
        }

        translateY = (this.height / 2);
        yAxis.resize(width, height);
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

        canvas.save();
        canvas.concat(frameMatrix);

        yAxis.drawBackground(canvas);

        canvas.translate(width / 2f + translateX, 0);

        dateTicks.draw(canvas, chartType);

        canvas.restore();

        canvas.save();
        canvas.concat(plotMatrix);
        for (Bar bar:bars){
            bar.draw(canvas);
        }
        canvas.restore();

        canvas.save();
        canvas.concat(frameMatrix);
        yAxis.drawForeground(canvas);
        canvas.restore();

        selector.draw(canvas);

        if (selected != null) {
            caption.draw(canvas, selected, chartType);
        }

    }
}
