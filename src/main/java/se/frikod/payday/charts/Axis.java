package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import se.frikod.payday.SIFormat;

class Tick {
    float value;
    String label;
    float width;
    boolean isLong;
}

class Axis {
    public float zoom;
    public int width;
    public int height;

    private Scale yScale;
    private Paint longTickStyle;
    private Paint tickStyle;
    private Paint scaleBackground;

    private int longTickWidth = 30;
    private int shortTickWidth = 15;

    private float margin;
    private String TAG = "Payday.Axis";

    private List<Tick> ticks;

    Axis(float zoom, Scale yScale, float density) {

        this.zoom = zoom;
        this.yScale = yScale;
        this.margin = 50 * density;

        longTickStyle = new Paint();
        longTickStyle.setColor(Color.HSVToColor(new float[]{220f, .1f, 1f}));
        longTickStyle.setStrokeWidth(density * 1f);
        longTickStyle.setStyle(Paint.Style.STROKE);

        tickStyle = new Paint();
        tickStyle.setColor(Color.HSVToColor(new float[]{220f, 0.0f, .70f}));
        tickStyle.setStrokeWidth(density * 1f);
        tickStyle.setTextSize(density * 12f);
        tickStyle.setAntiAlias(true);
        tickStyle.setTextAlign(Paint.Align.RIGHT);

        scaleBackground = new Paint();
        scaleBackground.setColor(Color.HSVToColor(new float[]{230f, 0.01f, 1f}));
        scaleBackground.setAlpha(200);

        this.ticks = new LinkedList<Tick>();

    }

    public void resize(int width, int height){
        this.width = width;
        this.height = height;
        this.calcStep(this.height);
    }

    public void calcStep(float height) {
        //Calculate best step size to fit in height
        Log.i(TAG, "" + yScale);
        double scaleMax = yScale.unscale(height);
        Log.i(TAG, String.format("Height: %s, Scaled height: %s", height, scaleMax));
        double step = findBestScaleStep(scaleMax);
        Log.i(TAG, "Step: " + step);
        ticks.clear();
        for (int i = 0; i < scaleMax; i += step) {
            Tick tick = new Tick();
            tick.value = (float) yScale.apply(i);
            tick.width = shortTickWidth;
            if (i % (5 * step) == 0) {
                tick.width = longTickWidth;
                tick.label = SIFormat.humanReadable(i);
                tick.isLong = true;
            }
            ticks.add(tick);
        }
        Log.i(TAG, "Number of ticks: " + ticks.size());
    }


    public double findBestScaleStep(double height) {

        double[] multiples =  {10, 5.0, 1.0, 0.5, 0.1};
        int magnitude = (int) Math.pow(10.0f, Math.floor(Math.log10(height)));
        int TARGET_STEPS = 50;

        Double best_step = null;
        Double best_delta = null;

        for (double mult : multiples) {
            double step = mult * magnitude;
            double nSteps = (height / step);
            double delta = Math.abs(TARGET_STEPS - nSteps);
            if (best_delta == null || delta <= best_delta) {
                best_step = step;
                best_delta = delta;
            }

        }
        assert best_step  != null;
        Log.i("Payday", String.format("Height / step: %s", height / best_step));
        return best_step;
    }

    public void drawBackground(Canvas canvas) {

        height = canvas.getHeight();
        width = canvas.getWidth();

        float origin = 0;
        for (Tick tick : ticks) {
            if (tick.isLong) {
                canvas.drawLine(margin, origin + -tick.value, width, origin + -tick.value, longTickStyle);
                canvas.drawLine(margin, origin + tick.value, width, origin + tick.value, longTickStyle);
            }
        }
    }

    public void drawForeground(Canvas canvas) {

        float origin = 0;
        float textSize = tickStyle.getTextSize();

        canvas.drawRect(0, -height / 2, margin + longTickWidth, height, scaleBackground);

        for (Tick tick : ticks) {
            if (tick.isLong) {
                canvas.drawText(tick.label,
                        margin - 10,
                        origin - tick.value + 0.3f * textSize,
                        tickStyle);
                if (tick.value != 0)
                    canvas.drawText("-" + tick.label,
                            margin - 10,
                            origin + tick.value + 0.3f * textSize,
                            tickStyle);
            }

            canvas.drawLine(margin, origin + tick.value, margin + tick.width, origin + tick.value, tickStyle);
            canvas.drawLine(margin, origin + -tick.value, margin + tick.width, origin + -tick.value, tickStyle);

        }
    }
}
