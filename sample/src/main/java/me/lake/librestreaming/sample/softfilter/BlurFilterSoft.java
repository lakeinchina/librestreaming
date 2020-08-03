package me.lake.librestreaming.sample.softfilter;

import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.Type;

import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;

/**
 * Created by lake on 16-4-12.
 */
public class BlurFilterSoft extends BaseSoftVideoFilter {
    ScriptIntrinsicBlur intrinsicBlur;
    RenderScript mRS;
    Allocation mInAllocation;
    Allocation mOutAllocations;
    int radius;
    byte[] i, o;

    public BlurFilterSoft(Context context) {
        mRS = RenderScript.create(context);
        intrinsicBlur = ScriptIntrinsicBlur.create(mRS, Element.U8(mRS));
        radius = 25;
        intrinsicBlur.setRadius(radius);
    }

    public void setRadius(int radius) {
        this.radius = radius>=1?(radius<=25?radius:25):1;
    }

    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);
        Type.Builder yuvType = new Type.Builder(mRS, Element.U8(mRS)).setX(SIZE_WIDTH).setY(SIZE_HEIGHT);
        mInAllocation = Allocation.createTyped(mRS, yuvType.create(),Allocation.USAGE_SCRIPT);
        mOutAllocations = Allocation.createTyped(mRS, yuvType.create(),Allocation.USAGE_SCRIPT);
        i = new byte[SIZE_Y];
        o = new byte[SIZE_Y];
    }

    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        System.arraycopy(orignBuff, 0, i, 0, SIZE_Y);
        mInAllocation.copyFrom(i);
        intrinsicBlur.setRadius(radius);
        intrinsicBlur.setInput(mInAllocation);
        intrinsicBlur.forEach(mOutAllocations);
        mOutAllocations.copyTo(o);
        System.arraycopy(o, 0, targetBuff, 0, SIZE_Y);
        System.arraycopy(orignBuff, SIZE_Y, targetBuff, SIZE_Y, SIZE_UV);
        return true;
    }
}
