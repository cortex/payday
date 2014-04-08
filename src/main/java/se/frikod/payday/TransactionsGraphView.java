package se.frikod.payday;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.List;

import se.frikod.payday.charts.TransactionsChart;

public class TransactionsGraphView extends View {

    private static final String TAG = "Payday.TransactionsGraphView";
    TransactionsChart mRenderer;
    private List<Transaction> transactions;
    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;
    public Context context;



    public TransactionsGraphView(Context context) {
        super(context);
        init(context);
    }



    public TransactionsGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public void init(Context context) {
        this.context = context;
        this.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());
        transactions = new ArrayList<Transaction>();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
        this.mRenderer = new TransactionsChart(this, transactions);
        //mRenderer.initialAnimation();
        //mRenderer.scaleToFit(false);
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        mRenderer.render(canvas);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh){
        mRenderer.resize(w, h);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_UP){
            mRenderer.snap();
        }

        invalidate();
        this.getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2,
                               final float velocityX, final float velocityY) {
            mRenderer.flingAnimation(velocityX);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            //mRenderer.setTranslateY(mRenderer.getTranslateY() - distanceY);
            mRenderer.setTranslate(mRenderer.getTranslateX() - distanceX, mRenderer.getTranslateY());
            mRenderer.setManualZoom(mRenderer.getZoom() *  (1 + 0.005f * distanceY));
            //scrolling = true;
            //return false;
            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent event){
            mRenderer.mxOffset = event.getX();

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            //mRenderer.toggleZoom();
            mRenderer.chartTypeAnimation();
            invalidate();
            return true;
        }
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            mRenderer.setManualZoom(mRenderer.getZoom() * (detector.getScaleFactor()));
            return true;
        }
    }
}