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
import me.lake.librestreaming.rtmp.RESFlvDataCollecter;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-5-24.
 */
public class RESSoftAudioCore {
    RESCoreParameters resCoreParameters;
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
            audioSenderThread = new AudioSenderThread("AudioSenderThread",dstAudioEncoder,flvDataCollecter);
            audioFilterHandlerThread.start();
            audioSenderThread.start();
            audioFilterHandler = new AudioFilterHandler(audioFilterHandlerThread.getLooper());
        } catch (Exception e) {
            LogTools.trace("RESSoftAudioCore", e);
        }
    }

    public void stop() {
        audioFilterHandler.removeCallbacksAndMessages(null);
        audioFilterHandlerThread.quit();
        try {
            audioFilterHandlerThread.join();
            audioSenderThread.quit();
            audioSenderThread.join();
        } catch (InterruptedException e) {
            LogTools.trace("RESSoftAudioCore", e);
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
}
