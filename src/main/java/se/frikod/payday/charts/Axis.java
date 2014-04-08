package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

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

    public void calcStep(float height) {
        //Calculate best step size to fit in height
        float lw = height / zoom;
        double max = lw / 2.0f / yScale.apply(1);
        int step = findBestScaleStep(height / 2 / zoom * 10);
        ticks.clear();
        for (int i = 0; i < max; i += step) {
            Tick tick = new Tick();
            tick.value = (float) (yScale.apply(i) * zoom);
            tick.width = shortTickWidth;
            if (i % (5 * step) == 0) {
                tick.width = longTickWidth;
                tick.label = SIFormat.humanReadable(i);
                tick.isLong = true;
            }
            ticks.add(tick);
        }

    }


    public int findBestScaleStep(double height) {
        double[] mults = {5.0, 2.0, 1.0, 0.5, 0.2, 0.1};
        int m = (int) Math.pow(10.0f, Math.round(Math.log10(height) - 1.0f));
        int MAX_STEPS = 50;
        int step = m;
        for (double mult : mults) {
            int nSteps = (int) (height / (mult * m) + 1);
            if (nSteps <= MAX_STEPS) {
                step = (int) (mult * m);
            }
        }
        return step;
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
