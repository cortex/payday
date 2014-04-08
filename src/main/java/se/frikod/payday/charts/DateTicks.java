package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Log;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ValueAnimator;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;

class DateTick{
    public String label;
    public String monthLabel;
    public LocalDate date;
    public EnumMap<ChartType, RectF> coords;
    public RectF currentRect;

    DateTick(LocalDate date){
        this.date = date;
        DateTimeFormatter format = DateTimeFormat.forPattern("dd");
        DateTimeFormatter monthFormat = DateTimeFormat.forPattern("YYYY-MM");
        this.monthLabel = monthFormat.print(date);
        this.label = format.print(date);
        this.coords = new EnumMap<ChartType, RectF>(ChartType.class);
    }
}

public class DateTicks {
    public final List<DateTick> ticks;
    public final Paint tickStyle;
    public final Paint tickStyleBold;
    public final Paint tickStyleOdd;
    public final Paint tickStyleEven;
    private static final float fontSize = 12.0f;
    private float density;

    DateTicks(int length, float density){
        int realFontSize = (int) (fontSize * density + 0.5f);
        this.density = density;

        tickStyle = new Paint();
        tickStyle.setColor(Color.HSVToColor(new float[]{340f, 0.0f, 0.5f}));
        tickStyle.setStrokeWidth(1.0f);
        tickStyle.setTextSize(realFontSize);
        tickStyle.setTextAlign(Paint.Align.LEFT);
        tickStyle.setAntiAlias(true);

        tickStyleBold = new Paint();
        tickStyleBold.setColor(Color.HSVToColor(new float[]{340f, 0.0f, 0.5f}));
        tickStyleBold.setStrokeWidth(1.0f);
        tickStyleBold.setTextSize(realFontSize);
        tickStyleBold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tickStyleBold.setTextAlign(Paint.Align.LEFT);
        tickStyleBold.setAntiAlias(true);

        tickStyleEven = new Paint();
        tickStyleEven.setColor(Color.HSVToColor(new float[]{220f, 1f, 0.2f}));
        tickStyleEven.setAlpha(0);

        tickStyleOdd = new Paint();
        tickStyleOdd.setColor(Color.HSVToColor(new float[]{220f, 1f, 0.8f}));
        tickStyleOdd.setAlpha(10);
        ticks = new ArrayList<DateTick>(length);
    }
    public Animator animateTo(ChartType chartType){
        List<Animator> animators = new LinkedList<Animator>();
        for (final DateTick dt: ticks){
            ValueAnimator lanim = ValueAnimator.ofFloat(dt.currentRect.left, dt.coords.get(chartType).left);
            ValueAnimator ranim = ValueAnimator.ofFloat(dt.currentRect.right, dt.coords.get(chartType).right);

            lanim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(com.nineoldandroids.animation.ValueAnimator valueAnimator) {
                    dt.currentRect.left = ((Float) valueAnimator.getAnimatedValue());
                }
            });
            ranim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(com.nineoldandroids.animation.ValueAnimator valueAnimator) {
                    dt.currentRect.right = ((Float) valueAnimator.getAnimatedValue());
                }
            });
            animators.add(lanim);
            animators.add(ranim);
        }

        AnimatorSet all = new AnimatorSet();
        all.playTogether(animators);
        return all;

    }

    void draw(Canvas canvas, ChartType chartType){
        int lastMonth = 0;
        boolean odd = true;
        float ct = canvas.getHeight()/2.0f;
        float cb = -canvas.getHeight()/2.0f;
        for(DateTick dt: ticks){

            RectF c = dt.currentRect;
            if (c.width() != 0){
                canvas.drawRect(c.left, cb, c.right, ct, odd ? tickStyleOdd : tickStyleEven);
                //Log.i("Payday", c.toString());

                canvas.save();

                canvas.translate(c.left, ct );
                //canvas.rotate(90);
                if (dt.date.getMonthOfYear() != lastMonth){
                    canvas.drawText(dt.monthLabel, 0, -density * fontSize, tickStyleBold);
                    lastMonth = dt.date.getMonthOfYear();
                }
                canvas.drawText(dt.label, 0, 0, tickStyle);
                canvas.restore();
                odd = !odd;
            }
        }
    }
}
