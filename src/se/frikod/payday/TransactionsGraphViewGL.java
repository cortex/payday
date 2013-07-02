package se.frikod.payday;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class TransactionsGraphViewGL extends GLSurfaceView {
    GraphRenderer mRenderer;

    private float mPreviousX;
    private float mPreviousY;
    private float TOUCH_SCALE_FACTOR = 1f;

    private ScaleGestureDetector mScaleDetector;
    private GestureDetector mGestureDetector;

    private float mScaleFactor = 1.f;
    private long mPreviousT;
    private String TAG = "Payday.TransactionsGraphViewGL";

    private ScheduledExecutorService scheduleTaskExecutor;

    public TransactionsGraphViewGL(Context context) {
        super(context);
    }

    public TransactionsGraphViewGL(Context context, AttributeSet attrs) {
        super(context, attrs);

        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        mGestureDetector = new GestureDetector(context, new GestureListener());

        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setDebugFlags(DEBUG_CHECK_GL_ERROR | DEBUG_LOG_GL_CALLS);

        mRenderer = new GraphRenderer(context);
        setEGLContextClientVersion(2);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void setTransactions(List<Transaction> transactions) {
        final List<Transaction> tr = transactions;
        queueEvent(new Runnable() {
            // This method will be called on the rendering
            // thread:
            public void run() {
                mRenderer.setTransactions(tr);
            }
        });
    }


    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        mGestureDetector.onTouchEvent(e);
        mScaleDetector.onTouchEvent(e);

        if (e.getAction() == MotionEvent.ACTION_UP){
            startKineticts();
        }

        this.requestRender();

        this.getParent().requestDisallowInterceptTouchEvent(true);
        return true;
    }


    private void startKineticts(){
        if (mRenderer.kinetics != true){
            mRenderer.kinetics = true;
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
            scheduleTaskExecutor = Executors.newSingleThreadScheduledExecutor();
            scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "Checking render mode");
                    if (mRenderer.kinetics == false){
                        Log.i(TAG, "Shutting down kinetics");
                        stopKineticts();
                        scheduleTaskExecutor.shutdown();
                    }
                }
            }, 500, 500, TimeUnit.MILLISECONDS);
        }
    }

    private void stopKineticts(){
        mRenderer.kinetics = false;
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        requestRender();
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, final float velocityX, final float velocityY){
            mRenderer.mxVel = velocityX / 100000f;
            startKineticts();
            return true;
        }
        
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
            mRenderer.mxOffset += -distanceX;
            stopKineticts();
            return true;

        }

    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));
            mRenderer.mScale = mScaleFactor;
            startKineticts();
            return true;
        }
    }

}


