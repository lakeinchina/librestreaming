package me.lake.librestreaming.rtmp;


/**
 * Created by hansentian on 11/23/15.
 */
public class SenderStatData {
    public static final int LEVEL1_QUEUE_SIZE = 200;
    public static final int LEVEL2_QUEUE_SIZE = 150;
    public static final int MIN_QUEUE_BUFFER = 1;

    public volatile int frame_video = 0;
    public volatile int frame_audio = 0;
    public volatile int video_byte_count = 0;
    public volatile int audio_byte_count = 0;

    public int lastTimeSendByteCount = 0;

    public void add(RESFlvData data) {
        if (data == null) {
            return;
        }
        if (data.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO) {
            frame_video++;
            video_byte_count += data.size;
        } else if (data.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO) {
            frame_audio++;
            audio_byte_count += data.size;
        }
    }

    public void remove(RESFlvData data) {
        if (data == null) {
            return;
        }
        if (data.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO) {
            frame_video--;
            video_byte_count -= data.size;
        } else if (data.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO) {
            frame_audio--;
            audio_byte_count -= data.size;
        }
    }

    public int getLastTimeSendByteCount() {
        int lastSend = lastTimeSendByteCount;
        lastTimeSendByteCount = 0;
        return lastSend;

    }

    public void clear() {
        frame_video = 0;
        frame_audio = 0;
        video_byte_count = 0;
        audio_byte_count = 0;
    }
}
