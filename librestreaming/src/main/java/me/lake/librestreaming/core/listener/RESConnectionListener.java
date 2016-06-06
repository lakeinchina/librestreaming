package me.lake.librestreaming.core.listener;

import android.graphics.Bitmap;

/**
 * Created by lake on 16-4-11.
 */
public interface RESConnectionListener {
    void onOpenConnectionResult(int result);

    void onWriteError(int error);

    void onCloseConnectionResult(int result);

    class RESWriteErrorRunable implements Runnable {
        RESConnectionListener connectionListener;
        int errorTime;

        public RESWriteErrorRunable(RESConnectionListener connectionListener, int errorTime) {
            this.connectionListener = connectionListener;
            this.errorTime = errorTime;
        }

        @Override
        public void run() {
            if (connectionListener != null) {
                connectionListener.onWriteError(errorTime);
            }
        }
    }
}