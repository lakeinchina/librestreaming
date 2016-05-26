package me.lake.librestreaming.core;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import me.lake.librestreaming.client.CallbackDelivery;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.RESVideoBuff;
import me.lake.librestreaming.render.GLESRender;
import me.lake.librestreaming.render.IRender;
import me.lake.librestreaming.render.NativeRender;
import me.lake.librestreaming.rtmp.RESFlvData;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.rtmp.RESRtmpSender;
import me.lake.librestreaming.tools.BuffSizeCalculator;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-5-24.
 */
public class RESSoftVideoCore implements RESVideoCore{
    RESCoreParameters resCoreParameters;
    private int currentCamera;
    private RESFlvDataCollecter dataCollecter;
    private MediaCodec dstVideoEncoder;
    private MediaFormat dstVideoFormat;
    //render
    private final Object syncPreview = new Object();
    private IRender previewRender;
    //filter
    private Lock lockVideoFilter = null;

    private BaseVideoFilter videoFilter;
    private VideoFilterHandler videoFilterHandler;
    private HandlerThread videoFilterHandlerThread;
    //sender
    private VideoSenderThread videoSenderThread;
    //VideoBuffs
    //buffers to handle buff from queueVideo
    private RESVideoBuff[] orignVideoBuffs;
    private int lastVideoQueueBuffIndex;
    //buffer to convert orignVideoBuff to NV21 if filter are set
    private RESVideoBuff orignNV21VideoBuff;
    //buffer to handle filtered color from filter if filter are set
    private RESVideoBuff filteredNV21VideoBuff;
    //buffer to convert other color format to suitable color format for dstVideoEncoder if nessesary
    private RESVideoBuff suitable4VideoEncoderBuff;

    final private Object syncResScreenShotListener = new Object();
    private RESScreenShotListener resScreenShotListener;

    public RESSoftVideoCore(RESCoreParameters parameters) {
        resCoreParameters = parameters;
        lockVideoFilter = new ReentrantLock(false);
        videoFilter = null;
    }

    public void setCurrentCamera(int camIndex) {
        currentCamera = camIndex;
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
        dstVideoEncoder = MediaCodecHelper.createSoftVideoMediaCodec(resCoreParameters, dstVideoFormat);
        if (dstVideoEncoder == null) {
            LogTools.e("create Video MediaCodec failed");
            return false;
        }
        //video
        int videoWidth = resCoreParameters.videoWidth;
        int videoHeight = resCoreParameters.videoHeight;
        int videoQueueNum = resCoreParameters.videoBufferQueueNum;
        int orignVideoBuffSize = BuffSizeCalculator.calculator(videoWidth, videoHeight, resCoreParameters.previewColorFormat);
        orignVideoBuffs = new RESVideoBuff[videoQueueNum];
        for (int i = 0; i < videoQueueNum; i++) {
            orignVideoBuffs[i] = new RESVideoBuff(resCoreParameters.previewColorFormat, orignVideoBuffSize);
        }
        resCoreParameters.previewBufferSize = orignVideoBuffSize;
        orignNV21VideoBuff = new RESVideoBuff(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                BuffSizeCalculator.calculator(videoWidth, videoHeight, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar));
        filteredNV21VideoBuff = new RESVideoBuff(MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                BuffSizeCalculator.calculator(videoWidth, videoHeight, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar));
        suitable4VideoEncoderBuff = new RESVideoBuff(resCoreParameters.mediacodecAVCColorFormat,
                BuffSizeCalculator.calculator(videoWidth, videoHeight, resCoreParameters.mediacodecAVCColorFormat));
        return true;
    }

    public void queueVideo(byte[] rawVideoFrame) {
        int targetIndex = (lastVideoQueueBuffIndex + 1) % orignVideoBuffs.length;
        if (orignVideoBuffs[targetIndex].isReadyToFill) {
            LogTools.d("queueVideo,accept ,targetIndex" + targetIndex);
            acceptVideo(rawVideoFrame, orignVideoBuffs[targetIndex].buff);
            orignVideoBuffs[targetIndex].isReadyToFill = false;
            lastVideoQueueBuffIndex = targetIndex;
            videoFilterHandler.sendMessage(videoFilterHandler.obtainMessage(VideoFilterHandler.WHAT_INCOMING_BUFF, targetIndex, 0));
        } else {
            LogTools.d("queueVideo,abandon,targetIndex" + targetIndex);
        }
    }


