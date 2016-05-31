package me.lake.librestreaming.sample.softfilter;

import android.graphics.Bitmap;

import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;

/**
 * Created by lake on 16-4-25.
 */
public class AnimationFilterSoft extends BaseSoftVideoFilter {

    byte[][] imageByte;
    int aniNum;
    int iconYSize;
    private int iconW;
    private int iconH;
    private int xPos;
    private int lastNum;

    public void setXPos(int x) {
        xPos = x;
    }

    public AnimationFilterSoft(Bitmap[] bitmaps) {
        aniNum = bitmaps.length;
        imageByte = new byte[aniNum][];
        iconW = bitmaps[0].getWidth();
        iconH = bitmaps[0].getHeight();
        iconYSize = iconH * iconW;
        for (int i = 0; i < bitmaps.length; ++i) {
            imageByte[i] = new byte[iconW * iconH * 3 / 2];
            int[] tem = new int[iconW * iconH];
            bitmaps[i].getPixels(tem, 0, iconW, 0, 0, iconW, iconH);
            for (int y = 0; y < iconH; y++) {
                for (int x = 0; x < iconW; x++) {
                    int color = tem[y * iconW + x];
                    if (((color & 0xFF000000) >> 24) == 0) {
                        imageByte[i][y * iconW + x] = 0;
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
                    imageByte[i][y * iconW + x] = (byte) Y;
                    if (x % 2 == 0) {
                        imageByte[i][(y / 2) * iconW + x + iconYSize] = (byte) V;
                        imageByte[i][(y / 2) * iconW + x + iconYSize + 1] = (byte) U;
                    }
                }
            }
        }
    }

    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        int index = lastNum % aniNum;
        if (sequenceNum % 3 == 0) {
            ++lastNum;
        }
        try {
            byte[] image = imageByte[index];
            for (int y = 0; y < iconH; y++) {
                for (int x = 0; x < iconW; x++) {
                    if (image[y * iconW + x] != 0) {
                        orignBuff[xPos + (y + 50) * SIZE_WIDTH + x] = image[y * iconW + x];
                        if ((x + xPos) % 2 == 0) {
                            if (x % 2 == 0) {
                                orignBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos] = image[iconYSize + (y / 2) * iconW + x];
                                orignBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos + 1] = image[iconYSize + (y / 2) * iconW + x + 1];
                            } else {
                                orignBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos] = image[iconYSize + (y / 2) * iconW + x + 1];
                                orignBuff[SIZE_Y + (y / 2 + 25) * SIZE_WIDTH + x + xPos + 1] = image[iconYSize + (y / 2) * iconW + x];
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {

        }

        return false;
    }
}
