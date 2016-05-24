package me.lake.librestreaming.core;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.nio.ByteBuffer;

import me.lake.librestreaming.model.RESAudioBuff;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.rtmp.RESFlvData;
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.rtmp.RESRtmpSender;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-5-24.
 */
public class RESSoftAudioCore {
    RESCoreParameters resCoreParameters;
    private RESFlvDataCollecter dataCollecter;
    private MediaCodec dstAudioEncoder;
    private MediaFormat dstAudioFormat;
    //AudioBuffs
    //buffers to handle buff from queueAudio
    private RESAudioBuff[] orignAudioBuffs;
    private int lastAudioQueueBuffIndex;
    //buffer to handle buff from orignAudioBuffs
    private RESAudioBuff orignAudioBuff;
    private AudioFilterHandler audioFilterHandler;
    private HandlerThread audioFilterHandlerThread;
    private AudioSenderThread audioSenderThread;

    public RESSoftAudioCore(RESCoreParameters parameters) {
        resCoreParameters = parameters;
    }

    public void queueAudio(byte[] rawAudioFrame) {
        int targetIndex = (lastAudioQueueBuffIndex + 1) % orignAudioBuffs.length;
        if (orignAudioBuffs[targetIndex].isReadyToFill) {
            LogTools.d("queueAudio,accept ,targetIndex" + targetIndex);
            System.arraycopy(rawAudioFrame, 0, orignAudioBuffs[targetIndex].buff, 0, resCoreParameters.audioRecoderBufferSize);
            orignAudioBuffs[targetIndex].isReadyToFill = false;
            lastAudioQueueBuffIndex = targetIndex;
            audioFilterHandler.sendMessage(audioFilterHandler.obtainMessage(AudioFilterHandler.WHAT_INCOMING_BUFF, targetIndex, 0));
        } else {
            LogTools.d("queueAudio,abandon,targetIndex" + targetIndex);
        }
    }

    public boolean prepare(RESConfig resConfig) {
        resCoreParameters.mediacodecAACProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        resCoreParameters.mediacodecAACSampleRate = 44100;
        resCoreParameters.mediacodecAACChannelCount = 1;
        resCoreParameters.mediacodecAACBitRate = 32 * 1024;
        resCoreParameters.mediacodecAACMaxInputSize = 8820;

        dstAudioFormat = new MediaFormat();
        dstAudioEncoder = MediaCodecHelper.createAudioMediaCodec(resCoreParameters, dstAudioFormat);
        if (dstAudioEncoder == null) {
            LogTools.e("create Audio MediaCodec failed");
            return false;
        }
        //audio
        //44100/10=4410,4410*2 = 8820
        int audioQueueNum = resCoreParameters.audioBufferQueueNum;
        int orignAudioBuffSize = resCoreParameters.mediacodecAACSampleRate / 5;
        orignAudioBuffs = new RESAudioBuff[audioQueueNum];
        for (int i = 0; i < audioQueueNum; i++) {
            orignAudioBuffs[i] = new RESAudioBuff(AudioFormat.ENCODING_PCM_16BIT, orignAudioBuffSize);
        }
        orignAudioBuff = new RESAudioBuff(AudioFormat.ENCODING_PCM_16BIT, orignAudioBuffSize);
        return true;
    }

