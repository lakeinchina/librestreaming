package me.lake.librestreaming.client;

import android.graphics.SurfaceTexture;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;

/**
 * Created by lake on 16-5-24.
 */
public interface RESVideoClient {
    boolean prepare(RESConfig resConfig);

    boolean start(RESFlvDataCollecter flvDataCollecter);

    boolean stop();

    boolean destroy();

    void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight);

    void updatePreview(int visualWidth, int visualHeight);

    void destroyPreview();

    boolean swapCamera();

    boolean toggleFlashLight();

    boolean setZoomByPercent(float targetPercent);

    BaseVideoFilter acquireVideoFilter();

    void releaseVideoFilter();

    void setVideoFilter(BaseVideoFilter baseVideoFilter);

    void takeScreenShot(RESScreenShotListener listener);
}
