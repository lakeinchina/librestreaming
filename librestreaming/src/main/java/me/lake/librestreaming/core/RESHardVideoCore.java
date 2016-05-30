package me.lake.librestreaming.core;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.opengl.EGL14;
import android.opengl.EGLExt;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.model.MediaCodecGLWapper;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.ScreenGLWapper;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-5-24.
 */
public class RESHardVideoCore implements RESVideoCore {
    RESCoreParameters resCoreParameters;
    private final Object syncOp = new Object();
    private int currentCamera;
    private MediaCodec dstVideoEncoder;
    private MediaFormat dstVideoFormat;
    private final Object syncPreview = new Object();
    private final Object syncScreenCleanUp = new Object();
    private ScreenParams screenParams;
    private VideoGLThread videoGLThread;
    //sender
    private VideoSenderThread videoSenderThread;

    public RESHardVideoCore(RESCoreParameters parameters) {
        resCoreParameters = parameters;
        screenParams = new ScreenParams();
    }

    public void onFrameAvailable() {
        if (videoGLThread != null) {
            videoGLThread.wakeup();
        }
    }

    @Override
    public boolean prepare(RESConfig resConfig) {
        synchronized (syncOp) {
            resCoreParameters.renderingMode = resConfig.getRenderingMode();
            resCoreParameters.mediacdoecAVCBitRate = resConfig.getBitRate();
            resCoreParameters.videoBufferQueueNum = resConfig.getVideoBufferQueueNum();
            resCoreParameters.mediacodecAVCProfile = MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline;
            resCoreParameters.mediacodecAVClevel = MediaCodecInfo.CodecProfileLevel.AVCLevel31;
            resCoreParameters.mediacodecAVCFrameRate = 30;
            resCoreParameters.mediacodecAVCIFrameInterval = 5;
            dstVideoFormat = new MediaFormat();
            dstVideoEncoder = MediaCodecHelper.createHardVideoMediaCodec(resCoreParameters, dstVideoFormat);
            if (dstVideoEncoder == null) {
                LogTools.e("create Video MediaCodec failed");
                return false;
            }
            return true;
        }
    }

    @Override
    public boolean start(RESFlvDataCollecter flvDataCollecter, SurfaceTexture camTex) {
        synchronized (syncOp) {
            try {
                if (dstVideoEncoder == null) {
                    dstVideoEncoder = MediaCodec.createEncoderByType(dstVideoFormat.getString(MediaFormat.KEY_MIME));
                }
                dstVideoEncoder.configure(dstVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                videoSenderThread = new VideoSenderThread("VideoSenderThread", dstVideoEncoder, flvDataCollecter);
                videoGLThread = new VideoGLThread(camTex, dstVideoEncoder.createInputSurface());
                dstVideoEncoder.start();
                videoSenderThread.start();
                videoGLThread.start();
            } catch (Exception e) {
                LogTools.trace("RESHardVideoCore,start()failed", e);
                return false;
            }
            return true;
        }
    }

    @Override
    public void updateCamTexture(SurfaceTexture camTex) {
        synchronized (syncOp) {
            if (videoGLThread != null) {
                videoGLThread.updateCamTexture(camTex);
            }
        }
    }

    @Override
    public boolean stop() {
        synchronized (syncOp) {
            videoGLThread.quit();
            videoSenderThread.quit();
            try {
                videoGLThread.join();
                videoSenderThread.join();
            } catch (InterruptedException e) {
                LogTools.trace("RESHardVideoCore,stop()failed", e);
                return false;
            }
            dstVideoEncoder.stop();
            dstVideoEncoder.release();
            videoGLThread = null;
            videoSenderThread = null;
            dstVideoEncoder = null;
            return true;
        }
    }

    @Override
    public boolean destroy() {
        synchronized (syncOp) {
            return true;
        }
    }

    @Override
    public void setCurrentCamera(int cameraIndex) {
        currentCamera = cameraIndex;
    }

    @Override
    public void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        synchronized (syncPreview) {
            if (screenParams.surfaceTexture != null) {
                throw new RuntimeException("createPreview without destroyPreview");
            }
            screenParams.surfaceTexture = surfaceTexture;
            screenParams.visualWidth = visualWidth;
            screenParams.visualHeight = visualHeight;
        }
    }

