package me.lake.librestreaming.sample.softfilter;

import android.graphics.Bitmap;

import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;

/**
 * Created by lake on 16-4-1.
 */
public class IconFilterSoft extends BaseSoftVideoFilter {
    byte[] imageByte;
    int iconYSize;
    private int iconW;
    private int iconH;
    private int xPos;

    public void setXPos(int x) {
        xPos = x;
    }

    public IconFilterSoft(Bitmap bitmap) {
        iconW = bitmap.getWidth();
        iconH = bitmap.getHeight();
        imageByte = new byte[iconW * iconH * 3 / 2];
        iconYSize = iconH * iconW;
        int[] tem = new int[iconW * iconH];
        bitmap.getPixels(tem, 0, iconW, 0, 0, iconW, iconH);
        for (int y = 0; y < iconH; y++) {
            for (int x = 0; x < iconW; x++) {
                int color = tem[y * iconW + x];
                if (((color & 0xFF000000) >> 24) == 0) {
                    imageByte[y * iconW + x] = 0;
                    continue;
                }
                int R = (color & 0xff0000) >> 16;
                int G = (color & 0xff00) >> 8;
                int B = (color & 0xff);

                int Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                int U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                int V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;
                Y = ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                U = ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                V = ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                imageByte[y * iconW + x] = (byte) Y;
                if (x % 2 == 0) {
                    imageByte[(y / 2) * iconW + x + iconYSize] = (byte) V;
                    imageByte[(y / 2) * iconW + x + iconYSize + 1] = (byte) U;
                }
            }
        }
    }

    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        System.arraycopy(orignBuff, 0, targetBuff, 0, SIZE_TOTAL);
        for (int y = 0; y < iconH; y++) {
            for (int x = 0; x < iconW; x++) {
                if (imageByte[y * iconW + x] != 0) {
                    targetBuff[xPos + (y + 50) * SIZE_WIDTH + x] = imageByte[y * iconW + x];
                    if ((x + xPos) % 2 == 0) {
                        if (x % 2 == 0) {
                            targetBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos] = imageByte[iconYSize + (y / 2) * iconW + x];
                            targetBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos + 1] = imageByte[iconYSize + (y / 2) * iconW + x + 1];
                        } else {
                            targetBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos] = imageByte[iconYSize + (y / 2) * iconW + x+1];
                            targetBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos + 1] = imageByte[iconYSize + (y / 2) * iconW + x];
                        }
                    }
                }
            }
        }
        return true;
    }
}
