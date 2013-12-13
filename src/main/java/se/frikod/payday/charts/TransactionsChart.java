package se.frikod.payday.charts;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.renderscript.Allocation;
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



public class TransactionsChart {

    private static final String TAG = "Payday.TransactionsChart";
    public float mxOffset = 0;
    public float scale = 1f;
    public float translateX = 0;
    public float translateY = 0;

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

    private Paint captionBackgroundStyle;

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
    private final RenderScript rs;
    private ScriptIntrinsicBlur theIntrinsic;

    public TransactionsChart(TransactionsGraphView view, List<Transaction> transactions) {
        this.view = view;
        this.transactions = transactions;

        selectorY = 100f;

        positiveBarStyle = new Paint();
        positiveBarStyle.setColor(Color.HSVToColor(new float[]{150f, 0.2f, 1f}));
        positiveBarBorderStyle = new Paint();
        positiveBarBorderStyle.setStrokeWidth(5.0f);
        positiveBarBorderStyle.setColor(Color.HSVToColor(new float[]{150f, 0.6f, 1f}));
        positiveBarSelectedStyle= new Paint();
        positiveBarSelectedStyle.setColor(Color.HSVToColor(new float[]{150f, 0.4f, 1f}));

        negativeBarStyle = new Paint();
        negativeBarStyle.setColor(Color.HSVToColor(new float[]{340f, 0.2f, 1f}));
        negativeBarBorderStyle = new Paint();
        negativeBarBorderStyle.setStrokeWidth(5.0f);
        negativeBarBorderStyle.setColor(Color.HSVToColor(new float[]{340f, 0.6f, 1f}));
        negativeBarSelectedStyle= new Paint();
        negativeBarSelectedStyle.setColor(Color.HSVToColor(new float[]{340f, 0.4f, 1f}));

        tickStyle = new Paint();
        tickStyle.setColor(Color.HSVToColor(new float[]{220f, 0.0f, .90f}));
        tickStyle.setAlpha(220);
        tickStyle.setStrokeWidth(1f);

        captionStyle = new Paint();
        captionStyle.setColor(Color.BLACK);
        captionStyle.setTextSize(captionFontSize);
        captionStyle.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        captionStyle.setAntiAlias(true);

        captionBackgroundStyle = new Paint();
        //captionBackgroundStyle.setColor(Color.HSVToColor(new float[]{220f, 0.1f, .90f}));
        //captionBackgroundStyle.setAlpha(100);
        //captionBackgroundStyle.setStrokeWidth(1f);

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
        conf = Bitmap.Config.ARGB_8888;

        //bmp = Bitmap.createBitmap(view.getWidth(), view.getHeight(), conf);
        //bmpCanvas = new Canvas(bmp);
        selected = null;
        selectedDeviceRect = null;
        rs = RenderScript.create(this.view.getContext());
        theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
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

    public float findMaxFontSize(float width, Paint style, String str){
        int textSize = 14;

        while (style.measureText(str) < width){
            textSize += 2;
            style.setTextSize(textSize+2);
        }

        captionStyle.setTextSize(textSize);
        return style.measureText(str);
    }

    public void drawCaption(Canvas canvas, Bar selectedBar, RectF selectedRect){

        String format = "%-24.24s %10.2f";

        int captionRow = 1;
        float margin = 20f;
        float arrowWidth = 50f;
        float arrowHeight= 50f;
        float textWidth = findMaxFontSize(canvas.getWidth() - 4 * margin , captionStyle, String.format(format, "test", 10000f));
        float top, bottom;
        Path path = new Path();
        float boxHeight = captionStyle.getFontSpacing() * (4 +  selectedBar.dayTransactions.size());

        top = canvas.getHeight() / 2f;
        bottom = canvas.getHeight();
        /*if (selectedBar.direction == Direction.POSITIVE){
            top = selectedDeviceRect.bottom + arrowHeight;
            bottom = top + boxHeight;
            path.moveTo(selectedDeviceRect.centerX() - arrowWidth, top);
            path.lineTo(selectedDeviceRect.centerX(), top - arrowHeight);
            path.lineTo(selectedDeviceRect.centerX() + arrowWidth, top);

        }

       else{
            bottom = selectedDeviceRect.top - arrowHeight;
            top = bottom - boxHeight;
            path.moveTo(selectedDeviceRect.centerX() - arrowWidth, bottom);
            path.lineTo(selectedDeviceRect.centerX(), bottom + arrowHeight);
            path.lineTo(selectedDeviceRect.centerX() + arrowWidth, bottom);
        }*/

        Bitmap blurBG  = bmp.copy(conf, false);
        Allocation tmpIn = Allocation.createFromBitmap(rs, bmp);
        Allocation tmpOut = Allocation.createFromBitmap(rs, blurBG);
        theIntrinsic.setRadius(25f);
        theIntrinsic.setInput(tmpIn);
        theIntrinsic.forEach(tmpOut);

        tmpOut.copyTo(blurBG);



        //Bitmap blurBG = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 8, bmp.getHeight() / 8, true);
        //blurBG = Bitmap.createScaledBitmap(blurBG, bmp.getWidth(), bmp.getHeight(), true);
        Shader shader = new BitmapShader(blurBG, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);



        captionBackgroundStyle.setShader(shader);
        canvas.drawRoundRect(new RectF(
                margin, top,
                (2 * margin) + textWidth, bottom),
                10f, 10f, tickStyle);
        canvas.drawRoundRect(new RectF(
                margin, top,
                (2 * margin) + textWidth, bottom),
                10f, 10f,
                captionBackgroundStyle);


        canvas.drawPath(path, captionBackgroundStyle);
        canvas.drawText(String.format("%24tF", selectedBar.date.toDate()),
                2*margin, captionRow * 2 * captionFontSize + top, captionStyle);
        captionRow+=2;
        for (Transaction t : selectedBar.dayTransactions) {
            canvas.drawText(String.format(format, t.description, t.amount),
                    2*margin, captionRow * 2 * captionFontSize + top,
                    captionStyle);
            captionRow++;
        }

    }

    public void initialAnimation(){
        int i = 0;
        for (final Bar bar: bars){
            ValueAnimator animation = ValueAnimator.ofFloat(0f, 10f);
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

        bmp = Bitmap.createBitmap(w, h, conf);
        bmpCanvas = new Canvas(bmp);
    }

    public void render(Canvas ocanvas) {
        Canvas canvas = bmpCanvas;
        bmp.eraseColor(0);
        Matrix m = new Matrix();
        m.preTranslate(canvas.getWidth() / 2f + translateX, translateY);
        m.preScale(scale, scale);
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

            if (deviceRect.contains(deviceRect.centerX(), selectorY)){
                bar.selected = true;
            }else{
                bar.selected = false;
            }

            if (bar.selected){
                selected = bar;
                selectedDeviceRect = deviceRect;
            }
            bar.draw(canvas);
        }

        if (lastSelected != selected){
            Vibrator vibrator = (Vibrator) view.context.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(20);
            Log.i(TAG, "started animation" + m.mapRadius(selectorY) + " " + selectedDeviceRect.centerY() + " " + translateY);

            lastSelected = selected;
            lastSelectedRect = selectedDeviceRect;
        }

        if(lastSelected != null){
            Log.i(TAG, String.format("%s %s %s",
                    translateY, selectedDeviceRect.centerY(),
                    selectorY - selectedDeviceRect.centerY()));
        }

        canvas.restore();

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
            drawCaption(canvas, selected, selectedDeviceRect);
        }
        ocanvas.drawBitmap(bmp, 0,0, new Paint());

    }
}
