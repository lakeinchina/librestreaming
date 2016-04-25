package me.lake.librestreaming.core;

import android.view.Surface;

/**
 * Created by lake on 16-4-5.
 */
@SuppressWarnings("all")
public class ColorHelper {
    static {
        System.loadLibrary("restreaming");
    }

    static public native void NV21TOYUV420SP(byte[] src, byte[] dst, int YSize);

    static public native void NV21TOYUV420P(byte[] src, byte[] dst, int YSize);

    static public native void YUV420SPTOYUV420P(byte[] src, byte[] dst, int YSize);

    static public native void NV21TOARGB(byte[] src, int[] dst, int width,int height);


    static public native void renderingSurface(Surface surface, byte[] pixel, int width, int height, int size);
}
