package me.lake.librestreaming.core.listener;

/**
 * Created by lake on 22/09/16.
 * Librestreaming project.
 */
public interface RESVideoChangeListener {
    void onVideoSizeChanged(int width, int height);

    class RESVideoChangeRunable implements Runnable {
        RESVideoChangeListener videoChangeListener;
        int w, h;

        public RESVideoChangeRunable(RESVideoChangeListener videoChangeListener, int w, int h) {
            this.videoChangeListener = videoChangeListener;
            this.w = w;
            this.h = h;
        }

        @Override
        public void run() {
            if (videoChangeListener != null) {
                videoChangeListener.onVideoSizeChanged(w, h);
            }
        }
    }
}