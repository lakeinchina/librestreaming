package me.lake.librestreaming.core;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.HandlerThread;

import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.model.RESAudioBuff;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.model.RESVideoBuff;
import me.lake.librestreaming.rtmp.RESRtmpSender;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-3-16.
 */
public class RESCore {

    private RESCoreParameters resCoreParameters;

    //filter
    final private Object syncThread = new Object();

    //STATE
    private enum STATE {
        IDLE,
        PREPARED,
        RUNING,
        STOPPED
    }

    private STATE runState;

    private long startTimeMs;

    private RESRtmpSender rtmpSender;


    public RESCore() {
        runState = STATE.IDLE;
    }

    public boolean prepare(RESCoreParameters coreParameters) {
        resCoreParameters = coreParameters;
        rtmpSender = new RESRtmpSender();
        rtmpSender.prepare(coreParameters);
        runState = STATE.PREPARED;
        return true;
    }

    public void setConnectionListener(RESConnectionListener connectionListener) {
        rtmpSender.setConnectionListener(connectionListener);
    }

    public void start() {
        synchronized (syncThread) {
            if (runState == STATE.RUNING) {
                return;
            }
            try {
                startTimeMs = 0;
                rtmpSender.start(resCoreParameters.rtmpAddr);
                runState = STATE.RUNING;
            } catch (Exception e) {
                LogTools.trace("RESCore,start,failed", e);
            }
        }
    }

    public int getTotalSpeed() {
        return rtmpSender.getTotalSpeed();
    }

    public void stop() {
        synchronized (syncThread) {
            if (runState == STATE.STOPPED) {
                return;
            }
        }
        rtmpSender.stop();
        runState = STATE.STOPPED;
    }

    public void destroy() {
        rtmpSender.destroy();
        rtmpSender = null;
    }

}
