package me.lake.librestreaming.core;

import android.graphics.SurfaceTexture;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;

/**
 * Created by lake on 16-5-24.
 */
public class RESHardVideoCore implements RESVideoCore {
    RESCoreParameters resCoreParameters;

    public RESHardVideoCore(RESCoreParameters parameters) {
        resCoreParameters = parameters;
    }

    public void onFrameAvailable() {

    }

    @Override
    public boolean prepare(RESConfig resConfig) {
        return false;
    }

    @Override
    public boolean start(RESFlvDataCollecter flvDataCollecter,SurfaceTexture camTex) {
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean destroy() {
        return false;
    }

    @Override
    public void setCurrentCamera(int cameraIndex) {

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
}
