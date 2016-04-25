package me.lake.librestreaming.core.listener;

/**
 * Created by lake on 16-4-11.
 */
public interface RESConnectionListener {
    void onOpenConnectionResult(int result);

    void onWriteError(int error);

    void onCloseConnectionResult(int result);
}