package me.lake.librestreaming.tools;

import android.content.Context;
import android.content.res.Resources;
import android.opengl.GLES20;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by lake on 16-5-4.
 */
public class GLESTools {
    public static String readTextFile(Resources res, int resId) {
        InputStream inputStream = res.openRawResource(resId);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        StringBuilder result = new StringBuilder();
        try {
            while ((line = br.readLine()) != null) {
                result.append(line);
                result.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result.toString();
    }

    public static int createProgram(Resources res, int vertexShaderResId, int fragmentShaderResId) {
        String vertexShaderCode = readTextFile(res, vertexShaderResId);
        String fragmentShaderCode = readTextFile(res, fragmentShaderResId);
        return createProgram(vertexShaderCode, fragmentShaderCode);
    }

    public static int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        if (vertexShaderCode == null || fragmentShaderCode == null) {
            throw new RuntimeException("invalid shader code");
        }
        int vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        int fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);

        GLES20.glShaderSource(vertexShader, vertexShaderCode);
        GLES20.glShaderSource(fragmentShader, fragmentShaderCode);
        int[] status = new int[1];
        GLES20.glCompileShader(vertexShader);
        GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("vertext shader compile,failed:" + GLES20.glGetShaderInfoLog(vertexShader));
        }
        GLES20.glCompileShader(fragmentShader);
        GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("fragment shader compile,failed:" + GLES20.glGetShaderInfoLog(fragmentShader));
        }
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0);
        if (GLES20.GL_FALSE == status[0]) {
            throw new RuntimeException("link program,failed:" + GLES20.glGetProgramInfoLog(program));
        }
        return program;
    }
    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            LogTools.d(msg);
            throw new RuntimeException(msg);
        }
    }
}
