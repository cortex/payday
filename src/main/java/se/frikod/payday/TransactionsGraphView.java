package se.frikod.payday;

import com.nineoldandroids.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import se.frikod.payday.charts.TransactionsChart;

public class TransactionsGraphView extends View {


    private static final String TAG = "Payday.TransactionsGraphView";
    TransactionsChart mRenderer;
    private List<Transaction> transactions;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;


    public TransactionsGraphView(Context context) {
        super(context);
        transactions = new ArrayList<Transaction>();
    }

    public TransactionsGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        transactions = new ArrayList<Transaction>();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        this.mRenderer = new TransactionsChart(this, transactions);
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        mRenderer.render(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        mRenderer.mxOffset = event.getX();

        invalidate();

        this.getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, final float velocityX, final float velocityY) {
            //mRenderer.setXVelDev(velocityX / 100f);
            //startKineticts();
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //mRenderer.mxOffset = -distanceX;
            mRenderer.translateX -= distanceX;
            //stopKineticts();
            return true;

        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mRenderer.initialAnimation();
            invalidate();
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            mRenderer.scale *= (detector.getScaleFactor());
            /*
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
            mRenderer.mScale = mScaleFactor;
            */
            //mRenderer.scaleModel(detector.getScaleFactor());
            //startKineticts();
            return true;
        }
    }

}
