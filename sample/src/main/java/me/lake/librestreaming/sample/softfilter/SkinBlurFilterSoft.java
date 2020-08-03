package me.lake.librestreaming.sample.softfilter;

import android.content.Context;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;

import me.lake.librestreaming.filter.softvideofilter.BaseSoftVideoFilter;
import me.lake.librestreaming.sample.ScriptC_skinblur;

/**
 * Created by lake on 16-4-13.
 */
public class SkinBlurFilterSoft extends BaseSoftVideoFilter {
    RenderScript mRS;
    ScriptC_skinblur sSkinblur;
    Allocation mInAllocation;
    Allocation mOutAllocation;
    int delta = 18;
    int radius = (int) (6 + 1.0);
    int numrad = (int) (radius + 1.0);
    Context context;

    public SkinBlurFilterSoft(Context context) {
        this.context = context;

    }
    public void setRadius(int radius)
    {
//        this.radius = (int) (radius + 1.0);
//        this.numrad = (int) (this.radius + 1.0);
    }


    @Override
    public void onInit(int VWidth, int VHeight) {
        super.onInit(VWidth, VHeight);

        mRS = RenderScript.create(context);
        sSkinblur = new ScriptC_skinblur(mRS);
        Type.Builder yuvType = new Type.Builder(mRS, Element.U8(mRS)).setX(SIZE_WIDTH * SIZE_HEIGHT * 3 / 2);

        mInAllocation = Allocation.createTyped(mRS, yuvType.create(), Allocation.USAGE_SHARED | Allocation.USAGE_SCRIPT | Allocation.USAGE_GRAPHICS_TEXTURE);
        mOutAllocation = Allocation.createTyped(mRS, yuvType.create(), Allocation.USAGE_SHARED | Allocation.USAGE_SCRIPT | Allocation.USAGE_GRAPHICS_TEXTURE);

    }

    @Override
    public boolean onFrame(byte[] orignBuff, byte[] targetBuff, long presentationTimeMs, int sequenceNum) {
        mInAllocation.copyFrom(orignBuff);
        sSkinblur.set_gIn(mInAllocation);
        sSkinblur.set_width(SIZE_WIDTH);
        sSkinblur.set_height(SIZE_HEIGHT);
        sSkinblur.set_snumrad(numrad);
        sSkinblur.set_radius(radius);
        sSkinblur.set_maxdelta(delta);
        sSkinblur.set_size(SIZE_Y);
        sSkinblur.forEach_blur(mInAllocation, mOutAllocation);
        mOutAllocation.copyTo(targetBuff);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mInAllocation.destroy();
        mOutAllocation.destroy();
        sSkinblur.destroy();
        mRS.destroy();
    }
}