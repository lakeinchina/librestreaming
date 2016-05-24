package me.lake.librestreaming.client;

import android.graphics.SurfaceTexture;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;

/**
 * Created by lake on 16-5-24.
 */
public class RESHardVideoClient implements RESVideoClient {
    RESCoreParameters resCoreParameters;

    public RESHardVideoClient(RESCoreParameters parameters) {
        resCoreParameters = parameters;
    }

    @Override
    public boolean prepare(RESConfig resConfig) {
        return false;
    }

    @Override
    public boolean start(RESFlvDataCollecter flvDataCollecter) {
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
    public void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight) {

    }

    @Override
    public void updatePreview(int visualWidth, int visualHeight) {

    }

    @Override
    public void destroyPreview() {

    }

    @Override
    public boolean swapCamera() {
        return false;
    }

    @Override
    public boolean toggleFlashLight() {
        return false;
    }

    @Override
    public boolean setZoomByPercent(float targetPercent) {
        return false;
    }

    @Override
    public BaseVideoFilter acquireVideoFilter() {
        return null;
    }

    @Override
    public void releaseVideoFilter() {

    }

    @Override
    public void setVideoFilter(BaseVideoFilter baseVideoFilter) {

    }

    @Override
    public void takeScreenShot(RESScreenShotListener listener) {

    }
}
