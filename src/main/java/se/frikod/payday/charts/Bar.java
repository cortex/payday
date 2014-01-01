package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import org.joda.time.DateTime;

import java.util.List;

import se.frikod.payday.Transaction;

enum Direction {POSITIVE, NEGATIVE}

class Bar{
    public Direction direction;

    public RectF targetRect;
    public RectF rect;
    public boolean selected;
    public List<Transaction> dayTransactions;

    static Paint negFill;
    static Paint negFillSelected;
    static Paint negBorder;

    static Paint posFill;
    static Paint posFillSelected;
    static Paint posBorder;

    static {
        negFill = new Paint();
        negFill.setColor(Color.HSVToColor(new float[]{340f, 0.2f, 1f}));
        negBorder = new Paint();
        negBorder.setStrokeWidth(0.01f);
        negBorder.setColor(Color.HSVToColor(new float[]{340f, 0.6f, 1f}));
        negFillSelected = new Paint();
        negFillSelected.setColor(Color.HSVToColor(new float[]{340f, 0.4f, 1f}));

        posFill = new Paint();
        posFill.setColor(Color.HSVToColor(new float[]{150f, 0.2f, 1f}));
        posBorder = new Paint();
        posBorder.setStrokeWidth(0.01f);
        posBorder.setColor(Color.HSVToColor(new float[]{150f, 0.6f, 1f}));
        posFillSelected = new Paint();
        posFillSelected.setColor(Color.HSVToColor(new float[]{150f, 0.4f, 1f}));

    }

    DateTime date;

    Bar(RectF rect, Direction direction){
        this.targetRect = rect;
        this.rect = new RectF(targetRect);
        this.direction = direction;
    }

    public static void setBorderWidth(float w){
        negBorder.setStrokeWidth(w);
        posBorder.setStrokeWidth(w);
    }

    public void scaleHeight(float value){
        if (direction == Direction.POSITIVE)
            rect.top = targetRect.bottom + (targetRect.top-targetRect.bottom) * value;
         if (direction == Direction.NEGATIVE)
            rect.bottom = targetRect.top + (targetRect.bottom-targetRect.top) * value;
    }

    public void draw(Canvas canvas){
        if (direction == Direction.POSITIVE){
            if (selected)
                canvas.drawRect(rect, posFillSelected);
            else
                canvas.drawRect(rect, posFill);
            canvas.drawLine(rect.left, rect.top, rect.right, rect.top, posBorder);
        }
        if (direction == Direction.NEGATIVE){
            if (selected)
                canvas.drawRect(rect, negFillSelected);
            else
                canvas.drawRect(rect, negFill);

            canvas.drawLine(rect.left, rect.bottom , rect.right, rect.bottom, negBorder);
        }
    }
}
