package me.lake.librestreaming.model;

import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;

/**
 * Created by lake on 25/05/16.
 */
public class ScreenGLWapper {
    public EGLDisplay eglDisplay;
    public EGLConfig eglConfig;
    public EGLSurface eglSurface;
    public EGLContext eglContext;

    public int drawProgram;
    public int drawTextureLoc;
    public int drawPostionLoc;
    public int drawTextureCoordLoc;
}
