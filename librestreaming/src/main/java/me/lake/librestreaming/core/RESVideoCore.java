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

    void updateCamTexture(SurfaceTexture camTex);

    void startPreview(SurfaceTexture surfaceTexture, int visualWidth, int visualHeight);

    void updatePreview(int visualWidth, int visualHeight);

    void stopPreview();

    boolean startStreaming(RESFlvDataCollecter flvDataCollecter);

    boolean stopStreaming();

    boolean destroy();

    void setCurrentCamera(int cameraIndex);

    void takeScreenShot(RESScreenShotListener listener);

    float getDrawFrameRate();
}
