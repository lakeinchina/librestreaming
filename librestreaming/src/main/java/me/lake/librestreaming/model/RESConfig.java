package me.lake.librestreaming.model;


import android.hardware.Camera;

/**
 * Created by lake on 16-3-16.
 */
public class RESConfig {
    public static class RenderingMode {
        public static final int NativeWindow = RESCoreParameters.RENDERING_MODE_NATIVE_WINDOW;
        public static final int OpenGLES = RESCoreParameters.RENDERING_MODE_OPENGLES;
    }

    public static class DirectionMode {
        public static final int FLAG_DIRECTION_FLIP_HORIZONTAL = RESCoreParameters.FLAG_DIRECTION_FLIP_HORIZONTAL;
        public static final int FLAG_DIRECTION_FLIP_VERTICAL = RESCoreParameters.FLAG_DIRECTION_FLIP_VERTICAL;
        public static final int FLAG_DIRECTION_ROATATION_0 = RESCoreParameters.FLAG_DIRECTION_ROATATION_0;
        public static final int FLAG_DIRECTION_ROATATION_90 = RESCoreParameters.FLAG_DIRECTION_ROATATION_90;
        public static final int FLAG_DIRECTION_ROATATION_180 = RESCoreParameters.FLAG_DIRECTION_ROATATION_180;
        public static final int FLAG_DIRECTION_ROATATION_270 = RESCoreParameters.FLAG_DIRECTION_ROATATION_270;
    }

    private Size targetVideoSize;
    private int videoBufferQueueNum;
    private int bitRate;
    private String rtmpAddr;
    private int renderingMode;
    private int defaultCamera;
    private int frontCameraDirectionMode;
    private int backCameraDirectionMode;
    private boolean printDetailMsg;

    private RESConfig() {
    }

    public static RESConfig obtain() {
        RESConfig res = new RESConfig();
        res.setRenderingMode(RenderingMode.NativeWindow);
        res.setTargetVideoSize(new Size(1280, 720));
        res.setVideoBufferQueueNum(5);
        res.setBitRate(2000000);
        res.setPrintDetailMsg(false);
        res.setDefaultCamera(Camera.CameraInfo.CAMERA_FACING_BACK);
        res.setBackCameraDirectionMode(DirectionMode.FLAG_DIRECTION_ROATATION_0);
        res.setFrontCameraDirectionMode(DirectionMode.FLAG_DIRECTION_ROATATION_0);
        return res;
    }

    public int getDefaultCamera() {
        return defaultCamera;
    }

    public void setDefaultCamera(int defaultCamera) {
        this.defaultCamera = defaultCamera;
    }

    public int getFrontCameraDirectionMode() {
        return frontCameraDirectionMode;
    }

    public void setFrontCameraDirectionMode(int frontCameraDirectionMode) {
        this.frontCameraDirectionMode = frontCameraDirectionMode;
    }

    public int getBackCameraDirectionMode() {
        return backCameraDirectionMode;
    }

    public void setBackCameraDirectionMode(int backCameraDirectionMode) {
        this.backCameraDirectionMode = backCameraDirectionMode;
    }

    public int getRenderingMode() {
        return renderingMode;
    }

    public void setRenderingMode(int renderingMode) {
        this.renderingMode = renderingMode;
    }

    public String getRtmpAddr() {
        return rtmpAddr;
    }

    public void setRtmpAddr(String rtmpAddr) {
        this.rtmpAddr = rtmpAddr;
    }

    public boolean isPrintDetailMsg() {
        return printDetailMsg;
    }

    public void setPrintDetailMsg(boolean printDetailMsg) {
        this.printDetailMsg = printDetailMsg;
    }

    public void setTargetVideoSize(Size videoSize) {
        targetVideoSize = videoSize;
    }

    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    public void setVideoBufferQueueNum(int num) {
        videoBufferQueueNum = num;
    }


    public Size getTargetVideoSize() {
        return targetVideoSize;
    }

    public int getVideoBufferQueueNum() {
        return videoBufferQueueNum;
    }

    public int getBitRate() {
        return bitRate;
    }
}
