package me.lake.librestreaming.rtmp;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import me.lake.librestreaming.client.CallbackDelivery;
import me.lake.librestreaming.core.RESByteSpeedometer;
import me.lake.librestreaming.core.listener.RESConnectionListener;
import me.lake.librestreaming.model.RESCoreParameters;
import me.lake.librestreaming.tools.LogTools;

/**
 * Created by lake on 16-4-8.
 */
public class RESRtmpSender {
    private static final int TIMEGRANULARITY = 3000;
    public static final int FROM_AUDIO = 8;
    public static final int FROM_VIDEO = 6;
    private WorkHandler workHandler;
    private HandlerThread workHandlerThread;

    public void prepare(RESCoreParameters coreParameters) {
        workHandlerThread = new HandlerThread("RESRtmpSender,workHandlerThread");
        workHandlerThread.start();
        workHandler = new WorkHandler(coreParameters.senderQueueLength,
                new FLvMetaData(coreParameters),
                workHandlerThread.getLooper());
    }

    public void setConnectionListener(RESConnectionListener connectionListener) {
        workHandler.setConnectionListener(connectionListener);
    }

    public void start(String rtmpAddr) {
        workHandler.sendStart(rtmpAddr);
    }

    public void feed(RESFlvData flvData, int type) {
        workHandler.sendFood(flvData, type);
    }

    public void stop() {
        workHandler.sendStop();
    }

    public int getTotalSpeed() {
        if (workHandler != null) {
            return workHandler.getTotalSpeed();
        } else {
            return 0;
        }
    }

    public void destroy() {
        workHandler.removeCallbacksAndMessages(null);
        workHandlerThread.quit();
//        try {
//            workHandlerThread.join();
//        } catch (InterruptedException ignored) {
//        }
    }

    static class WorkHandler extends Handler {
        private final static int MSG_START = 1;
        private final static int MSG_WRITE = 2;
        private final static int MSG_STOP = 3;
        private long jniRtmpPointer = 0;
        private int maxQueueLength;
        private int writeMsgNum = 0;
        private RESByteSpeedometer videoByteSpeedometer = new RESByteSpeedometer(TIMEGRANULARITY);
        private RESByteSpeedometer audioByteSpeedometer = new RESByteSpeedometer(TIMEGRANULARITY);
        private FLvMetaData fLvMetaData;
        private RESConnectionListener connectionListener;
        private final Object syncConnectionListener = new Object();
        private int errorTime=0;

        private enum STATE {
            IDLE,
            RUNNING,
            STOPPED
        }

        private STATE state;

        WorkHandler(int maxQueueLength, FLvMetaData fLvMetaData, Looper looper) {
            super(looper);
            this.maxQueueLength = maxQueueLength;
            this.fLvMetaData = fLvMetaData;
            state = STATE.IDLE;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_START:
                    if (state == STATE.RUNNING) {
                        break;
                    }
                    LogTools.d("RESRtmpSender,WorkHandler,tid=" + Thread.currentThread().getId());
                    jniRtmpPointer = RtmpClient.open((String) msg.obj, true);
                    final int openR = jniRtmpPointer == 0 ? 1 : 0;
                    synchronized (syncConnectionListener) {
                        if (connectionListener != null) {
                            CallbackDelivery.i().post(new Runnable() {
                                @Override
                                public void run() {
                                    connectionListener.onOpenConnectionResult(openR);
                                }
                            });
                        }
                    }
                    if (jniRtmpPointer == 0) {
                        break;
                    } else {
                        byte[] MetaData = fLvMetaData.getMetaData();
                        RtmpClient.write(jniRtmpPointer,
                                MetaData,
                                MetaData.length,
                                RESFlvData.FLV_RTMP_PACKET_TYPE_INFO, 0);
                        state = STATE.RUNNING;
                    }
                    break;
                case MSG_STOP:
                    if (state == STATE.STOPPED ||  jniRtmpPointer==0) {
                        break;
                    }
                    final int closeR = RtmpClient.close(jniRtmpPointer);
                    synchronized (syncConnectionListener) {
                        if (connectionListener != null) {
                            CallbackDelivery.i().post(new Runnable() {
                                @Override
                                public void run() {
                                    connectionListener.onCloseConnectionResult(closeR);
                                }
                            });
                        }
                    }
                    state = STATE.STOPPED;
                    break;
                case MSG_WRITE:
                    --writeMsgNum;
                    if (state != STATE.RUNNING) {
                        break;
                    }
                    RESFlvData flvData = (RESFlvData) msg.obj;
                    final int res = RtmpClient.write(jniRtmpPointer, flvData.byteBuffer, flvData.byteBuffer.length, flvData.flvTagType, flvData.dts);
                    if (res != 0) {
                        errorTime=0;
                        if (flvData.flvTagType == RESFlvData.FLV_RTMP_PACKET_TYPE_VIDEO) {
                            videoByteSpeedometer.gain(flvData.size);
                        } else {
                            audioByteSpeedometer.gain(flvData.size);
                        }
                    } else {
                        ++errorTime;
                        synchronized (syncConnectionListener) {
                            if (connectionListener != null) {
                                CallbackDelivery.i().post(new RESConnectionListener.RESWriteErrorRunable(connectionListener,errorTime));
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }

        public void sendStart(String rtmpAddr) {
            this.removeMessages(MSG_START);
            this.removeMessages(MSG_WRITE);
            this.sendMessage(this.obtainMessage(MSG_START, rtmpAddr));
        }

        public void sendStop() {
            this.removeMessages(MSG_STOP);
            this.removeMessages(MSG_WRITE);
            this.sendEmptyMessage(MSG_STOP);
        }

        public void sendFood(RESFlvData flvData, int type) {
            //LAKETODO optimize
            if (writeMsgNum <= maxQueueLength) {
                this.sendMessage(this.obtainMessage(MSG_WRITE, type, 0, flvData));
                ++writeMsgNum;
            } else {
                if (flvData.isKeyframe()) {
                    this.sendMessage(this.obtainMessage(MSG_WRITE, type, 0, flvData));
                    ++writeMsgNum;
                } else {
                    LogTools.d("senderQueue is full,abandon");
                }
            }
        }

        public void setConnectionListener(RESConnectionListener connectionListener) {
            synchronized (syncConnectionListener) {
                this.connectionListener = connectionListener;
            }
        }

        public int getTotalSpeed() {
            return getVideoSpeed() + getAudioSpeed();
        }

        public int getVideoSpeed() {
            return videoByteSpeedometer.getSpeed();
        }

        public int getAudioSpeed() {
            return audioByteSpeedometer.getSpeed();
        }
    }
}

