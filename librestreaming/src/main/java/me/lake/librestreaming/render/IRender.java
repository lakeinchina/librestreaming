package me.lake.librestreaming.render;

import android.graphics.SurfaceTexture;

/**
 * Created by lake on 16-5-3.
 */
public interface IRender {
    void create(SurfaceTexture visualSurfaceTexture, int pixelFormat, int pixelWidth, int pixelHeight, int visualWidth, int visualHeight);

    void update(int visualWidth, int visualHeight);

    void rendering(byte[] pixel);

    void destroy(boolean releaseTexture);
}
