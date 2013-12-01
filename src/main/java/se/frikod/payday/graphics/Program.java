package se.frikod.payday.graphics;


import android.opengl.GLES20;
import android.util.Log;
import se.frikod.payday.GraphRendererGL;

public class Program {
    public final int handle;
    private static String TAG ="Payday.graphics.Program";
    public Program(String vertexShaderCode, String fragmentShaderCode){
        handle = GLES20.glCreateProgram();

        int vertexShader = GraphRendererGL.loadShader(GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = GraphRendererGL.loadShader(GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        GLES20.glAttachShader(handle, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(handle, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(handle);                  // create OpenGL program executables
        checkGlError("glLinkProgram");

    }
    public void use(){
        GLES20.glUseProgram(handle);
        Util.checkGlError();
    }
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

}
