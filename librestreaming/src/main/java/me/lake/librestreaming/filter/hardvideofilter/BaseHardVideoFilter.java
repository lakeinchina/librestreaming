package me.lake.librestreaming.filter.hardvideofilter;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import me.lake.librestreaming.core.GLHelper;

/**
 * Created by lake on 16-5-31.
 */
public class BaseHardVideoFilter {
    protected int SIZE_WIDTH;
    protected int SIZE_HEIGHT;
    protected ShortBuffer drawIndecesBuffer;

    public void onInit(int VWidth, int VHeight) {
        SIZE_WIDTH = VWidth;
        SIZE_HEIGHT = VHeight;
        drawIndecesBuffer = GLHelper.getDrawIndecesBuffer();
        GLES20.glEnable(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
    }

    public void onDraw(final int cameraTexture, final FloatBuffer shapeBuffer, final FloatBuffer textrueBuffer) {
    }

    public void onDestroy() {

    }
}
