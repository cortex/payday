package se.frikod.payday.graphics;

import android.graphics.*;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Text {
    private static final String TAG = "Payday.graphics.text";
    private final String vertexShaderCode =
            "uniform mat4 u_MVPMatrix;" +

            "attribute vec4 a_Position;" +
            "uniform vec4 u_Color;" +
            "attribute vec2 a_TexCoordinate;" +

            "varying vec2 v_TexCoordinate;" +
            "varying vec4 v_Color;" +

            "void main() {" +
            "  v_Color = u_Color;" +
            "  v_TexCoordinate = a_TexCoordinate;" +
            "  gl_Position = u_MVPMatrix * a_Position;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform sampler2D u_Texture;" +

            "varying vec4 v_Color;" +
            "varying vec2 v_TexCoordinate;" +

            "void main() {" +
            "  gl_FragColor = texture2D(u_Texture, v_TexCoordinate);" +
            "}";

    Program program;

    private int mColorHandle;						   // Shader color handle
    private int mTextureUniformHandle;                 // Shader texture handle
    private int mPositionHandle;
    private int mTextureCoordinateHandle;


    private int mMVPMatrixHandle;

    private final FloatBuffer vertexBuffer;
    private final FloatBuffer texBuffer;
    private final ShortBuffer drawListBuffer;

    static final int COORDS_PER_VERTEX = 3;
    private final short drawOrder[] = {0, 1, 2, 0, 2, 3};

    private final int vertexStride = COORDS_PER_VERTEX * 4;

    final int[] textureHandle = new int[1];
    public final float[] mMatrix = new float[16];

    float color[] = {0.2f, 0.709803922f, 0.898039216f, 1.0f};

    public Text(String text, int x, int y){
        program = new Program(vertexShaderCode, fragmentShaderCode);

        Paint textPaint = new Paint();
        textPaint.setTextSize(32);
        textPaint.setAntiAlias(true);
        //textPaint.setColor( 0xffffffff );
        textPaint.setColor( 0xff000000 );

        Rect bounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), bounds);
        bounds.set(0,0,Util.nextPowerOf2(bounds.width()), Util.nextPowerOf2(bounds.height()));


        Bitmap bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);  // Create Bitmap
        Canvas canvas = new Canvas( bitmap );           // Create Canvas for Rendering to Bitmap
        bitmap.eraseColor( 0x00000000 );                // Set Transparent Background (ARGB)
        canvas.drawText(text, 0,bounds.height(), textPaint);

        GLES20.glGenTextures(1, textureHandle, 0);
        Util.checkGlError();
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE );  // Set U Wrapping
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE );  // Set V Wrapping

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        Util.checkGlError();

        bitmap.recycle();

//        float squareCoords[] = {
//                x, y + bounds.height(), 0.0f,     // top left
//                x, y, 0.0f,         // bottom left
//                x + bounds.width(), y, 0.0f,     // bottom right
//                x + bounds.width(), y + bounds.height(), 0.0f  // top right
//        };


        float bh = bounds.height();
        float bw = bh *(bounds.width() / bounds.height());


        float squareCoords[] = {
                x, y + bh, 0.0f,
                x, y, 0.0f,
                x + bw, y, 0.0f,
                x + bw, y + bh, 0.0f
        };


        float texCoords[] = {
            0.0f, 0.0f,
            0.0f, 1,
            1.0f, 1.0f,
            1.0f, 0.0f,
        };

        Matrix.setIdentityM(mMatrix, 0);
        //Matrix.scaleM(mMatrix, 0, 0.25f,0.25f,0.25f);

        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);

        ByteBuffer tb = ByteBuffer.allocateDirect(texCoords.length * 4);
        tb.order(ByteOrder.nativeOrder());
        texBuffer = tb.asFloatBuffer();
        texBuffer.put(texCoords);
        texBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);


    }

    public void draw(float[] mvpMatrix) {
        program.use();
        Util.checkGlError();

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);  // Set the active texture unit to texture unit 0
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]); // Bind the texture to this unit

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0
        mTextureUniformHandle = GLES20.glGetUniformLocation(program.handle, "u_Texture");
        GLES20.glUniform1i(mTextureUniformHandle, 0);


        // Pass in the texture coordinate information

        mTextureCoordinateHandle = GLES20.glGetAttribLocation(program.handle, "a_TexCoordinate");
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, texBuffer);


        mPositionHandle = GLES20.glGetAttribLocation(program.handle, "a_Position");
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                vertexStride, vertexBuffer);

        mColorHandle = GLES20.glGetUniformLocation(program.handle, "u_Color");
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);

        mMVPMatrixHandle = GLES20.glGetUniformLocation(program.handle, "u_MVPMatrix");
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);


        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);

    }

}
