package se.frikod.payday;

import android.content.Context;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.os.Vibrator;
import android.util.Log;
import org.joda.time.DateTime;
import org.joda.time.Days;
import se.frikod.payday.graphics.Rectangle;
import se.frikod.payday.graphics.Text;
import se.frikod.payday.graphics.Util;

import javax.microedition.khronos.opengles.GL10;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "Payday.GraphRenderer";

    private static final float MIN_F = 0.000000000001f / 1000f;
    private static final float MAX_VEL = 1000000f / 1000f;
    private static final float MAX_X_MARGIN = 10f;

    Context context;

    List<Transaction> transactions;
    List<Rectangle> rectangles = new ArrayList<Rectangle>();
    List<Rectangle> ticks = new ArrayList<Rectangle>();
    List<Rectangle> bounds = new ArrayList<Rectangle>();

    int mSelected = -1;

    Text text;

    int barWidth = 100;
    int barMargin = 10;
    //float mxOffset = 0;
    float mxVelMod = 0;

    long mPreviousTime;

    float mScale = 1.0f;

    float mFriction = 30f / 1000f;
    float mKs = 5f / 1000f;

    boolean kinetics = true;

    float yBarScale = 0.5f;

    private int width = 100;
    private int height = 100;

    private final float[] mModelSpaceInv = new float[16];

    //Screenspace = 0,0 at upper left, MAXX, MAXY
    private final float[] mScreenSpace = new float[16];

    //Modelspace = 0,0 at lower left, 100,l at screen width
    private final float[] mModelSpace = new float[16];


    private final float [] mMVPMatrix = new float[16];


    private final float[] mCursorPosMod = {0, 0f, 0f, 1f};
    private final float[] mCursorPosDev = new float[4];

    private Text transactionDescription;
    private Text transactionDate;
    private Rectangle l;
    private Vibrator v;
    private Rectangle modelRect;

    public GraphRenderer(Context context) {
        super();
        this.context = context;
    }

    public static int loadShader(int type, String shaderCode) {

        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    public void initGraph() {
        DateTime startDate = transactions.get(0).date;
        DateTime endDate = transactions.get(0).date;
        v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        Map<DateTime, List<Transaction>> transactionsPerDate = new HashMap<DateTime, List<Transaction>>();

        int maxDailyTransactions = 1;

        for (Transaction t : transactions) {
            if (startDate.isAfter(t.date))
                startDate = t.date;
            if (endDate.isBefore(t.date))
                endDate = t.date;

            List<Transaction> ts = transactionsPerDate.get(t.date);
            if (ts == null) {
                ts = new ArrayList<Transaction>();
                transactionsPerDate.put(t.date, ts);
            }
            ts.add(t);
            if (ts.size() > maxDailyTransactions) maxDailyTransactions = ts.size();
        }

        Days days = Days.daysBetween(startDate, endDate);

        float x = 0;

        for (int i = 0; i <= days.getDays(); i++) {
            DateTime day = startDate.plus(Days.days(i));
            List<Transaction> dayTransactions = transactionsPerDate.get(day);

            Rectangle tick = new Rectangle(x, -5, 1, 10);
            tick.setColor(0.7f, 0.7f, 0.7f, 1.0f);
            ticks.add(tick);
            float xoff = 0;
            if (dayTransactions != null) {

                //float bw = barWidth / (maxDailyTransactions - 1.0f);
                float bw = barWidth; // (maxDailyTransactions - 1.0f);
                for (Transaction t : dayTransactions) {
                    Rectangle rectangle = new Rectangle(x + xoff, 0, bw, t.amount.toBigInteger().intValue() * yBarScale);
                    if (t.amount.intValue() < 0) {
                        rectangle.setColor(.8f, 0.5f, 0.5f, 1f);
                    }

                    if (t.amount.intValue() > 0) {
                        rectangle.setColor(0.5f, .8f, 0.5f, 1f);
                    }

                    xoff += bw + 10;
                    this.rectangles.add(rectangle);
                }
            }
            x += xoff;
        }
        Util.checkGlError();
        transactionDate = new Text("x", 0, 0);
        transactionDescription = new Text("x", 0, 0);

    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        float[] mProjMatrix = new float[16];
        float[] mVMatrix = new float[16];

        GLES20.glViewport(0, 0, width, height);
        Matrix.orthoM(mProjMatrix, 0, 0, width, 0, height, 3, 7);
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mScreenSpace, 0, mProjMatrix, 0, mVMatrix, 0);
        Matrix.translateM(mModelSpace, 0, mScreenSpace, 0, 0, height / 2, 0);

        this.width = width;
        this.height = height;
        updateMatrices();

        initGraph();

        this.mPreviousTime = SystemClock.uptimeMillis();
        l = new Rectangle(width / 2-50, 100, 100, height - 100);
        l.setColor(0.95f, 0.95f, 0.95f, .7f);

        float w = 100f;
        Rectangle r = new Rectangle(-w, -w, 10.0f, 10.0f);
        r.setColor(Color.RED);
        bounds.add(r);

        r = new Rectangle(-w, w, 10.0f, 10.0f);
        r.setColor(Color.BLUE);
        bounds.add(r);

        r = new Rectangle(0, w, 10.0f, 10.0f);
        r.setColor(Color.CYAN);
        bounds.add(r);

        r = new Rectangle(w, -w, 10.0f, 10.0f);
        r.setColor(Color.GREEN);
        bounds.add(r);

        r = new Rectangle(w, w, 10.0f, 10.0f);
        r.setColor(Color.YELLOW);
        bounds.add(r);

        r = new Rectangle(0, 0, 10.0f, 10.0f);
        r.setColor(Color.MAGENTA);
        bounds.add(r);

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_BLEND);
        Util.checkGlError();
        GLES20.glClearColor(1, 1, 1, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        updateMatrices();

    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;

    }

    public void updateMatrices(){
        //Matrix.multiplyMM(mScreenSpace, 0, mProjMatrix, 0, mVMatrix, 0);
        //Matrix.scaleM(mModelSpace, 0, mScreenSpace, 0, mScale, mScale, 1f);
        //Matrix.translateM(mModelSpace, 0, mModelSpace, 0, mxOffset, height / 2, 0);

        Matrix.invertM(mModelSpaceInv, 0, mModelSpace, 0);
        Matrix.multiplyMV(mCursorPosDev, 0, mModelSpaceInv, 0, mCursorPosMod, 0);

    }


    /*public float[] mod2scr(float[] modVec){
        float[] devVec = new float[4];
        Matrix.multiplyMV(devVec, 0, mModelSpace2, 0, modVec, 0);
        return devVec;
    }*/

    public float[] mod2dev(float[] modVec){
        float[] devVec = new float[4];
        Matrix.multiplyMV(devVec, 0, mModelSpace, 0, modVec, 0);
        return devVec;
    }

    public float[] dev2mod(float[] screenVec){
        float[] modVec = new float[4];
        Matrix.multiplyMV(modVec, 0, mModelSpaceInv, 0, screenVec, 0);
        return modVec;
    }

    public float[] x2abs4f(float x){
        float[] xv = {x,0,0,1};
        return xv;
    }

    public float[] x2dis4f(float x){
        float[] xv = {x,0,0,0};
        return xv;
    }

    public void translateModel(float xd, float yd, float zd){
        //float [] translateVectorDevice = {xd, yd, zd, 0};
        //float [] translateVectorModel = dev2mod(translateVectorDevice);
        //Log.i(TAG, String.format("Before: %2f After: %2f", xd, translateVectorModel[0]));
        //Matrix.translateM(mModelSpace, 0,
        //        translateVectorModel[0],
        //        translateVectorModel[1],
        //        translateVectorModel[2]);
        Matrix.translateM(mModelSpace, 0, xd,yd,zd);
        updateMatrices();
    }

    public void setXVelDev(float xVelDev){
        mxVelMod = dev2mod(x2dis4f(xVelDev))[0];
        Log.i(TAG, String.format("Setting velocity %s %s", xVelDev, mxVelMod));
    }

    public void scaleModel(float scaleFactorDeviceSpace){
        //float scaleFactorModelSpace = x2dis4f(scaleFactorDeviceSpace)[0];
        Matrix.scaleM(mModelSpace, 0, scaleFactorDeviceSpace, scaleFactorDeviceSpace, 1f);
        updateMatrices();

    }

    public void onDrawFrame(GL10 gl) {
        updateMatrices();
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        for (Rectangle rect: bounds){
            rect.draw(mScreenSpace);
            rect.draw(mModelSpace);
        }


        if (kinetics) {
            long dt = SystemClock.uptimeMillis() - mPreviousTime;

            float mindx = Float.MAX_VALUE;
            Rectangle closest = rectangles.get(0);
            /*for (Rectangle r : rectangles) {
                float  dx = Math.abs(mCursorPosDev[0] - (r.mx + r.mw / 2f));
                if (dx < mindx) {
                    mindx = dx;
                    closest = r;
                }
            }*/

            float F = 0.0f;

            float[] closestPosMod = x2abs4f((closest.mx + closest.mw/2f));
            //F += (mCursorPosMod[0] - closestPosMod[0]) * mKs;mCursorPosDev
            F += -(mFriction * mxVelMod);

            float OldVelMod = mxVelMod;
            mxVelMod = mxVelMod + F * dt;

            /*if (Math.abs(F) < MIN_F) {
                Log.i(TAG, "" + F);
                kinetics = false;
            }*/

            if (Math.abs(mxVelMod) > MAX_VEL) {
                mxVelMod = Math.signum(mxVelMod) * MAX_VEL;
            }

            float[] offsetDeltaDev = mod2dev(x2dis4f((OldVelMod + mxVelMod) * 0.5f * dt));
            float[] offsetDeltaDev2 = mod2dev(x2dis4f(0.0f));

            Text nt = new Text(String.format("Offset: %.2f Vel: %.2f F: %.2f O: %.2f",
                    mModelSpace[3], mxVelMod, F, offsetDeltaDev[0]), 0, height - 50);
            nt.draw(mScreenSpace);

            Matrix.translateM(mModelSpace, 0, offsetDeltaDev[0], 0, 0);
        }
        this.mPreviousTime = SystemClock.uptimeMillis();


        //RESTRICTIONS
//
//        if (Math.abs(mxOffset) > rectangles.get(rectangles.size()-1).mx + MAX_X_MARGIN) {
//            mxOffset = rectangles.get(rectangles.size()-1).mx + MAX_X_MARGIN;
//        }
//
//
//        if (Math.abs(mxOffset) > rectangles.get(0).mx - MAX_X_MARGIN) {
//            mxOffset = rectangles.get(0).mx - MAX_X_MARGIN;
//        }

         updateMatrices();


        for (Rectangle tick : ticks) {
            tick.draw(mModelSpace);
        }

        for (int i = 0; i < rectangles.size(); i++) {
            Rectangle r = rectangles.get(i);
            Transaction t = transactions.get(i);

            if (t.amount.signum() > 0){
                r.setColor(0xFF87DEAA);
            }else{
                r.setColor(0xFFFF80B2);
            }

            if (r.isInX(mCursorPosDev[0])) {

                if (i != mSelected){

                    v.vibrate(7);


                    transactionDescription = new Text(t.description, 0, 0);
                    transactionDate = new Text(t.date_string + "  |  SEK" + t.amount, 0, 50);
                }
                mSelected = i;
            }

            if(i == mSelected){
                r.setColor(0.8f, 0.8f, 1.0f, 1.0f);
            }

            r.draw(mModelSpace);
        }

        l.draw(mScreenSpace);

        transactionDate.draw(mScreenSpace);
        transactionDescription.draw(mScreenSpace);
    }
}
