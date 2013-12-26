package se.frikod.payday.charts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
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

enum Direction {POSITIVE, NEGATIVE}

class Bar{
    public Direction direction;
    private Paint fill;
    private Paint selectedFill;
    private Paint border;
    public RectF targetRect;
    public RectF rect;
    public boolean selected;
    public List<Transaction> dayTransactions;

    DateTime date;

    Bar(RectF rect, Paint fill, Paint selectedFill, Paint border){
        this.targetRect = rect;
        this.rect = new RectF(targetRect);
        this.fill = fill;
        this.border = border;
        this.selectedFill = selectedFill;
    }

    public void scaleHeight(float value){
        if (direction == Direction.POSITIVE)
            rect.top = targetRect.bottom + (targetRect.top-targetRect.bottom) * value;
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


class PositiveBar extends Bar{
    PositiveBar(RectF rect, Paint fill, Paint selectedFill, Paint border){
        super(rect, fill, selectedFill, border);
        this.direction = Direction.POSITIVE;
        this.rect = new RectF(rect.left, rect.bottom, rect.right,rect.bottom);
    }
}

class NegativeBar extends Bar{
    NegativeBar(RectF rect, Paint fill, Paint selectedFill, Paint border){
        super(rect, fill, selectedFill, border);
        this.direction = Direction.NEGATIVE;
        this.rect = new RectF(rect.left, rect.top, rect.right,rect.top);
    }
}


class Caption{

    private Paint captionTextStyle;
    private Paint captionAmountStyle;
    private Paint captionBackgroundStyle;
    private Paint captionOverlayStyle;

    private ScriptIntrinsicBlur theIntrinsic;
    private final RenderScript rs;

    Caption(TransactionsGraphView view){
        float captionFontSize = view.getWidth() / 80f;
        captionTextStyle = new Paint();
        captionTextStyle.setColor(Color.DKGRAY);
        captionTextStyle.setTextSize(captionFontSize);
        Typeface textFont = Typeface.createFromAsset(view.context.getAssets(), "fonts/Roboto-Regular.ttf");

        captionTextStyle.setTypeface(textFont);
        captionTextStyle.setAntiAlias(true);

        captionAmountStyle = new Paint();
        captionAmountStyle.setColor(Color.DKGRAY);
        captionAmountStyle.setTextSize(captionFontSize);
        captionAmountStyle.setTypeface(Typeface.createFromAsset(view.context.getAssets(), "fonts/Roboto-Light.ttf"));
        captionAmountStyle.setAntiAlias(true);


        captionBackgroundStyle = new Paint();
        //captionBackgroundStyle.setColor(Color.HSVToColor(new float[]{220f, 0.1f, .90f}));
        //captionBackgroundStyle.setAlpha(100);
        //captionBackgroundStyle.setStrokeWidth(1f);

        captionOverlayStyle = new Paint();
        captionOverlayStyle.setColor(Color.WHITE);
        captionOverlayStyle.setColor(0xFFF5F5FF);
        captionOverlayStyle.setAlpha(190);

        rs = RenderScript.create(view.getContext());
        theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
        theIntrinsic.setRadius(5f);

    }

    public void resize(int w, int h){
        captionTextStyle.setTextSize(w/24);
        captionAmountStyle.setTextSize(w/24);
    }

    public void draw(Canvas canvas, Bitmap bmp, Bar selectedBar, RectF selectedRect){

        int captionRow = 1;
        float margin = 20f;

        String descriptionFormat = "%s";
        String amountFormat = " %10.2f";
        float captionFontSize = captionTextStyle.getTextSize();

        //float textWidth = captionTextStyle.measureText(String.format(format, "test test test test", 10000f));
        //float textWidth = findMaxFontSize(canvas.getWidth() - 4 * margin , captionTextStyle, );

        float top, bottom;
        Path path = new Path();
        float boxHeight = captionTextStyle.getFontSpacing() * (4 +  selectedBar.dayTransactions.size());

        top = canvas.getHeight() / 2f;
        bottom = canvas.getHeight();

        canvas.drawRoundRect(new RectF(
                margin, top,
                (2 * margin) + canvas.getWidth() - 2*margin, bottom),
                10f, 10f, captionOverlayStyle);

        canvas.drawPath(path, captionBackgroundStyle);

        canvas.drawText(String.format("- %tF -", selectedBar.date.toDate()),
                canvas.getWidth() / 2.0f - 100, captionRow * 2 * captionFontSize + top, captionTextStyle);

        captionRow += 2;
        for (Transaction t : selectedBar.dayTransactions) {
            canvas.drawText(String.format(descriptionFormat, t.description),
                    2*margin, captionRow * 1.5f * captionFontSize + top,
                    captionTextStyle);
            String amount = String.format(amountFormat, t.amount);
            canvas.drawText(amount,
                    canvas.getWidth() - 2 * margin - captionAmountStyle.measureText(amount),
                    captionRow * 1.5f * captionFontSize + top,
                    captionAmountStyle);
            captionRow++;
        }

    }

}

public class TransactionsChart {

    private static final String TAG = "Payday.TransactionsChart";
    public float mxOffset = 0;
    public float scale = 1f;
    public float translateX = 0;
    public float translateY = 0;

    int barWidth = 50;
    int barMargin = 10;

    private Map<DateTime, List<Transaction>> transactionsPerDate;
    private BigDecimal maxTrans = BigDecimal.ZERO;
    private BigDecimal minTrans = BigDecimal.ZERO;


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
    private Bar lastSelected;
    private TransactionsGraphView view;
    private final float selectorY;
    private Bar selected;
    private RectF selectedDeviceRect;
    private RectF lastSelectedRect;
    private Bitmap bmp;
    private Canvas bmpCanvas;
    private final Bitmap.Config conf;
    private final Caption caption;

    public TransactionsChart(TransactionsGraphView view, List<Transaction> transactions) {
        this.view = view;
        this.transactions = transactions;

        selectorY = 100f;

        positiveBarStyle = new Paint();
        positiveBarStyle.setColor(Color.HSVToColor(new float[]{150f, 0.2f, 1f}));
        positiveBarBorderStyle = new Paint();
        positiveBarBorderStyle.setStrokeWidth(0.01f);
        positiveBarBorderStyle.setColor(Color.HSVToColor(new float[]{150f, 0.6f, 1f}));
        positiveBarSelectedStyle= new Paint();
        positiveBarSelectedStyle.setColor(Color.HSVToColor(new float[]{150f, 0.4f, 1f}));

        negativeBarStyle = new Paint();
        negativeBarStyle.setColor(Color.HSVToColor(new float[]{340f, 0.2f, 1f}));
        negativeBarBorderStyle = new Paint();
        negativeBarBorderStyle.setStrokeWidth(0.01f);
        negativeBarBorderStyle.setColor(Color.HSVToColor(new float[]{340f, 0.6f, 1f}));
        negativeBarSelectedStyle= new Paint();
        negativeBarSelectedStyle.setColor(Color.HSVToColor(new float[]{340f, 0.4f, 1f}));

        tickStyle = new Paint();
        tickStyle.setColor(Color.HSVToColor(new float[]{220f, 0.0f, .70f}));
        tickStyle.setAlpha(220);
        tickStyle.setStrokeWidth(2f);
        tickStyle.setTextSize(20f);
        tickStyle.setAntiAlias(true);

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

        //Log.i(TAG, String.format("%f, %f", maxTrans.doubleValue(), minTrans.doubleValue()));
        yScale = new Scale();

        conf = Bitmap.Config.ARGB_8888;

        //bmp = Bitmap.createBitmap(view.getWidth(), view.getHeight(), conf);
        //bmpCanvas = new Canvas(bmp);
        selected = null;
        selectedDeviceRect = null;
        caption = new Caption(this.view);
        makeBars();
        resize(view.getWidth(), view.getHeight());
        setScale(scale);

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
                        bar = new PositiveBar(
                                new RectF(
                                        x, positiveHeight - val,
                                        x + barWidth, positiveHeight),
                                positiveBarStyle,
                                positiveBarSelectedStyle,
                                positiveBarBorderStyle);
                        positiveHeight -= val;
                    } else {
                        bar = new NegativeBar(
                                new RectF(
                                        x,  negativeHeight,
                                        x + barWidth, negativeHeight - val),
                                negativeBarStyle,
                                negativeBarSelectedStyle,
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


    public void initialAnimation(){
        int i = 0;
        for (final Bar bar: bars){
            ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
            animation.setStartDelay(i);
            i+=10;
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
                translateY = (Float) valueAnimator.getAnimatedValue();
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
        caption.resize(w,h);
     }

    public void setScale(float scale){
        this.scale = scale;
        positiveBarBorderStyle.setStrokeWidth(5f/scale);
        negativeBarBorderStyle.setStrokeWidth(5f/scale);
    }

    public void drawScale(Canvas canvas, Matrix m){
        float h;
        float val;
        float lw = (canvas.getWidth()) / scale; //width in local coords
        float max = 2*lw*100f;
        int step = 10;
        if (max >= 1000)
            step = 100;
        if (max >= 5000)
            step = 500;
        if (max >= 10000)
            step = 1000;
        if (max >= 50000)
            step = 5000;
        if (max >= 100000)
            step = 10000;
        if (max >= 500000)
            step = 50000;
        //int step = (int) (Math.ceil(lw / 100f));
        Log.d(TAG, String.format("Scale: %s MaxG: %s, Max: %s Step: %s Scaled: %s", scale, canvas.getWidth(), max, step, yScale.apply(step) ));
        for(int i = 0; i<max; i+= step){
            val = (float)(yScale.apply(i) * scale);

            if (i % (5*step) == 0){
                h = 30;
               canvas.drawText(Integer.toString(i),
                       (canvas.getWidth() / 2.0f) +  val - (tickStyle.measureText(Integer.toString(i))/2f),
                       h + 25,
                       tickStyle);
               if (val!=0)
                    canvas.drawText("-" + Integer.toString(i),
                        (canvas.getWidth() / 2.0f) -  val - (tickStyle.measureText("-" + Integer.toString(i))/2f),
                        h + 25,
                        tickStyle);
            }else{
                h = 15;


            }
            //Log.d(TAG, "Scale " + i + " " + val);

            canvas.drawLine(((float) canvas.getWidth() / 2.0f) +  val, -h, ((float) canvas.getWidth() / 2.0f) +  val, h, tickStyle);
            canvas.drawLine(((float) canvas.getWidth() / 2.0f) + -val, -h, ((float) canvas.getWidth() / 2.0f) + -val, h, tickStyle);
        }
    }

    public void render(Canvas canvas) {
        Matrix m = new Matrix();
        m.preTranslate(canvas.getWidth() / 2f + translateX, translateY);
        m.preScale(scale, 1);
        m.preRotate(90f);

        float tickHeight = 30;

        canvas.save();
        canvas.concat(m);

        if (transactions.size() == 0) {
            return;
        }

        for (Bar bar:bars){
            RectF deviceRect = new RectF();
            m.mapRect(deviceRect, bar.rect);

            bar.selected  = deviceRect.contains(deviceRect.centerX(), selectorY);

            if (bar.selected){
                selected = bar;
                selectedDeviceRect = deviceRect;
            }
            bar.draw(canvas);
        }

        if (lastSelected != selected){
            Vibrator vibrator = (Vibrator) view.context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(20);
            //Log.i(TAG, "started animation" + m.mapRadius(selectorY) + " " + selectedDeviceRect.centerY() + " " + translateY);

            lastSelected = selected;
            lastSelectedRect = selectedDeviceRect;
        }

        if(lastSelected != null){
            /*Log.i(TAG, String.format("%s %s %s",
                    translateY, selectedDeviceRect.centerY(),
                    selectorY - selectedDeviceRect.centerY()));*/
        }

        canvas.restore();
        drawScale(canvas, m);

        Path p = new Path();
        p.moveTo(0, selectorY - tickHeight);
        p.lineTo(tickHeight, selectorY);
        p.lineTo(0, selectorY + tickHeight);
        canvas.drawPath(p, tickStyle);

        p.reset();
        p.moveTo(canvas.getWidth(), selectorY - tickHeight);
        p.lineTo(canvas.getWidth() - tickHeight, selectorY);
        p.lineTo(canvas.getWidth(), selectorY + tickHeight);
        canvas.drawPath(p, tickStyle);
        canvas.drawLine(0, selectorY, canvas.getWidth(), selectorY, tickStyle);

        if (selected != null) {
            caption.draw(canvas, bmp, selected, selectedDeviceRect);
        }
    }
}
