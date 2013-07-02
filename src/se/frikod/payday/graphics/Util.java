package se.frikod.payday.graphics;

import android.opengl.GLES20;
import android.util.Log;

public class Util {
    private static String TAG = "Util";
    private Util(){};

    static int nextPowerOf2(int val){
        val--;
        val = (val >> 1) | val;
        val = (val >> 2) | val;
        val = (val >> 4) | val;
        val = (val >> 8) | val;
        val = (val >> 16) | val;
        val++;
        return val;
    }

    public static void checkGlError() throws RuntimeException {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            RuntimeException e = new RuntimeException("glError " + error);
            Log.e(TAG, "glError " + error + " " + e.getStackTrace()[1].getFileName() + ":" + e.getStackTrace()[1].getLineNumber());
            throw e;
        }
    }
}
