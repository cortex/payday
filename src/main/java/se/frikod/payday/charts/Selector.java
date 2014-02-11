package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Matrix;

import java.util.List;

public class Selector {
    private static Paint selectorStyle;


    private int width;
    private int height;
    float tickWidth = 30;
    public RectF selectedDeviceRect = null;


    public float selectorX;

    static {
        selectorStyle = new Paint();
        //selectorStyle.setColor(Color.HSVToColor(new float[]{220f, 0.0f, .70f}));
        selectorStyle.setColor(Color.LTGRAY);
        selectorStyle.setAlpha(100);
        selectorStyle.setStrokeWidth(2f);
        selectorStyle.setTextSize(30f);
        selectorStyle.setAntiAlias(true);
    }

    Bar getSelected(List<Bar> bars, Matrix graphCoords){
        Bar selected = null;
        for (Bar bar:bars){
            RectF deviceRect = new RectF();
            graphCoords.mapRect(deviceRect, bar.rect);

            bar.selected  = deviceRect.contains(selectorX, deviceRect.centerY());

            if (bar.selected){
                selected = bar;
                selectedDeviceRect = deviceRect;
            }
        }
        return selected;
    }

    public void resize(int w, int h){
        this.width = w;
        this.height = h;
        this.selectorX = (float) (w / 2.0);
    }

    public void draw(Canvas canvas){

        Path p = new Path();
        p.moveTo(selectorX - tickWidth, 0);
        p.lineTo(selectorX, tickWidth);
        p.lineTo(selectorX + tickWidth, 0);
        canvas.drawPath(p, selectorStyle);

        p.reset();
        p.moveTo(selectorX - tickWidth, height);
        p.lineTo(selectorX, height - tickWidth);
        p.lineTo(selectorX + tickWidth, height);
        canvas.drawPath(p, selectorStyle);

        canvas.drawLine(selectorX, 0 + tickWidth, selectorX, height - tickWidth, selectorStyle);

    }
}
