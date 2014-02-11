package se.frikod.payday.charts;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.View;

import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ValueAnimator;

import org.joda.time.DateTime;

import java.util.List;

import se.frikod.payday.Transaction;

enum Direction {POSITIVE, NEGATIVE}

class Bar{
    public final PointF groupedPos;
    public final PointF stackedPos;

    public final PointF groupedDatePos;
    public final PointF stackedDatePos;

    public RectF rect;

    public boolean selected;
    public List<Transaction> dayTransactions;
    public Transaction transaction;
    static Paint negFill;
    static Paint negFillSelected;
    static Paint negBorder;

    static Paint posFill;
    static Paint posFillSelected;
    static Paint posBorder;

    static int width = 50;
    static int margin = 5;

    public Direction direction;

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

    Bar(float stackedX, float groupedX, float stackedDateX, float groupedDateX,  float val, float positiveHeight, float negativeHeight){
        if (val > 0) {
            this.rect = new RectF(0, 0, width, val);
            this.stackedPos = new PointF(stackedX, positiveHeight - val );
            this.stackedDatePos = new PointF(stackedDateX, positiveHeight - val);
            this.groupedPos = new PointF(groupedX, -val);
            this.groupedDatePos = new PointF(groupedDateX, -val);
            direction = Direction.POSITIVE;
        } else {
            this.rect = new RectF(0, 0, width, -val);
            this.stackedPos = new PointF(stackedX, negativeHeight);
            this.stackedDatePos = new PointF(stackedDateX, negativeHeight);
            this.groupedDatePos = new PointF(groupedDateX, 0);
            this.groupedPos = new PointF(groupedX, 0);

            direction = Direction.NEGATIVE;
        }
        this.rect.offsetTo(stackedPos.x, stackedPos.y);
    }

    public static void setBorderWidth(float w){
        negBorder.setStrokeWidth(w);
        posBorder.setStrokeWidth(w);
    }

    public Animator animateTo(final View view, final PointF endPos){
        final PointF startPos = new PointF(rect.left, rect.top);
        ValueAnimator xanim = ValueAnimator.ofFloat(startPos.x, endPos.x);
        ValueAnimator yanim = ValueAnimator.ofFloat(startPos.y, endPos.y);
        AnimatorSet animation = new AnimatorSet();

        xanim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                rect.offsetTo((Float) valueAnimator.getAnimatedValue(), startPos.y);
                view.invalidate();
            }
        });

        yanim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                rect.offsetTo(endPos.x, (Float) valueAnimator.getAnimatedValue());
                view.invalidate();
            }
        });
        animation.playSequentially(xanim, yanim);
        return animation;
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
