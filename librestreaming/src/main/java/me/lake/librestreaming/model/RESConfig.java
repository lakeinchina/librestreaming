package me.lake.librestreaming.model;


/**
 * Created by lake on 16-3-16.
 */
public class RESConfig {

    private Size targetVideoSize;
    private int videoBufferQueueNum;
    private int bitRate;
    private String rtmpAddr;
    private boolean printDetailMsg;

    private RESConfig() {
    }

    public static RESConfig obtain() {
        RESConfig res = new RESConfig();
        res.setTargetVideoSize(new Size(1280, 720));
        res.setVideoBufferQueueNum(5);
        res.setBitRate(2000000);
        res.setPrintDetailMsg(false);
        return res;
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
