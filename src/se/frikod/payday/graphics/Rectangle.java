package se.frikod.payday.graphics;

import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Rectangle {
    private static String TAG ="Payday.graphics.Rectangle";
    private final String vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "uniform mat4 mMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * mMatrix * vPosition;" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}";

    private final FloatBuffer vertexBuffer;
    private final ShortBuffer drawListBuffer;

    private Program program;

    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mMatrixHandle;

    public float mx;
    public float mw;

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    private final short drawOrder[] = {0, 1, 2, 0, 2, 3};
    private final int vertexStride = COORDS_PER_VERTEX * 4;

    float color[] = {0.2f, 0.709803922f, 0.898039216f, 1.0f};

    public final float[] mMatrix = new float[16];

    public boolean isInX(float x){
        return ((x >= mx) && (x  <= (mx+mw)));
    }

    public Rectangle(float x, float y, float w, float h) {
        mx = x;
        mw = w;

        float squareCoords[] = {
                x, y + h, 0.0f,     // top left
                x, y, 0.0f,         // bottom left
                x + w, y, 0.0f,     // bottom right
                x + w, y + h, 0.0f  // top right
        };

        Matrix.setIdentityM(mMatrix, 0);

        ByteBuffer bb = ByteBuffer.allocateDirect(
            // (# of coordinate values * 4 bytes per float)
            squareCoords.length * 4);

        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        program = new Program(vertexShaderCode, fragmentShaderCode);
    }

    public void setColor(float r,float g,float b,float a){
        color[0] = r;
        color[1] = g;
        color[2] = b;
        color[3] = a;
    }


    public void setColor(int r, int g, int b, int a){
        setColor(
                (float)(r/255.0),
                (float)(g/255.0),
                (float)(b/255.0),
                (float)(a/255.0)
        );
    }

    public void setColor(int color){

        this.setColor(
                Color.red(color),
                Color.green(color),
                Color.blue(color),
                Color.alpha(color));
    }

    public void draw(float[] mvpMatrix) {
        program.use();
        Util.checkGlError();

        mPositionHandle = GLES20.glGetAttribLocation(program.handle, "vPosition");
        Util.checkGlError();

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        Util.checkGlError();

        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);
        Util.checkGlError();

        mColorHandle = GLES20.glGetUniformLocation(program.handle, "vColor");
        Util.checkGlError();

        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
        Util.checkGlError();

        mMVPMatrixHandle = GLES20.glGetUniformLocation(program.handle, "uMVPMatrix");
        Util.checkGlError();

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        Util.checkGlError();

        mMatrixHandle = GLES20.glGetUniformLocation(program.handle, "mMatrix");
        Util.checkGlError();

        GLES20.glUniformMatrix4fv(mMatrixHandle, 1, false, mMatrix, 0);
        Util.checkGlError();


        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        Util.checkGlError();

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        Util.checkGlError();

    }

}
