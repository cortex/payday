package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * Created by joakim on 12/29/13.
 */
class Axis{
    public float zoom;
    private Scale yScale;
    private Paint longTickStyle;
    private Paint tickStyle;

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


    }

    public void draw(Canvas canvas, Matrix m){

            float h;
            float val;
            float lw = (canvas.getWidth()) / zoom;
            double max = lw/2.0f / yScale.apply(1);
            int step;

            double mx = Math.log10(max) -1;

            if(mx - Math.floor(mx) < 0.4)
                step = (int) (1 * (Math.pow(10, Math.floor(mx))));
            else
                step = (int) (5 * (Math.pow(10, Math.floor(mx))));
            Paint p = new Paint();
            p.setColor(Color.HSVToColor(new float[]{230f, 0.01f, 1f}));
            p.setAlpha(160);
            canvas.drawRect(0,0,canvas.getWidth(), 80, p);

            //Log.d(TAG, String.format("Max: %s Mx: %s Step: %s", max,  mx, step ));
            for(int i = 0; i<max; i+= step){
                val = (float)(yScale.apply(i) * zoom);
                //TODO: crashes if you zoom in too much
                if (i % (5*step) == 0){
                    h = 30;
                    canvas.drawText(Integer.toString(i),
                            (canvas.getWidth() / 2.0f) +  val - (tickStyle.measureText(Integer.toString(i))/2f),
                            25,
                            tickStyle);
                    if (val!=0)
                        canvas.drawText("-" + Integer.toString(i),
                                (canvas.getWidth() / 2.0f) -  val - (tickStyle.measureText("-" + Integer.toString(i))/2f),
                                25,
                                tickStyle);
                    Path path = new Path();
                    path.moveTo(((float) canvas.getWidth() / 2.0f) + -val, 30);
                    path.lineTo( ((float) canvas.getWidth() / 2.0f) + -val, canvas.getHeight());
                    path.moveTo(((float) canvas.getWidth() / 2.0f) + val, 30);
                    path.lineTo( ((float) canvas.getWidth() / 2.0f) + val, canvas.getHeight());

                    canvas.drawPath(path, longTickStyle);

                }else{
                    h = 15;
                }

                canvas.drawLine(((float) canvas.getWidth() / 2.0f) +  val, 30, ((float) canvas.getWidth() / 2.0f) +  val, 30 + h, tickStyle);
                canvas.drawLine(((float) canvas.getWidth() / 2.0f) + -val, 30, ((float) canvas.getWidth() / 2.0f) + -val, 30 + h, tickStyle);


            }
        }

    }
