package me.lake.librestreaming.core;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import me.lake.librestreaming.model.MediaCodecGLWapper;
import me.lake.librestreaming.model.ScreenGLWapper;
import me.lake.librestreaming.tools.GLESTools;

/**
 * Created by lake on 16-5-25.
 */
public class GLHelper {
    private static final int EGL_RECORDABLE_ANDROID = 0x3142;
    private static String VERTEXSHADER = "" +
            "attribute vec4 aPosition;\n" +
            "attribute vec2 aTextureCoord;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main(){\n" +
            "    gl_Position= aPosition;\n" +
            "    vTextureCoord = aTextureCoord;\n" +
            "}";
    private static String FRAGMENTSHADER_CAMERA = "" +
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying mediump vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static String FRAGMENTSHADER_2D = "" +
            "precision mediump float;\n" +
            "varying mediump vec2 vTextureCoord;\n" +
            "uniform sampler2D uTexture;\n" +
            "void main(){\n" +
            "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
            "    gl_FragColor = color;\n" +
            "}";
    private static float SquareVertices[] = {
            -1.0f, 1.0f,
            -1.0f, -1.0f,
            1.0f, -1.0f,
            1.0f, 1.0f};
    private static float CamTextureVertices[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f};
    private static float MediaCodecTextureVertices[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f};
    private static float ScreenTextureVertices[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f};
    public static int FLOAT_SIZE_BYTES = 4;
    public static int SHORT_SIZE_BYTES = 2;
    public static int COORDS_PER_VERTEX = 2;
    public static int TEXTURE_COORDS_PER_VERTEX = 2;

    public static void initMediaCodecGL(MediaCodecGLWapper wapper, Surface mediaInputSurface) {
        wapper.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        wapper.eglConfig = configs[0];
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, EGL14.EGL_NO_CONTEXT, contextSpec, 0);
        if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int[] values = new int[1];
        EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        Log.d("AA", "mediaWapper,EGLContext created, client version " + values[0]);
        wapper.eglSurface = EGL14.eglCreateWindowSurface(wapper.eglDisplay, wapper.eglConfig, mediaInputSurface, surfaceAttribs, 0);
        if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void initScreenGL(ScreenGLWapper wapper, EGLContext sharedContext, SurfaceTexture screenSurface) {
        wapper.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
        if (EGL14.EGL_NO_DISPLAY == wapper.eglDisplay) {
            throw new RuntimeException("eglGetDisplay,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int versions[] = new int[2];
        if (!EGL14.eglInitialize(wapper.eglDisplay, versions, 0, versions, 1)) {
            throw new RuntimeException("eglInitialize,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int configsCount[] = new int[1];
        EGLConfig configs[] = new EGLConfig[1];
        int configSpec[] = new int[]{
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 0,
                EGL14.EGL_STENCIL_SIZE, 0,
                EGL14.EGL_NONE
        };
        EGL14.eglChooseConfig(wapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0);
        if (configsCount[0] <= 0) {
            throw new RuntimeException("eglChooseConfig,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        wapper.eglConfig = configs[0];
        int[] surfaceAttribs = {
                EGL14.EGL_NONE
        };
        int contextSpec[] = new int[]{
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
        };
        wapper.eglContext = EGL14.eglCreateContext(wapper.eglDisplay, wapper.eglConfig, sharedContext, contextSpec, 0);
        if (EGL14.EGL_NO_CONTEXT == wapper.eglContext) {
            throw new RuntimeException("eglCreateContext,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
        int[] values = new int[1];
        EGL14.eglQueryContext(wapper.eglDisplay, wapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, values, 0);
        Log.d("AA", "screenWapper,EGLContext created, client version " + values[0]);
        wapper.eglSurface = EGL14.eglCreateWindowSurface(wapper.eglDisplay, wapper.eglConfig, screenSurface, surfaceAttribs, 0);
        if (null == wapper.eglSurface || EGL14.EGL_NO_SURFACE == wapper.eglSurface) {
            throw new RuntimeException("eglCreateWindowSurface,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void currentMediaCodec(MediaCodecGLWapper wapper) {
        if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void currentScreen(ScreenGLWapper wapper) {
        if (!EGL14.eglMakeCurrent(wapper.eglDisplay, wapper.eglSurface, wapper.eglSurface, wapper.eglContext)) {
            throw new RuntimeException("eglMakeCurrent,failed:" + GLUtils.getEGLErrorString(EGL14.eglGetError()));
        }
    }

    public static void createCamFrameBuff(int[] frameBuffer, int[] frameBufferTex, int width, int height) {
        GLES20.glGenFramebuffers(1, frameBuffer, 0);
        GLES20.glGenTextures(1, frameBufferTex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTex[0]);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0]);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTex[0], 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLESTools.checkGlError("createCamFrameBuff");
    }

    public static void enableVertex(int posLoc, int texLoc, FloatBuffer shapeBuffer, FloatBuffer texBuffer) {
        GLES20.glEnableVertexAttribArray(posLoc);
        GLES20.glEnableVertexAttribArray(texLoc);
        shapeBuffer.position(0);
        GLES20.glVertexAttribPointer(posLoc, COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                COORDS_PER_VERTEX * 4, shapeBuffer);
        texBuffer.position(0);
        GLES20.glVertexAttribPointer(texLoc, TEXTURE_COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false,
                TEXTURE_COORDS_PER_VERTEX * 4, texBuffer);
    }

    public static void disableVertex(int posLoc, int texLoc) {
        GLES20.glDisableVertexAttribArray(posLoc);
        GLES20.glDisableVertexAttribArray(texLoc);
    }

    public static int createCameraProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_CAMERA);
    }

    public static int createMediaCodecProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_2D);
    }

    public static int createScreenProgram() {
        return GLESTools.createProgram(VERTEXSHADER, FRAGMENTSHADER_2D);
    }

    public static FloatBuffer getShapeVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * SquareVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(SquareVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getMediaCodecTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * MediaCodecTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(MediaCodecTextureVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getScreenTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * ScreenTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(ScreenTextureVertices);
        result.position(0);
        return result;
    }

    public static FloatBuffer getCameraTextureVerticesBuffer() {
        FloatBuffer result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * CamTextureVertices.length).
                order(ByteOrder.nativeOrder()).
                asFloatBuffer();
        result.put(CamTextureVertices);
        result.position(0);
        return result;
    }
}