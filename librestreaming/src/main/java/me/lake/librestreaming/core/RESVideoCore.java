package me.lake.librestreaming.core;

import android.graphics.SurfaceTexture;

import me.lake.librestreaming.core.listener.RESScreenShotListener;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;

/**
 * Created by lake on 16-5-25.
 */
public interface RESVideoCore {
    int OVERWATCH_TEXTURE_ID = 10;
    boolean prepare(RESConfig resConfig);

    boolean startPreview(SurfaceTexture camTex);

    boolean startStreaming(RESFlvDataCollecter flvDataCollecter);

    void updateCamTexture(SurfaceTexture camTex);

    boolean stopStreaming();

    boolean stopPreview();

    boolean destroy();

    void setCurrentCamera(int cameraIndex);

    void createPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight);

    void updatePreview(int visualWidth, int visualHeight);

    void destroyPreview();

    void takeScreenShot(RESScreenShotListener listener);

    float getDrawFrameRate();
}
