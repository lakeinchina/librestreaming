package me.lake.librestreaming.rtmp;

import java.util.Comparator;

/**
 * Created by lake on 16-3-16.
 */
public class RESFlvData {

    public final static int FLV_RTMP_PACKET_TYPE_VIDEO = 9;
    public final static int FLV_RTMP_PACKET_TYPE_AUDIO = 8;
    public final static int FLV_RTMP_PACKET_TYPE_INFO = 18;
    public final static int NALU_TYPE_IDR = 5;

    public int dts;//解码时间戳

    public byte[] byteBuffer; //数据

    public int size; //字节长度

    public int flvTagType; //视频和音频的分类

    public int videoFrameType;

    public boolean isKeyframe() {
        return videoFrameType == NALU_TYPE_IDR;
    }

    public static class RESFlvDataDtsComparator implements Comparator<RESFlvData> {
        @Override
        public int compare(RESFlvData lhs, RESFlvData rhs) {
            if (lhs.dts < rhs.dts) {
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        public boolean equals(Object object) {
            return false;
        }
    }

}
