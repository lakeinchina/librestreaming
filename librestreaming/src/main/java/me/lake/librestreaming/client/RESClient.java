package me.lake.librestreaming.client;


import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.view.Surface;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import me.lake.librestreaming.core.CameraHelper;
import me.lake.librestreaming.core.RESCore;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-3-16.
 */
public class RESClient {
    private static int[] supportedSrcVideoFrameColorType = new int[]{ImageFormat.NV21, ImageFormat.YV12};
    private int cameraNum;
    private int currentCameraIndex;
    private RESCore resCore;
    private SurfaceTexture videoTexture;
    private Camera camera;

    private final Object SyncOp;

    private AudioRecordThread audioRecordThread;
    private AudioRecord audioRecord;
    private byte[] audioBuffer;

    //parameters
    RESCoreParameters coreParameters;

    public RESClient() {
        cameraNum = Camera.getNumberOfCameras();
        currentCameraIndex = 0;
        SyncOp = new Object();
        coreParameters = new RESCoreParameters();
        CallbackDelivery.i();
    }

    public boolean prepare(RESConfig resConfig) {
        synchronized (SyncOp) {
            coreParameters.rtmpAddr = resConfig.getRtmpAddr();
            coreParameters.printDetailMsg = resConfig.isPrintDetailMsg();
            coreParameters.mediacdoecAVCBitRate = resConfig.getBitRate();
            coreParameters.videoBufferQueueNum = resConfig.getVideoBufferQueueNum();
            coreParameters.audioBufferQueueNum = 5;

            if (null == (camera = createCamera(currentCameraIndex))) {
                LogTools.e("can not open camera");
                return false;
            }
            Camera.Parameters parameters = camera.getParameters();
            CameraHelper.selectCameraFpsRange(parameters, coreParameters);
            CameraHelper.selectCameraPreviewWH(parameters, coreParameters, resConfig.getTargetVideoSize());
            //预览支持的颜色格式
            List<Integer> srcColorTypes = new LinkedList<>();
            List<Integer> supportedPreviewFormates = parameters.getSupportedPreviewFormats();
            for (int colortype : supportedSrcVideoFrameColorType) {
                if (supportedPreviewFormates.contains(colortype)) {
                    srcColorTypes.add(colortype);
                }
            }
            resCore = new RESCore();
            if (!resCore.config(coreParameters, srcColorTypes)) {
                LogTools.e("resCore.config,failed");
                coreParameters.dump();
                return false;
            }
            if (!CameraHelper.configCamera(camera, coreParameters)) {
                LogTools.e("CameraHelper.configCamera,Failed");
                coreParameters.dump();
                return false;
            }
            //all parameters have been set
            if (!resCore.init(coreParameters)) {
                LogTools.e("resCore.init,failed");
                coreParameters.dump();
                return false;
            }
            coreParameters.audioRecoderFormat = AudioFormat.ENCODING_PCM_16BIT;
            coreParameters.audioRecoderChannelConfig = AudioFormat.CHANNEL_IN_MONO;
            coreParameters.audioRecoderSliceSize = coreParameters.mediacodecAACSampleRate / 10;
            coreParameters.audioRecoderBufferSize = coreParameters.audioRecoderSliceSize * 2;
            coreParameters.audioRecoderSource = MediaRecorder.AudioSource.DEFAULT;
            coreParameters.audioRecoderSampleRate = coreParameters.mediacodecAACSampleRate;
            coreParameters.done = true;

            prepareVideo();
            prepareAudio();
            LogTools.d("===INFO===coreParametersReady:");
            LogTools.d(coreParameters.toString());
            return true;
        }
    }


    public void start() {
        synchronized (SyncOp) {
            if (!startVideo()) {
                coreParameters.dump();
                LogTools.e("RESClient,start(),failed");
                return;
            }
            if (!startAudio()) {
                coreParameters.dump();
                LogTools.e("RESClient,start(),failed");
                return;
            }
            resCore.start();
            LogTools.d("RESClient,start()");
        }
    }

    /**
     * call it AFTER {@link #prepare(RESConfig)}
     * @param surface to rendering preview
     */
    public void setPreview(Surface surface) {
        resCore.setPreview(surface);
    }

    /**
     * change camera on running.
     * call it AFTER {@link #start()} & BEFORE {@link #stop()}
     */
    public void swapCamera() {
        synchronized (SyncOp) {
            LogTools.d("RESClient,swapCamera()");
            camera.stopPreview();
            camera.release();
            camera = createCamera(currentCameraIndex = (++currentCameraIndex) % cameraNum);
            CameraHelper.configCamera(camera, coreParameters);
            prepareVideo();
            startVideo();
        }
    }

    public void stop() {
        synchronized (SyncOp) {
            resCore.stop();
            camera.stopPreview();
            audioRecordThread.quit();
            try {
                audioRecordThread.join();
            } catch (InterruptedException e) {
            }
            audioRecordThread = null;
            audioRecord.stop();
            LogTools.d("RESClient,stop()");
        }
    }

