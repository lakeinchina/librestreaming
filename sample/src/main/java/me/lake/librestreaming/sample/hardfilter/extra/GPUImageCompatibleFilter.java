package me.lake.librestreaming.sample.hardfilter.extra;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;

/**
 * Created by lake on 12/07/16.
 * Librestreaming project.
 */
public class GPUImageCompatibleFilter<T extends GPUImageFilter> extends BaseHardVideoFilter {
    private T innerGPUImageFilter;

    public GPUImageCompatibleFilter(T filter) {
        innerGPUImageFilter = filter;
    }

    public T getGPUImageFilter() {
        return innerGPUImageFilter;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        innerGPUImageFilter.init();
        innerGPUImageFilter.onOutputSizeChanged(VWidth, VHeight);
    }

    @Override
    public void onDraw(int cameraTexture, int targetFrameBuffer, FloatBuffer shapeBuffer, FloatBuffer textrueBuffer) {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, targetFrameBuffer);
        innerGPUImageFilter.onDraw(cameraTexture, shapeBuffer, textrueBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        innerGPUImageFilter.destroy();
    }
}
