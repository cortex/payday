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
import org.joda.time.LocalDate;

import java.util.EnumMap;
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

    LocalDate date;

    Bar( EnumMap<ChartType, Float> posX, float width, float height, float positiveHeight, float negativeHeight){
        if (height > 0) {
            this.rect = new RectF(0, 0, width, height);
            this.stackedPos = new PointF(posX.get(ChartType.STACKED), positiveHeight - height );
            this.stackedDatePos = new PointF(posX.get(ChartType.STACKED_DATE), positiveHeight - height);
            this.groupedPos = new PointF(posX.get(ChartType.GROUPED), -height);
            this.groupedDatePos = new PointF(posX.get(ChartType.GROUPED_DATE), -height);
            direction = Direction.POSITIVE;
        } else {
            this.rect = new RectF(0, 0, width, -height);
            this.stackedPos = new PointF(posX.get(ChartType.STACKED), negativeHeight);
            this.stackedDatePos = new PointF(posX.get(ChartType.STACKED_DATE), negativeHeight);
            this.groupedPos = new PointF(posX.get(ChartType.GROUPED), 0);
            this.groupedDatePos = new PointF(posX.get(ChartType.GROUPED_DATE), 0);

            direction = Direction.NEGATIVE;
        }
        this.rect.offsetTo(stackedPos.x, stackedPos.y);
    }

    public static void setBorderWidth(float w){
        negBorder.setStrokeWidth(w);
        posBorder.setStrokeWidth(w);
    }

    public Animator animateTo(final View view, final PointF endPos, boolean direction){
        final PointF startPos = new PointF(rect.left, rect.top);

        ValueAnimator xanim = ValueAnimator.ofFloat(startPos.x, endPos.x);
        ValueAnimator yanim = ValueAnimator.ofFloat(startPos.y, endPos.y);

        AnimatorSet animation = new AnimatorSet();

        xanim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                rect.offsetTo((Float) valueAnimator.getAnimatedValue(), rect.top);
                view.invalidate();
            }
        });

        yanim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                rect.offsetTo(rect.left, (Float) valueAnimator.getAnimatedValue());
                view.invalidate();
            }
        });
        if (direction) {
            animation.playSequentially(xanim, yanim);
        }else{
            animation.playSequentially(yanim, xanim);
        }
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