    private void acceptVideo(byte[] src, byte[] dst) {
        int directionFlag = currentCamera == Camera.CameraInfo.CAMERA_FACING_BACK ? resCoreParameters.backCameraDirectionMode : resCoreParameters.frontCameraDirectionMode;
        ColorHelper.NV21Transform(src,
                dst,
                resCoreParameters.previewVideoWidth,
                resCoreParameters.previewVideoHeight,
                directionFlag);
    }

    @Override
    public boolean start(RESFlvDataCollecter flvDataCollecter,SurfaceTexture camTex) {
        try {
            dataCollecter = flvDataCollecter;
            for (RESVideoBuff buff : orignVideoBuffs) {
                buff.isReadyToFill = true;
            }
            if (dstVideoEncoder == null) {
                dstVideoEncoder = MediaCodec.createEncoderByType(dstVideoFormat.getString(MediaFormat.KEY_MIME));
            }
            dstVideoEncoder.configure(dstVideoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            dstVideoEncoder.start();
            lastVideoQueueBuffIndex = 0;
            videoFilterHandlerThread = new HandlerThread("videoFilterHandlerThread");
            videoSenderThread = new VideoSenderThread("VideoSenderThread");
            videoFilterHandlerThread.start();
            videoSenderThread.start();
            videoFilterHandler = new VideoFilterHandler(videoFilterHandlerThread.getLooper());
        } catch (Exception e) {
            LogTools.trace("RESVideoClient.start()failed", e);
            return false;
        }
        return true;
    }

    public boolean stop() {
        videoFilterHandler.removeCallbacksAndMessages(null);
        videoFilterHandlerThread.quit();
        videoSenderThread.quit();
        try {
            videoFilterHandlerThread.join();
            videoSenderThread.join();
        } catch (InterruptedException e) {
            LogTools.trace("RESCore", e);
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
        lockVideoFilter.lock();
        if (videoFilter != null) {
            videoFilter.onDestroy();
        }
        lockVideoFilter.unlock();
        return true;
    }
    @Override
    public void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        synchronized (syncPreview) {
            if (previewRender != null) {
                throw new RuntimeException("createPreview without destroy previous");
            }
            switch (resCoreParameters.renderingMode) {
                case RESCoreParameters.RENDERING_MODE_NATIVE_WINDOW:
                    previewRender = new NativeRender();
                    break;
                case RESCoreParameters.RENDERING_MODE_OPENGLES:
                    previewRender = new GLESRender();
                    break;
                default:
                    throw new RuntimeException("Unknow rendering mode");
            }
            previewRender.create(surfaceTexture,
                    resCoreParameters.previewColorFormat,
                    resCoreParameters.videoWidth,
                    resCoreParameters.videoHeight,
                    visualWidth,
                    visualHeight);
        }
    }
    @Override
    public void updatePreview(int visualWidth, int visualHeight) {
        synchronized (syncPreview) {
            if (previewRender == null) {
                throw new RuntimeException("updatePreview without createPreview");
            }
            previewRender.update(visualWidth, visualHeight);
        }
    }
    @Override
    public void destroyPreview() {
        synchronized (syncPreview) {
            if (previewRender == null) {
                throw new RuntimeException("destroyPreview without createPreview");
            }
            previewRender.destroy();
            previewRender = null;
        }
    }

    /**
     * use it to update filter property.<br/>
     * call it with {@link #releaseVideoFilter()}<br/>
     * make sure to release it in 3ms
     *
     * @return the videofilter in use
     */
    public BaseVideoFilter acquireVideoFilter() {
        lockVideoFilter.lock();
        return videoFilter;
    }

    /**
     * call it with {@link #acquireVideoFilter()}
     */
    public void releaseVideoFilter() {
        lockVideoFilter.unlock();
    }

    /**
     * set videofilter.<br/>
     * can be called Repeatedly.<br/>
     * do NOT call it between {@link #acquireVideoFilter()} & {@link #releaseVideoFilter()}
     *
     * @param baseVideoFilter
     */
    public void setVideoFilter(BaseVideoFilter baseVideoFilter) {
        lockVideoFilter.lock();
        if (videoFilter != null) {
            videoFilter.onDestroy();
        }
        videoFilter = baseVideoFilter;
        if (videoFilter != null) {
            videoFilter.onInit(resCoreParameters.videoWidth, resCoreParameters.videoHeight);
        }
        lockVideoFilter.unlock();
    }
    @Override
    public void takeScreenShot(RESScreenShotListener listener) {
        synchronized (syncResScreenShotListener) {
            resScreenShotListener = listener;
        }
    }

    //worker handler
    private class VideoFilterHandler extends Handler {
        public static final int FILTER_LOCK_TOLERATION = 3;//3ms
        public static final int WHAT_INCOMING_BUFF = 1;
        private int sequenceNum;

        VideoFilterHandler(Looper looper) {
            super(looper);
            sequenceNum = 0;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != WHAT_INCOMING_BUFF) {
                return;
            }
            sequenceNum++;
            long nowTimeMs = System.currentTimeMillis();
            int targetIndex = msg.arg1;
            boolean isFilterLocked = lockVideoFilter();
            if (isFilterLocked) {
                /**
                 * orignVideoBuffs[targetIndex] is ready
                 * orignVideoBuffs[targetIndex]->orignNV21VideoBuff
                 */
                if (orignVideoBuffs[targetIndex].colorFormat == ImageFormat.NV21) {
                    System.arraycopy(orignVideoBuffs[targetIndex].buff, 0,
                            orignNV21VideoBuff.buff, 0, orignNV21VideoBuff.buff.length);
                } else if (orignVideoBuffs[targetIndex].colorFormat == ImageFormat.YV12) {
                    //LAKETODO colorconvert
                }
                orignVideoBuffs[targetIndex].isReadyToFill = true;
                boolean modified;
                modified = videoFilter.onFrame(orignNV21VideoBuff.buff, filteredNV21VideoBuff.buff, nowTimeMs, sequenceNum);
                unlockVideoFilter();
                rendering(modified ? filteredNV21VideoBuff.buff : orignNV21VideoBuff.buff);
                checkScreenShot(modified ? filteredNV21VideoBuff.buff : orignNV21VideoBuff.buff);
                /**
                 * orignNV21VideoBuff is ready
                 * orignNV21VideoBuff->suitable4VideoEncoderBuff
                 */
                if (resCoreParameters.mediacodecAVCColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                    ColorHelper.NV21TOYUV420SP(modified ? filteredNV21VideoBuff.buff : orignNV21VideoBuff.buff,
                            suitable4VideoEncoderBuff.buff, resCoreParameters.videoWidth * resCoreParameters.videoHeight);
                } else if (resCoreParameters.mediacodecAVCColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                    ColorHelper.NV21TOYUV420P(modified ? filteredNV21VideoBuff.buff : orignNV21VideoBuff.buff,
                            suitable4VideoEncoderBuff.buff, resCoreParameters.videoWidth * resCoreParameters.videoHeight);
                } else {//LAKETODO colorConvert
                }
            } else {
                rendering(orignVideoBuffs[targetIndex].buff);
                checkScreenShot(orignVideoBuffs[targetIndex].buff);
                if (orignVideoBuffs[targetIndex].colorFormat == ImageFormat.NV21) {
                    if (resCoreParameters.mediacodecAVCColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
                        ColorHelper.NV21TOYUV420SP(orignVideoBuffs[targetIndex].buff,
                                suitable4VideoEncoderBuff.buff,
                                resCoreParameters.videoWidth * resCoreParameters.videoHeight);
                    } else if (resCoreParameters.mediacodecAVCColorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar) {
                        ColorHelper.NV21TOYUV420P(orignVideoBuffs[targetIndex].buff,
                                suitable4VideoEncoderBuff.buff,
                                resCoreParameters.videoWidth * resCoreParameters.videoHeight);
                    } else {//LAKETODO colorConvert
                    }

                } else if (orignVideoBuffs[targetIndex].colorFormat == ImageFormat.YV12) {
                } else {
                }
                orignVideoBuffs[targetIndex].isReadyToFill = true;
            }

            //suitable4VideoEncoderBuff is ready
            int eibIndex = dstVideoEncoder.dequeueInputBuffer(-1);
            if (eibIndex >= 0) {
                ByteBuffer dstVideoEncoderIBuffer = dstVideoEncoder.getInputBuffers()[eibIndex];
                dstVideoEncoderIBuffer.position(0);
                dstVideoEncoderIBuffer.put(suitable4VideoEncoderBuff.buff, 0, suitable4VideoEncoderBuff.buff.length);
                dstVideoEncoder.queueInputBuffer(eibIndex, 0, suitable4VideoEncoderBuff.buff.length, nowTimeMs * 1000, 0);
            } else {
                LogTools.d("dstVideoEncoder.dequeueInputBuffer(-1)<0");
            }

            LogTools.d("VideoFilterHandler,ProcessTime:" + (System.currentTimeMillis() - nowTimeMs));
        }

        /**
         * rendering nv21 using native window
         *
         * @param pixel
         */
        private void rendering(byte[] pixel) {
            synchronized (syncPreview) {
                if (previewRender == null) {
                    return;
                }
                previewRender.rendering(pixel);
            }
        }

        /**
         * check if screenshotlistener exist
         *
         * @param pixel
         */
        private void checkScreenShot(byte[] pixel) {
            synchronized (syncResScreenShotListener) {
                if (resScreenShotListener != null) {
                    int[] argbPixel = new int[resCoreParameters.videoWidth * resCoreParameters.videoHeight];
                    ColorHelper.NV21TOARGB(pixel,
                            argbPixel,
                            resCoreParameters.videoWidth,
                            resCoreParameters.videoHeight);
                    Bitmap result = Bitmap.createBitmap(argbPixel,
                            resCoreParameters.videoWidth,
                            resCoreParameters.videoHeight,
                            Bitmap.Config.ARGB_8888);
                    CallbackDelivery.i().post(new RESScreenShotListener.RESScreenShotListenerRunable(resScreenShotListener, result));
                    resScreenShotListener = null;
                }
            }
        }

        /**
         * @return ture if filter locked & filter!=null
         */

        private boolean lockVideoFilter() {
            try {
                boolean locked = lockVideoFilter.tryLock(FILTER_LOCK_TOLERATION, TimeUnit.MILLISECONDS);
                if (locked) {
                    if (videoFilter != null) {
                        return true;
                    } else {
                        lockVideoFilter.unlock();
                        return false;
                    }
                } else {
                    return false;
                }
            } catch (InterruptedException e) {
            }
            return false;
        }

        private void unlockVideoFilter() {
            lockVideoFilter.unlock();
        }
    }

    private class VideoSenderThread extends Thread {
        private static final long WAIT_TIME = 10000;//5ms;
        private MediaCodec.BufferInfo eInfo;
        private long startTime=0;

        VideoSenderThread(String name) {
            super(name);
            eInfo = new MediaCodec.BufferInfo();
            startTime=0;
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
                        /**
                         * we send sps pps already in INFO_OUTPUT_FORMAT_CHANGED
                         * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                         */
                        if (eInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && eInfo.size != 0) {
                            ByteBuffer realData = dstVideoEncoder.getOutputBuffers()[eobIndex];
                            realData.position(eInfo.offset + 4);
                            realData.limit(eInfo.offset + eInfo.size);
                            sendRealData((eInfo.presentationTimeUs / 1000)-startTime, realData);
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