    @Override
    public void updatePreview(int visualWidth, int visualHeight) {
        synchronized (syncPreview) {
            screenParams.visualWidth = visualWidth;
            screenParams.visualHeight = visualHeight;
        }
    }

    @Override
    public void destroyPreview() {
        synchronized (syncOp) {
            if (videoGLThread != null) {
                synchronized (syncPreview) {
                    synchronized (syncScreenCleanUp) {
                        try {
                            syncScreenCleanUp.wait();
                            screenParams.surfaceTexture = null;
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        }
    }

    @Override
    public void takeScreenShot(RESScreenShotListener listener) {

    }

    private class VideoGLThread extends Thread {
        private boolean quit;
        private final Object syncThread = new Object();
        private int frameNum = 0;
        //gl stuff
        private Surface mediaInputSurface;
        private SurfaceTexture cameraTexture;
        private MediaCodecGLWapper mediaCodecGLWapper;
        private ScreenGLWapper screenGLWapper;
        private int frameBuffer;
        private int frameBufferTexture;
        private FloatBuffer shapeVerticesBuffer;
        private FloatBuffer mediaCodecTextureVerticesBuffer;
        private FloatBuffer screenTextureVerticesBuffer;
        private FloatBuffer cameraTextureVerticesBuffer;
        private ShortBuffer drawIndecesBuffer;

        VideoGLThread(SurfaceTexture camTexture, Surface inputSuface) {
            mediaInputSurface = inputSuface;
            cameraTexture = camTexture;
            screenGLWapper = null;
            mediaCodecGLWapper = null;
            quit = false;
        }

        public void updateCamTexture(SurfaceTexture surfaceTexture) {
            if (surfaceTexture != cameraTexture) {
                cameraTexture = surfaceTexture;
                frameNum=0;
            }
        }

        public void quit() {
            synchronized (syncThread) {
                quit = true;
                syncThread.notify();
            }
        }

        public void wakeup() {
            synchronized (syncThread) {
                ++frameNum;
                syncThread.notify();
            }
        }

        @Override
        public void run() {
            initBuffer();
            mediaCodecGLWapper = new MediaCodecGLWapper();
            GLHelper.initMediaCodecGL(mediaCodecGLWapper, mediaInputSurface);
            GLHelper.currentMediaCodec(mediaCodecGLWapper);
            int[] fb = new int[1], fbt = new int[1];
            GLHelper.createCamFrameBuff(fb, fbt, resCoreParameters.videoWidth, resCoreParameters.videoHeight);
            initMediaCodecProgram(mediaCodecGLWapper);
            frameBuffer = fb[0];
            frameBufferTexture = fbt[0];
            while (!quit) {
                GLHelper.currentMediaCodec(mediaCodecGLWapper);
                waitCamera();
                if (quit) {
                    break;
                }
                synchronized (syncThread) {
                    if (cameraTexture != null) {
                        cameraTexture.updateTexImage();
                    }
                }
                drawFrameBuffer();
                drawMediaCodec();
                drawScreen();
                synchronized (syncThread) {
                    frameNum--;
                }
            }
            GLHelper.currentMediaCodec(mediaCodecGLWapper);
            synchronized (syncPreview) {
                synchronized (syncScreenCleanUp) {
                    if (screenGLWapper != null) {
                        Log.e("aa", "drawscreenha");
                        cleanUpScreen();
                    }
                    syncScreenCleanUp.notify();
                }
            }
            cleanUpMedia();
        }

        private void waitCamera() {
            synchronized (syncThread) {
                if (cameraTexture != null) {
                    while (frameNum >= 2) {
                        cameraTexture.updateTexImage();
                        --frameNum;
                    }
                }
                if (frameNum == 0) {
                    try {
                        syncThread.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        private void drawFrame() {
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawIndecesBuffer.limit(), GLES20.GL_UNSIGNED_SHORT, drawIndecesBuffer);
        }

        private void drawFrameBuffer() {
            GLES20.glUseProgram(mediaCodecGLWapper.camProgram);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, OVERWATCH_TEXTURE_ID);
            GLES20.glUniform1i(mediaCodecGLWapper.camTextureLoc, 0);
            GLHelper.enableVertex(mediaCodecGLWapper.camPostionLoc, mediaCodecGLWapper.camTextureCoordLoc,
                    shapeVerticesBuffer, cameraTextureVerticesBuffer);
            GLES20.glViewport(0, 0, resCoreParameters.videoWidth, resCoreParameters.videoHeight);
            drawFrame();
            GLES20.glFinish();
            GLHelper.disableVertex(mediaCodecGLWapper.camPostionLoc, mediaCodecGLWapper.camTextureCoordLoc);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
            GLES20.glUseProgram(0);
        }

        private void drawMediaCodec() {
            GLES20.glUseProgram(mediaCodecGLWapper.drawProgram);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture);
            GLES20.glUniform1i(mediaCodecGLWapper.drawTextureLoc, 0);
            GLHelper.enableVertex(mediaCodecGLWapper.drawTextureLoc, mediaCodecGLWapper.drawTextureCoordLoc,
                    shapeVerticesBuffer, mediaCodecTextureVerticesBuffer);
            drawFrame();
            GLHelper.disableVertex(mediaCodecGLWapper.drawTextureLoc, mediaCodecGLWapper.drawTextureCoordLoc);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glUseProgram(0);
            EGLExt.eglPresentationTimeANDROID(mediaCodecGLWapper.eglDisplay, mediaCodecGLWapper.eglSurface, cameraTexture.getTimestamp());
            if (!EGL14.eglSwapBuffers(mediaCodecGLWapper.eglDisplay, mediaCodecGLWapper.eglSurface)) {
                throw new RuntimeException("eglSwapBuffers,failed!");
            }
        }

        private void drawScreen() {
            synchronized (syncPreview) {
                Log.e("aa", "drawscreen1");
                if (screenParams.surfaceTexture == null) {
                    synchronized (syncScreenCleanUp) {
                        if (screenGLWapper == null) {
                            Log.e("aa", "drawscreen2");
                            return;
                        } else {
                            Log.e("aa", "drawscreen3");
                            cleanUpScreen();
                        }
                        syncScreenCleanUp.notify();
                    }
                }
                if (screenGLWapper == null) {
                    Log.e("aa", "drawscreen4");
                    screenGLWapper = new ScreenGLWapper();
                    GLHelper.initScreenGL(screenGLWapper, mediaCodecGLWapper.eglContext, screenParams.surfaceTexture);
                    initScreenProgram(screenGLWapper);
                }
                Log.e("aa", "drawscreen");
                GLHelper.currentScreen(screenGLWapper);
                //drawScreen
                GLES20.glUseProgram(screenGLWapper.drawProgram);
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture);
                GLES20.glUniform1i(screenGLWapper.drawTextureLoc, 0);
                GLHelper.enableVertex(screenGLWapper.drawTextureLoc, screenGLWapper.drawTextureCoordLoc,
                        shapeVerticesBuffer, screenTextureVerticesBuffer);
                GLES20.glViewport(0, 0, screenParams.visualWidth, screenParams.visualHeight);
                drawFrame();
                GLHelper.disableVertex(screenGLWapper.drawTextureLoc, screenGLWapper.drawTextureCoordLoc);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
                GLES20.glUseProgram(0);
                if (!EGL14.eglSwapBuffers(screenGLWapper.eglDisplay, screenGLWapper.eglSurface)) {
                    throw new RuntimeException("eglSwapBuffers,failed!");
                }
            }
        }

        private void initBuffer() {
            shapeVerticesBuffer = GLHelper.getShapeVerticesBuffer();
            mediaCodecTextureVerticesBuffer = GLHelper.getMediaCodecTextureVerticesBuffer();
            screenTextureVerticesBuffer = GLHelper.getScreenTextureVerticesBuffer();
            cameraTextureVerticesBuffer = GLHelper.getCameraTextureVerticesBuffer();
            drawIndecesBuffer = GLHelper.getDrawIndecesBuffer();
        }

        private void initMediaCodecProgram(MediaCodecGLWapper wapper) {
            GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
            //mediacodec
            wapper.drawProgram = GLHelper.createMediaCodecProgram();
            GLES20.glUseProgram(wapper.drawProgram);
            wapper.drawTextureLoc = GLES20.glGetUniformLocation(wapper.drawProgram, "uTexture");
            wapper.drawPostionLoc = GLES20.glGetAttribLocation(wapper.drawProgram, "aPosition");
            wapper.drawTextureCoordLoc = GLES20.glGetAttribLocation(wapper.drawProgram, "aTextureCoord");
            //camera
            wapper.camProgram = GLHelper.createCameraProgram();
            GLES20.glUseProgram(wapper.camProgram);
            wapper.camTextureLoc = GLES20.glGetUniformLocation(wapper.camProgram, "uTexture");
            wapper.camPostionLoc = GLES20.glGetAttribLocation(wapper.camProgram, "aPosition");
            wapper.camTextureCoordLoc = GLES20.glGetAttribLocation(wapper.camProgram, "aTextureCoord");
        }

        private void initScreenProgram(ScreenGLWapper wapper) {
            wapper.drawProgram = GLHelper.createScreenProgram();
            GLES20.glUseProgram(wapper.drawProgram);
            wapper.drawTextureLoc = GLES20.glGetUniformLocation(wapper.drawProgram, "uTexture");
            wapper.drawPostionLoc = GLES20.glGetAttribLocation(wapper.drawProgram, "aPosition");
            wapper.drawTextureCoordLoc = GLES20.glGetAttribLocation(wapper.drawProgram, "aTextureCoord");
        }

        private void cleanUpMedia() {
            GLHelper.currentMediaCodec(mediaCodecGLWapper);
            GLES20.glDeleteProgram(mediaCodecGLWapper.camProgram);
            GLES20.glDeleteProgram(mediaCodecGLWapper.drawProgram);
            GLES20.glDeleteFramebuffers(1, new int[]{frameBuffer}, 0);
            GLES20.glDeleteTextures(1, new int[]{frameBufferTexture}, 0);
            EGL14.eglDestroySurface(mediaCodecGLWapper.eglDisplay, mediaCodecGLWapper.eglSurface);
            EGL14.eglDestroyContext(mediaCodecGLWapper.eglDisplay, mediaCodecGLWapper.eglContext);
            EGL14.eglTerminate(mediaCodecGLWapper.eglDisplay);
            EGL14.eglMakeCurrent(mediaCodecGLWapper.eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
        }

        private void cleanUpScreen() {
            GLHelper.currentScreen(screenGLWapper);
            GLES20.glDeleteProgram(screenGLWapper.drawProgram);
            EGL14.eglDestroySurface(screenGLWapper.eglDisplay, screenGLWapper.eglSurface);
            EGL14.eglDestroyContext(screenGLWapper.eglDisplay, screenGLWapper.eglContext);
            EGL14.eglTerminate(screenGLWapper.eglDisplay);
        }
    }

    class ScreenParams {
        int visualWidth;
        int visualHeight;
        SurfaceTexture surfaceTexture;
    }
}