    public void start(RESFlvDataCollecter flvDataCollecter) {
        try {
            dataCollecter = flvDataCollecter;
            for (RESAudioBuff buff : orignAudioBuffs) {
                buff.isReadyToFill = true;
            }
            if (dstAudioEncoder == null) {
                dstAudioEncoder = MediaCodec.createEncoderByType(dstAudioFormat.getString(MediaFormat.KEY_MIME));
            }
            dstAudioEncoder.configure(dstAudioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            dstAudioEncoder.start();
            lastAudioQueueBuffIndex = 0;
            audioFilterHandlerThread = new HandlerThread("audioFilterHandlerThread");
            audioSenderThread = new AudioSenderThread("AudioSenderThread");
            audioFilterHandlerThread.start();
            audioSenderThread.start();
            audioFilterHandler = new AudioFilterHandler(audioFilterHandlerThread.getLooper());
        } catch (Exception e) {
            LogTools.trace("RESSoftAudioCore", e);
        }
    }

    public void stop() {
        dataCollecter = null;
        audioFilterHandler.removeCallbacksAndMessages(null);
        audioFilterHandlerThread.quit();
        try {
            audioFilterHandlerThread.join();
            audioSenderThread.quit();
            audioSenderThread.join();
        } catch (InterruptedException e) {
            LogTools.trace("RESCore", e);
        }
        dstAudioEncoder.stop();
        dstAudioEncoder.release();
        dstAudioEncoder = null;
    }

    public void destroy() {

    }

    private class AudioFilterHandler extends Handler {
        public static final int WHAT_INCOMING_BUFF = 1;

        AudioFilterHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what != WHAT_INCOMING_BUFF) {
                return;
            }
            int targetIndex = msg.arg1;
            long nowTimeMs = System.currentTimeMillis();
            System.arraycopy(orignAudioBuffs[targetIndex].buff, 0,
                    orignAudioBuff.buff, 0, orignAudioBuff.buff.length);
            orignAudioBuffs[targetIndex].isReadyToFill = true;
            //orignAudioBuff is ready
            int eibIndex = dstAudioEncoder.dequeueInputBuffer(-1);
            if (eibIndex >= 0) {
                ByteBuffer dstAudioEncoderIBuffer = dstAudioEncoder.getInputBuffers()[eibIndex];
                dstAudioEncoderIBuffer.position(0);
                dstAudioEncoderIBuffer.put(orignAudioBuff.buff, 0, orignAudioBuff.buff.length);
                dstAudioEncoder.queueInputBuffer(eibIndex, 0, orignAudioBuff.buff.length, nowTimeMs * 1000, 0);
            } else {
                LogTools.d("dstAudioEncoder.dequeueInputBuffer(-1)<0");
            }
            LogTools.d("AudioFilterHandler,ProcessTime:" + (System.currentTimeMillis() - nowTimeMs));
        }
    }

    private class AudioSenderThread extends Thread {
        private static final long WAIT_TIME = 5000;//1ms;
        private MediaCodec.BufferInfo eInfo;
        private long startTime = 0;

        AudioSenderThread(String name) {
            super(name);
            eInfo = new MediaCodec.BufferInfo();
            startTime = 0;
        }

        private boolean shouldQuit = false;

        void quit() {
            shouldQuit = true;
            this.interrupt();
        }

        @Override
        public void run() {
            while (!shouldQuit) {
                int eobIndex = dstAudioEncoder.dequeueOutputBuffer(eInfo, WAIT_TIME);
                switch (eobIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        LogTools.d("AudioSenderThread,MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED");
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
//                        LogTools.d("AudioSenderThread,MediaCodec.INFO_TRY_AGAIN_LATER");
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        LogTools.d("AudioSenderThread,MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:" +
                                dstAudioEncoder.getOutputFormat().toString());
                        ByteBuffer csd0 = dstAudioEncoder.getOutputFormat().getByteBuffer("csd-0");
                        sendAudioSpecificConfig(0, csd0);
                        break;
                    default:
                        LogTools.d("AudioSenderThread,MediaCode,eobIndex=" + eobIndex);
                        if (startTime == 0) {
                            startTime = eInfo.presentationTimeUs / 1000;
                        }
                        /**
                         * we send audio SpecificConfig already in INFO_OUTPUT_FORMAT_CHANGED
                         * so we ignore MediaCodec.BUFFER_FLAG_CODEC_CONFIG
                         */
                        if (eInfo.flags != MediaCodec.BUFFER_FLAG_CODEC_CONFIG && eInfo.size != 0) {
                            ByteBuffer realData = dstAudioEncoder.getOutputBuffers()[eobIndex];
                            realData.position(eInfo.offset);
                            realData.limit(eInfo.offset + eInfo.size);
                            sendRealData((eInfo.presentationTimeUs / 1000)-startTime, realData);
                        }
                        dstAudioEncoder.releaseOutputBuffer(eobIndex, false);
                        break;
                }
            }
            eInfo = null;
        }

        private void sendAudioSpecificConfig(long tms, ByteBuffer realData) {
            int packetLen = Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH +
                    realData.remaining();
            byte[] finalBuff = new byte[packetLen];
            realData.get(finalBuff, Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH,
                    realData.remaining());
            Packager.FLVPackager.fillFlvAudioTag(finalBuff,
                    0,
                    true);
            RESFlvData resFlvData = new RESFlvData();
            resFlvData.byteBuffer = finalBuff;
            resFlvData.size = finalBuff.length;
            resFlvData.dts = (int) tms;
            resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO;
            dataCollecter.collect(resFlvData, RESRtmpSender.FROM_AUDIO);
        }

        private void sendRealData(long tms, ByteBuffer realData) {
            int packetLen = Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH +
                    realData.remaining();
            byte[] finalBuff = new byte[packetLen];
            realData.get(finalBuff, Packager.FLVPackager.FLV_AUDIO_TAG_LENGTH,
                    realData.remaining());
            Packager.FLVPackager.fillFlvAudioTag(finalBuff,
                    0,
                    false);
            RESFlvData resFlvData = new RESFlvData();
            resFlvData.byteBuffer = finalBuff;
            resFlvData.size = finalBuff.length;
            resFlvData.dts = (int) tms;
            resFlvData.flvTagType = RESFlvData.FLV_RTMP_PACKET_TYPE_AUDIO;
            dataCollecter.collect(resFlvData, RESRtmpSender.FROM_AUDIO);
        }
    }
}
