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

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.model.MediaCodecGLWapper;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.ScreenGLWapper;
import me.lake.librestreaming.rtmp.RESFlvData;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.rtmp.RESRtmpSender;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-5-24.
 */
public class RESHardVideoCore implements RESVideoCore {
    RESCoreParameters resCoreParameters;
    private int currentCamera;
    private RESFlvDataCollecter dataCollecter;
    private MediaCodec dstVideoEncoder;
    private MediaFormat dstVideoFormat;
    //sender
    private VideoSenderThread videoSenderThread;
    private VideoGLThread videoGLThread;

    public RESHardVideoCore(RESCoreParameters parameters) {
        resCoreParameters = parameters;
    }

    public void onFrameAvailable() {
        videoGLThread.wakeup();
    }

    @Override
    public boolean prepare(RESConfig resConfig) {
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

    @Override
    public boolean start(RESFlvDataCollecter flvDataCollecter, SurfaceTexture camTex) {
        try {
            dataCollecter = flvDataCollecter;
            if (dstVideoEncoder == null) {
                dstVideoEncoder = MediaCodec.createEncoderByType(dstVideoFormat.getString(MediaFormat.KEY_MIME));
            }
            dstVideoEncoder.configure(dstVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            videoSenderThread = new VideoSenderThread("VideoSenderThread");
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

    @Override
    public boolean stop() {
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
        dstVideoEncoder = null;
        dataCollecter = null;
        return true;
    }

    @Override
    public boolean destroy() {
        return false;
    }

    @Override
    public void setCurrentCamera(int cameraIndex) {
        currentCamera = cameraIndex;
    }

    @Override
    public void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {

    }

    @Override
    public void updatePreview(int visualWidth, int visualHeight) {

    }

    @Override
    public void destroyPreview() {

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
            quit = false;
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
            cameraTexture.attachToGLContext(OVERWATCH_TEXTURE_ID);
            while (!quit) {
                waitCamera();
                cameraTexture.updateTexImage();
                drawFrameBuffer();
                drawMediaCodec();
                drawScreen();
                synchronized (syncThread) {
                    frameNum--;
                }
            }
            cameraTexture.detachFromGLContext();
        }

        private void waitCamera() {
            synchronized (syncThread) {
                while (frameNum >= 2) {
                    cameraTexture.updateTexImage();
                    --frameNum;
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
    }

    private class VideoSenderThread extends Thread {
        private static final long WAIT_TIME = 10000;//5ms;
        private MediaCodec.BufferInfo eInfo;
        private long startTime = 0;

        VideoSenderThread(String name) {
            super(name);
            eInfo = new MediaCodec.BufferInfo();
            startTime = 0;
        }

        private boolean shouldQuit = false;

        void quit() {
            shouldQuit = true;
            this.interrupt();
        }

        @Override
        public void run() {
            while (!shouldQuit) {
                int eobIndex = dstVideoEncoder.dequeueOutputBuffer(eInfo, WAIT_TIME);
                switch (eobIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        LogTools.d("VideoSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        LogTools.d("VideoSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                                dstVideoEncoder.getOutputFormat().toString());
                        sendAVCDecoderConfigurationRecord(0, dstVideoEncoder.getOutputFormat());
                        break;
                    default:
                        LogTools.d("VideoSenderThread,MediaCode,eobIndex=" + eobIndex);
                        if (startTime == 0) {
                            startTime = eInfo.presentationTimeUs / 1000;
                        }
                        Log.e("aa", "eInfo.presentationTimeUs" + eInfo.presentationTimeUs);
                        /**
                         * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                         * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                         */
                        if (eInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && eInfo.size != 0) {
                            ByteBuffer realData = dstVideoEncoder.getOutputBuffers()[eobIndex];
                            realData.position(eInfo.offset + 4);
                            realData.limit(eInfo.offset + eInfo.size);
                            sendRealData((eInfo.presentationTimeUs / 1000) - startTime, realData);
                        }
                        dstVideoEncoder.releaseOutputBuffer(eobIndex, false);
                        break;
                }
            }
            eInfo = null;
        }

        private void sendAVCDecoderConfigurationRecord(long tms, MediaFormat format) {
            byte[] AVCDecoderConfigurationRecord = Packager.H264Packager.generateAVCDecoderConfigurationRecord(format);
            int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                    AVCDecoderConfigurationRecord.length;
            byte[] finalBuff = new byte[packetLen];
            Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                    0,
                    true,
                    true,
                    AVCDecoderConfigurationRecord.length);
            System.arraycopy(AVCDecoderConfigurationRecord, 0,
                    finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH, AVCDecoderConfigurationRecord.length);
            RESFlvData resFlvData = new RESFlvData();
            resFlvData.byteBuffer = finalBuff;
            resFlvData.size = finalBuff.length;
            resFlvData.dts = (int) tms;
            resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
            resFlvData.videoFrameType = RESFlvData.NALU_TYPE_IDR;
            dataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
        }

        private void sendRealData(long tms, ByteBuffer realData) {
            int realDataLength = realData.remaining();
            int packetLen = Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                    Packager.FLVPackager.NALU_HEADER_LENGTH +
                    realDataLength;
            byte[] finalBuff = new byte[packetLen];
            realData.get(finalBuff, Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                            Packager.FLVPackager.NALU_HEADER_LENGTH,
                    realDataLength);
            int frameType = finalBuff[Packager.FLVPackager.FLV_VIDEO_TAG_LENGTH +
                    Packager.FLVPackager.NALU_HEADER_LENGTH] & 0x1F;
            Packager.FLVPackager.fillFlvVideoTag(finalBuff,
                    0,
                    false,
                    frameType == 5,
                    realDataLength);
            RESFlvData resFlvData = new RESFlvData();
            resFlvData.byteBuffer = finalBuff;
            resFlvData.size = finalBuff.length;
            resFlvData.dts = (int) tms;
            resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO;
            resFlvData.videoFrameType = frameType;
            dataCollecter.collect(resFlvData, RESRtmpSender.FROM_VIDEO);
        }
    }
}
