package me.lake.librestreaming.client;


import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import java.util.List;

import me.lake.librestreaming.core.CameraHelper;
import me.lake.librestreaming.core.RESCore;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.rtmp.RESFlvData;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.rtmp.RESRtmpSender;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-3-16.
 */
public class RESClient {
    private RESVideoClient videoClient;
    private RESAudioClient audioClient;
    private final Object SyncOp;
    //parameters
    RESCoreParameters coreParameters;
    private RESRtmpSender rtmpSender;
    private RESFlvDataCollecter dataCollecter;

    public RESClient() {
        SyncOp = new Object();
        coreParameters = new RESCoreParameters();
        CallbackDelivery.i();
    }

    public boolean prepare(RESConfig resConfig) {
        synchronized (SyncOp) {
            checkDirection(resConfig);
            coreParameters.filterMode=resConfig.getFilterMode();
            coreParameters.rtmpAddr = resConfig.getRtmpAddr();
            coreParameters.printDetailMsg = resConfig.isPrintDetailMsg();
            coreParameters.senderQueueLength = 150;
            switch (coreParameters.filterMode) {
                case RESCoreParameters.FILTER_MODE_SOFT:
                    videoClient = new RESSoftVideoClient(coreParameters);
                    break;
                case RESCoreParameters.FILTER_MODE_HARD:
                    videoClient = new RESHardVideoClient(coreParameters);
                    break;
                default:
                    return false;
            }
            audioClient = new RESSoftAudioClient(coreParameters);
            if (!videoClient.prepare(resConfig)) {
                LogTools.d("!!!!!videoClient.prepare()failed");
                LogTools.d(coreParameters.toString());
                return false;
            }
            if (!audioClient.prepare(resConfig)) {
                LogTools.d("!!!!!audioClient.prepare()failed");
                LogTools.d(coreParameters.toString());
                return false;
            }
            rtmpSender = new RESRtmpSender();
            rtmpSender.prepare(coreParameters);
            dataCollecter = new RESFlvDataCollecter() {
                @Override
                public void collect(RESFlvData flvData, int type) {
                    rtmpSender.feed(flvData, type);
                }
            };
            coreParameters.done = true;
            LogTools.d("===INFO===coreParametersReady:");
            LogTools.d(coreParameters.toString());
            return true;
        }
    }


    public void start() {
        synchronized (SyncOp) {
            rtmpSender.start(coreParameters.rtmpAddr);
            videoClient.start(dataCollecter);
            audioClient.start(dataCollecter);
            LogTools.d("RESClient,start()");
        }
    }

    public void stop() {
        synchronized (SyncOp) {
            videoClient.stop();
            audioClient.stop();
            rtmpSender.stop();
            LogTools.d("RESClient,stop()");
        }
    }

    public void destroy() {
        synchronized (SyncOp) {
            rtmpSender.destroy();
            videoClient.destroy();
            audioClient.destroy();
            LogTools.d("RESClient,destroy()");
        }
    }

    /**
     * call it AFTER {@link #prepare(RESConfig)}
     *
     * @param surfaceTexture to rendering preview
     */
    public void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {
        videoClient.createPreview(surfaceTexture, visualWidth, visualHeight);
    }

    public void updatePreview(int visualWidth, int visualHeight) {
        videoClient.updatePreview(visualWidth, visualHeight);
    }

    public void destroyPreview() {
        videoClient.destroyPreview();
    }

    /**
     * change camera on running.
     * call it AFTER {@link #start()} & BEFORE {@link #stop()}
     */
    public boolean swapCamera() {
        synchronized (SyncOp) {
            LogTools.d("RESClient,swapCamera()");
            return videoClient.swapCamera();
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
        return videoClient.acquireVideoFilter();
    }

    /**
     * call it with {@link #acquireVideoFilter()}
     */
    public void releaseVideoFilter() {
        videoClient.releaseVideoFilter();
    }

    /**
     * set videofilter.<br/>
     * can be called Repeatedly.<br/>
     * do NOT call it between {@link #acquireVideoFilter()} & {@link #releaseVideoFilter()}
     *
     * @param baseVideoFilter videofilter to apply
     */
    public void setVideoFilter(BaseVideoFilter baseVideoFilter) {
        videoClient.setVideoFilter(baseVideoFilter);
    }

    /**
     * get video & audio real send Speed
     *
     * @return speed in B/s
     */
    public int getAVSpeed() {
        return rtmpSender.getTotalSpeed();
    }

    /**
     * call it AFTER {@link #prepare(RESConfig)}
     *
     * @param connectionListener
     */
    public void setConnectionListener(RESConnectionListener connectionListener) {
        rtmpSender.setConnectionListener(connectionListener);
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
    public boolean setZoomByPercent(float targetPercent) {
        return videoClient.setZoomByPercent(targetPercent);
    }

    /**
     * toggle flash light
     *
     * @return true if operation success
     */
    public boolean toggleFlashLight() {
        return videoClient.toggleFlashLight();
    }

    public void takeScreenShot(RESScreenShotListener listener) {
        videoClient.takeScreenShot(listener);
    }

    /**
     * =====================PRIVATE=================
     **/
    private void checkDirection(RESConfig resConfig) {
        int frontFlag = resConfig.getFrontCameraDirectionMode();
        int backFlag = resConfig.getBackCameraDirectionMode();
        int fbit = 0;
        int bbit = 0;
        if ((frontFlag >> 4) == 0) {
            frontFlag |= RESCoreParameters.FLAG_DIRECTION_ROATATION_0;
        }
        if ((backFlag >> 4) == 0) {
            backFlag |= RESCoreParameters.FLAG_DIRECTION_ROATATION_0;
        }
        for (int i = 4; i <= 8; ++i) {
            if (((frontFlag >> i) & 0x1) == 1) {
                fbit++;
            }
            if (((backFlag >> i) & 0x1) == 1) {
                bbit++;
            }
        }
        if (fbit != 1 || bbit != 1) {
            throw new RuntimeException("invalid direction rotation flag:frontFlagNum=" + fbit + ",backFlagNum=" + bbit);
        }
        if (((frontFlag & RESCoreParameters.FLAG_DIRECTION_ROATATION_0) != 0) || ((frontFlag & RESCoreParameters.FLAG_DIRECTION_ROATATION_180) != 0)) {
            fbit = 0;
        } else {
            fbit = 1;
        }
        if (((backFlag & RESCoreParameters.FLAG_DIRECTION_ROATATION_0) != 0) || ((backFlag & RESCoreParameters.FLAG_DIRECTION_ROATATION_180) != 0)) {
            bbit = 0;
        } else {
            bbit = 1;
        }
        if (bbit != fbit) {
            if (bbit == 0) {
                throw new RuntimeException("invalid direction rotation flag:back camera is landscape but front camera is portrait");
            } else {
                throw new RuntimeException("invalid direction rotation flag:back camera is portrait but front camera is landscape");
            }
        }
        if (fbit == 1) {
            coreParameters.isPortrait = true;
        } else {
            coreParameters.isPortrait = false;
        }
        coreParameters.backCameraDirectionMode = backFlag;
        coreParameters.frontCameraDirectionMode = frontFlag;
    }
    static {
        System.loadLibrary("restreaming");
    }
}
