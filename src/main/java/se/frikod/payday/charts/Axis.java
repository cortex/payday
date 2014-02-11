package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import se.frikod.payday.SIFormat;

/**
 * Created by joakim on 12/29/13.
 */
class Axis{
    public float zoom;
    public int width;
    public int height;

    private Scale yScale;
    private Paint longTickStyle;
    private Paint tickStyle;

    private int longTickWidth = 30;
    private int shortTickWidth = 15;

    Axis(float zoom, Scale yScale){
        this.zoom = zoom;
        this.yScale = yScale;

        longTickStyle = new Paint();
        longTickStyle.setColor(Color.HSVToColor(new float[]{220f, 0.0f, .70f}));
        longTickStyle.setAlpha(220);
        longTickStyle.setPathEffect(new DashPathEffect(new float[]{2, 8}, 0));
        longTickStyle.setStrokeWidth(2f);
        longTickStyle.setStyle(Paint.Style.STROKE);

        tickStyle = new Paint();
        tickStyle.setColor(Color.HSVToColor(new float[]{220f, 0.0f, .70f}));
        tickStyle.setAlpha(220);
        tickStyle.setStrokeWidth(2f);
        tickStyle.setTextSize(30f);
        tickStyle.setAntiAlias(true);
        tickStyle.setTextAlign(Paint.Align.RIGHT);

    }

    /*private void calcStep(float height, int maxSteps){

            step = floor(height/maxSteps);
    }*/

    public void draw(Canvas canvas){

            float tickWidth;
            float val;
            float lw = height / zoom;
            double max = lw / 2.0f / yScale.apply(1);
            int step;
            int margin = 100;
            double mx = Math.log10(max) -1;

            if(mx - Math.floor(mx) < 0.8)
                step = (int) (1 * (Math.pow(10, Math.floor(mx))));
            else
                step = (int) (5 * (Math.pow(10, Math.floor(mx))));
            Paint p = new Paint();
            p.setColor(Color.HSVToColor(new float[]{230f, 0.01f, 1f}));
            p.setAlpha(200);
            canvas.drawRect(0, 0, margin + longTickWidth, height, p);

            //Log.d(TAG, String.format("Max: %s Mx: %s Step: %s", max,  mx, step ));
            for(int i = 0; i<max; i+= step){
                val = (float)(yScale.apply(i) * zoom);
                float origin = 0; //((float) height / 2.0f);

                //TODO: crashes if you zoom in too much
                if (i % (5 * step) == 0){
                    tickWidth = longTickWidth;

                    String number = SIFormat.humanReadable(i);

                    canvas.drawText(number,
                            margin - 10,
                            origin - val + 0.3f * tickStyle.getTextSize(),
                            tickStyle);
                    if (val != 0)
                        canvas.drawText("-" + number,
                                margin - 10,
                                origin + val + 0.3f * tickStyle.getTextSize(),
                                tickStyle);
                    Path path = new Path();


                    path.moveTo(margin, origin + -val);
                    path.lineTo(width, origin + -val);
                    path.moveTo(margin, origin +  val);
                    path.lineTo(width, origin +  val);

                    canvas.drawPath(path, longTickStyle);

                }else{
                    tickWidth = shortTickWidth;
                }

                //canvas.drawLine(((float) width / 2.0f) +  val, 30, ((float) width / 2.0f) +  val, 30 + h, tickStyle);
                //canvas.drawLine(((float) width / 2.0f) + -val, 30, ((float) width / 2.0f) + -val, 30 + h, tickStyle);

                canvas.drawLine(margin, origin +  val, margin + tickWidth, origin +  val, tickStyle);
                canvas.drawLine(margin, origin + -val, margin + tickWidth, origin + -val, tickStyle);

            }
        }

    }