    public void destroy() {
        synchronized (SyncOp) {
            resCore.destroy();
            camera.release();
            audioRecord.release();
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
        return resCore.acquireVideoFilter();
    }

    /**
     * call it with {@link #acquireVideoFilter()}
     */
    public void releaseVideoFilter() {
        resCore.releaseVideoFilter();
    }

    /**
     * set videofilter.<br/>
     * can be called Repeatedly.<br/>
     * do NOT call it between {@link #acquireVideoFilter()} & {@link #releaseVideoFilter()}
     *
     * @param baseVideoFilter videofilter to apply
     */
    public void setVideoFilter(BaseVideoFilter baseVideoFilter) {
        resCore.setVideoFilter(baseVideoFilter);
    }

    /**
     * get video & audio real send Speed
     *
     * @return speed in B/s
     */
    public int getAVSpeed() {
        return resCore.getTotalSpeed();
    }

    /**
     * call it AFTER {@link #prepare(RESConfig)}
     *
     * @param connectionListener
     */
    public void setConnectionListener(RESConnectionListener connectionListener) {
        resCore.setConnectionListener(connectionListener);
    }

    /**
     * get the param of video,audio,mediacodec
     *
     * @return info
     */
    public String getConfigInfo() {
        return coreParameters.toString();
    }

    /**
     * set zoom by percent [0.0f,1.0f]
     *
     * @param targetPercent zoompercent
     */
    public void setZoomByPercent(float targetPercent) {
        targetPercent = Math.min(Math.max(0f, targetPercent), 1f);
        Camera.Parameters p = camera.getParameters();
        p.setZoom((int) (p.getMaxZoom() * targetPercent));
        camera.setParameters(p);
    }

    /**
     * toggle flash light
     *
     * @return true if operation success
     */
    public boolean toggleFlashLight() {
        try {
            Camera.Parameters parameters = camera.getParameters();
            List<String> flashModes = parameters.getSupportedFlashModes();
            String flashMode = parameters.getFlashMode();
            if (!Camera.Parameters.FLASH_MODE_TORCH.equals(flashMode)) {
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                    camera.setParameters(parameters);
                    return true;
                }
            } else if (!Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)) {
                if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    camera.setParameters(parameters);
                    return true;
                }
            }
        } catch (Exception e) {
            LogTools.d("toggleFlashLight,failed" + e.getMessage());
            return false;
        }
        return false;
    }

    public void takeScreenShot(RESScreenShotListener listener) {
        resCore.takeScreenShot(listener);
    }

    /**
     * =====================PRIVATE=================
     **/
    private Camera createCamera(int cameraId) {
        camera = Camera.open(cameraId);
        try {
            videoTexture = new SurfaceTexture(10);
            camera.setPreviewTexture(videoTexture);
        } catch (IOException e) {
            LogTools.trace(e);
            return null;
        }
        return camera;
    }

    private boolean startVideo() {
        //some fucking phone release their callback after stopPreview
        //so we set it at startVideo
        camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                if (resCore != null && data != null) {
                    resCore.queueVideo(data);
                }
                camera.addCallbackBuffer(data);
            }
        });
        camera.startPreview();
        return true;
    }

    private boolean prepareVideo() {
        camera.addCallbackBuffer(new byte[coreParameters.previewBufferSize]);
        camera.addCallbackBuffer(new byte[coreParameters.previewBufferSize]);
        return true;
    }

    private boolean prepareAudio() {
        int minBufferSize = AudioRecord.getMinBufferSize(coreParameters.audioRecoderSampleRate,
                coreParameters.audioRecoderChannelConfig,
                coreParameters.audioRecoderFormat);
        audioRecord = new AudioRecord(coreParameters.audioRecoderSource,
                coreParameters.audioRecoderSampleRate,
                coreParameters.audioRecoderChannelConfig,
                coreParameters.audioRecoderFormat,
                minBufferSize * 5);
        audioBuffer = new byte[coreParameters.audioRecoderBufferSize];
        if (AudioRecord.STATE_INITIALIZED != audioRecord.getState()) {
            LogTools.e("audioRecord.getState()!=AudioRecord.STATE_INITIALIZED!");
            return false;
        }
        if (AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(coreParameters.audioRecoderSliceSize)) {
            LogTools.e("AudioRecord.SUCCESS != audioRecord.setPositionNotificationPeriod(coreParameters.audioRecoderSliceSize");
            return false;
        }
        return true;
    }

    private boolean startAudio() {
        audioRecord.startRecording();
        audioRecordThread = new AudioRecordThread();
        audioRecordThread.start();
        return true;
    }


    class AudioRecordThread extends Thread {
        private boolean isRunning = true;

        AudioRecordThread() {
            isRunning = true;
        }

        public void quit() {
            isRunning = false;
        }

        @Override
        public void run() {
            LogTools.d("AudioRecordThread,tid=" + Thread.currentThread().getId());
            while (isRunning) {
                int size = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (isRunning && resCore != null && size > 0) {
                    resCore.queueAudio(audioBuffer);
                }
            }
        }
    }

}
