package se.frikod.payday;

import android.content.Context;
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

    private static final float MIN_F = 0.0000000001f / 1000f;
    private static final float MAX_VEL = 100f / 1000f;

    private static final float MAX_X_MARGIN = 10f;


    Context context;

    List<Transaction> transactions;
    List<Rectangle> rectangles = new ArrayList<Rectangle>();
    List<Rectangle> ticks = new ArrayList<Rectangle>();

    int mSelected = -1;

    Text text;

    int barWidth = 100;
    int barMargin = 10;
    float mxOffset = 0;
    double mxVel = 0;

    long mPreviousTime;

    float mScale = 1.0f;

    float mFriction = 0.03f / 1000f;
    float mKs = 0.0000005f / 1000f;

    boolean kinetics = false;

    float yBarScale = 0.5f;

    private int width = 100;
    private int height = 100;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mMVPInvMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];

    private final float[] mScreenSpace = new float[16];
    private final float[] mModelSpace= new float[16];

    private final float[] mCursorPos = {0, 0f, 0f, 1f};
    private final float[] mCursorPosTrans = new float[4];
    private Text transactionDescription;
    private Text transactionDate;
    private Rectangle l;
    private Vibrator v;

    public GraphRenderer(Context context) {
        super();
        this.context = context;
    }

    public static int loadShader(int type, String shaderCode) {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
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
        text = new Text("Hello World!", 0, 0);

    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {

        GLES20.glViewport(0, 0, width, height);
        Matrix.orthoM(mProjMatrix, 0, 0, width, 0, height, 3, 7);
        Matrix.setLookAtM(mVMatrix, 0, 0, 0, 3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.multiplyMM(mScreenSpace, 0, mProjMatrix, 0, mVMatrix, 0);

        initGraph();

        this.width = width;
        this.height = height;

        this.mPreviousTime = SystemClock.uptimeMillis();
        l = new Rectangle(width / 2-50, 100, 100, height - 100);
        l.setColor(0.95f, 0.95f, 0.95f, .7f);

    }

    @Override
    public void onSurfaceCreated(GL10 gl10, javax.microedition.khronos.egl.EGLConfig eglConfig) {
        GLES20.glEnable(GLES20.GL_BLEND);
        Util.checkGlError();
        GLES20.glClearColor(1, 1, 1, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        //GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);


    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;

    }

    public void onDrawFrame(GL10 gl) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (kinetics) {
            long dt = SystemClock.uptimeMillis() - mPreviousTime;

            double mindx = Float.MAX_VALUE;
            Rectangle closest = rectangles.get(0);
            for (Rectangle r : rectangles) {
                double  dx = Math.abs(mCursorPosTrans[0] - (r.mx + r.mw / 2f));
                if (dx < mindx) {
                    mindx = dx;
                    closest = r;
                }
            }

            double F = 0.0f;
            F += -((closest.mx + closest.mw/2f) - mCursorPosTrans[0]) * mKs;
            F += -(mFriction * mxVel);

            double OldVel = mxVel;
            mxVel = mxVel + (F) * dt;


            if (Math.abs(F) < MIN_F) {
                Log.i(TAG, "" + F);
                kinetics = false;
            }

            if (Math.abs(mxVel) > MAX_VEL) {
                mxVel = Math.signum(mxVel) * MAX_VEL;
            }
            mxOffset += (OldVel + mxVel) * 0.5f * dt;
            //Text nt = new Text(String.format("Offset: %s Vel: %s F: %s", mxOffset, mxVel, F), 0, height - 50);
            //nt.draw(mScreenSpace);


        }

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


        Matrix.translateM(mModelSpace, 0, mScreenSpace, 0, mxOffset, height / 2, 0);
        Matrix.scaleM(mModelSpace, 0, mScale, mScale, 1f);

        Matrix.invertM(mMVPInvMatrix, 0, mModelSpace, 0);
        Matrix.multiplyMV(mCursorPosTrans, 0, mMVPInvMatrix, 0, mCursorPos, 0);

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

            if (r.isInX(mCursorPosTrans[0])) {

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
