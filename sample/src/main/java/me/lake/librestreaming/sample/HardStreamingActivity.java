package me.lake.librestreaming.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import jp.co.cyberagent.android.gpuimage.GPUImage3x3ConvolutionFilter;
import jp.co.cyberagent.android.gpuimage.GPUImage3x3TextureSamplingFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageCGAColorspaceFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageColorInvertFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageCrosshatchFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageGrayscaleFilter;
import jp.co.cyberagent.android.gpuimage.GPUImagePixelationFilter;
import me.lake.librestreaming.filter.hardvideofilter.BaseHardVideoFilter;
import me.lake.librestreaming.filter.hardvideofilter.HardVideoGroupFilter;
import me.lake.librestreaming.filter.hardvideofilter.OriginalHardVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.sample.hardfilter.ColorMixHardFilter;
import me.lake.librestreaming.sample.hardfilter.DifferenceBlendFilterHard;
import me.lake.librestreaming.sample.hardfilter.FishEyeFilterHard;
import me.lake.librestreaming.sample.hardfilter.GaussianBlurHardFilter;
import me.lake.librestreaming.sample.hardfilter.IconHardFilter;
import me.lake.librestreaming.sample.hardfilter.SeaScapeFilter;
import me.lake.librestreaming.sample.hardfilter.SkinBlurHardVideoFilter;
import me.lake.librestreaming.sample.hardfilter.SobelEdgeDetectionHardVideoFilter;
import me.lake.librestreaming.sample.hardfilter.TextHardFilter;
import me.lake.librestreaming.sample.hardfilter.TimeStampHardFilter;
import me.lake.librestreaming.sample.hardfilter.TowInputFilterHard;
import me.lake.librestreaming.sample.hardfilter.ViewHardFilter;
import me.lake.librestreaming.sample.hardfilter.WhiteningHardVideoFilter;
import me.lake.librestreaming.sample.hardfilter.extra.GPUImageCompatibleFilter;
import me.lake.librestreaming.sample.ui.FakeView;

/**
 * Created by lake on 16-5-31.
 */
public class HardStreamingActivity extends BaseStreamingActivity {
    protected FilterAdapter filterAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        filtermode = RESConfig.FilterMode.HARD;
        super.onCreate(savedInstanceState);
        /**
         * filters just for demo
         */
        ArrayList<FilterItem> filterItems = new ArrayList<>();
        filterItems.add(new FilterItem("NoFilter", null));
        filterItems.add(new FilterItem("DoNothing", new OriginalHardVideoFilter(null, null)));
        filterItems.add(new FilterItem("ViewRending", new ViewHardFilter(new FakeView(this))));
        filterItems.add(new FilterItem("FishEye", new FishEyeFilterHard()));
        filterItems.add(new FilterItem("SkinBlur", new SkinBlurHardVideoFilter(2)));
        filterItems.add(new FilterItem("Whitening", new WhiteningHardVideoFilter()));
        filterItems.add(new FilterItem("ColorMix", new ColorMixHardFilter(0.98f, 0.72f, 0.82f, 0.3f)));
        LinkedList<BaseHardVideoFilter> filters = new LinkedList<>();
        filters.add(new SkinBlurHardVideoFilter(2));
        filters.add(new WhiteningHardVideoFilter());
        filterItems.add(new FilterItem("FacialUp", new HardVideoGroupFilter(filters)));
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        filterItems.add(new FilterItem("NormalBlend", new TowInputFilterHard(null, null, bitmap)));
        filters = new LinkedList<>();
        filters.add(new SobelEdgeDetectionHardVideoFilter());
        filters.add(new FishEyeFilterHard());
        filterItems.add(new FilterItem("GroupFilter", new HardVideoGroupFilter(filters)));
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        filterItems.add(new FilterItem("DifferenceBlend", new DifferenceBlendFilterHard(bitmap)));
        filterItems.add(new FilterItem("SobelEdgeDetection", new SobelEdgeDetectionHardVideoFilter()));
        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        filterItems.add(new FilterItem("Icon",new IconHardFilter(bitmap,new Rect(100,100,200,200))));
        filterItems.add(new FilterItem("SeaScape", new SeaScapeFilter()));
        filterItems.add(new FilterItem("SimpleText", new TextHardFilter("タイムマシン", Color.argb(127, 255, 0, 0), 35)));
        CharSequence htmlText = Html.fromHtml("<font color=\"#7FFF0000\">红色字体红色</font><br/><b>字体加粗</b><br/><s>删除字体</s><i>斜体字体</i>");
        TextHardFilter htmlTextHardFilter = new TextHardFilter(htmlText);
        htmlTextHardFilter.setPostion(TextHardFilter.Gravity.RIGHT | TextHardFilter.Gravity.TOP, 50, 50);
        filterItems.add(new FilterItem("HtmlText", htmlTextHardFilter));
        TextHardFilter moreText = new TextHardFilter("Fou ki ra hyear presia reen\n" +
                "Was zweie ra na stel yorra zuieg manaf\n" +
                "Ma zweie ra irs manaf chyet oz omnis\n" +
                "en na cyurio re chyet\n" +
                "Was touwaka gaya presia accrroad ieeya\n" +
                "Nn num gagis knawa na lequera walasye\n" +
                "Was quel gagis presia accrroad ieeya whou wearequewie fogabe",Color.RED,30);
        moreText.setPostion(TextHardFilter.Gravity.BOTTOM,50,50);
        filterItems.add(new FilterItem("MoreText", moreText));
        TimeStampHardFilter timeStampHardFilter = new TimeStampHardFilter(null,Color.RED,30);
        timeStampHardFilter.setPostion(TextHardFilter.Gravity.BOTTOM|TextHardFilter.Gravity.RIGHT,30,30);
        filterItems.add(new FilterItem("TimeStamp", timeStampHardFilter));
        TimeStampHardFilter timeStampHardFilter2 = new TimeStampHardFilter(null,Color.TRANSPARENT,30);
        timeStampHardFilter2.setPostion(TextHardFilter.Gravity.BOTTOM | TextHardFilter.Gravity.RIGHT, 30, 30);
        filterItems.add(new FilterItem("invertText", timeStampHardFilter2));
        filterItems.add(new FilterItem("GaussianBlur",new GaussianBlurHardFilter(20)));
        filterItems.add(new FilterItem("gpuimage:Invert", new GPUImageCompatibleFilter<>(new GPUImageColorInvertFilter())));
        filterItems.add(new FilterItem("gpuimage:Pixelation", new GPUImageCompatibleFilter<>(new GPUImagePixelationFilter())));
        GPUImage3x3ConvolutionFilter tmp = new GPUImage3x3ConvolutionFilter();
        tmp.setConvolutionKernel(new float[]{
                -1.0f, 0.0f, 1.0f,
                -2.0f, 0.0f, 2.0f,
                -1.0f, 0.0f, 1.0f
        });
        filterItems.add(new FilterItem("gpuimage:3x3Convolution", new GPUImageCompatibleFilter<>(tmp)));
        LinkedList<BaseHardVideoFilter> sketchfilters = new LinkedList<>();
        sketchfilters.add(new GPUImageCompatibleFilter<>(new GPUImageGrayscaleFilter()));
        sketchfilters.add(new GPUImageCompatibleFilter<>(new GPUImage3x3TextureSamplingFilter(SKETCH_FRAGMENT_SHADER)));
        filterItems.add(new FilterItem("gpuimage:SketchGroup", new HardVideoGroupFilter(sketchfilters)));
        filterItems.add(new FilterItem("gpuimage:CGAColor", new GPUImageCompatibleFilter<>(new GPUImageCGAColorspaceFilter())));
        filterItems.add(new FilterItem("gpuimage:Crosshatch", new GPUImageCompatibleFilter<>(new GPUImageCrosshatchFilter())));
        filterAdapter = new FilterAdapter();
        filterAdapter.updateFilters(filterItems);
        lv_filter.setAdapter(filterAdapter);
        lv_filter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (filterAdapter.selectItem(position)) {
                    //changeFilter
                    BaseHardVideoFilter filter = ((FilterItem) filterAdapter.getItem(position)).filter;
                    resClient.setHardVideoFilter(filter);
                }
            }
        });
        sb_attr.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                BaseHardVideoFilter filter = resClient.acquireHardVideoFilter();
                if (filter != null) {
                    if (filter instanceof GPUImageCompatibleFilter) {
                        GPUImageFilter gpufilter = ((GPUImageCompatibleFilter) filter).getGPUImageFilter();
                        if (gpufilter instanceof GPUImagePixelationFilter) {
                            ((GPUImagePixelationFilter) gpufilter).setPixel(progress / 5.0f);
                        }
                        if (gpufilter instanceof GPUImageCrosshatchFilter) {
                            ((GPUImageCrosshatchFilter) gpufilter).setCrossHatchSpacing(range(progress, 0.0f, 0.06f));
                            ((GPUImageCrosshatchFilter) gpufilter).setLineWidth(range(progress, 0.0f, 0.006f));
                        }
                    }
                    if (filter instanceof ColorMixHardFilter) {
                        ((ColorMixHardFilter)filter).setMixColor((float)(0.5+0.5*Math.sin(Math.PI*5*(progress/100.0))),(float)(0.5+0.5*Math.sin(Math.PI*2*(progress/100.0))),(float)(0.5+0.5*Math.sin(Math.PI*7*(progress/100.0))),0.3f);
                    }
                    if (filter instanceof IconHardFilter) {
                        ((IconHardFilter) filter).updateIcon(null, new Rect(progress*2+100, 100, progress*2+200, 200));
                    }
                }
                resClient.releaseHardVideoFilter();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    class FilterItem {
        String name;
        BaseHardVideoFilter filter;

        public FilterItem(String name, BaseHardVideoFilter filter) {
            this.name = name;
            this.filter = filter;
        }
    }

    class FilterAdapter extends BaseAdapter {
        private List<FilterItem> filters;
        private int selectIndex = 0;

        FilterAdapter() {
            filters = new ArrayList<>(0);
        }

        public boolean selectItem(int index) {
            if (selectIndex == index) {
                return false;
            }
            selectIndex = index;
            notifyDataSetChanged();
            return true;
        }

        public void updateFilters(List<FilterItem> filters) {
            this.filters = filters == null ? new ArrayList<FilterItem>(0) : filters;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return filters.size();
        }

        @Override
        public Object getItem(int position) {
            return filters.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(HardStreamingActivity.this).inflate(R.layout.item_filter, parent, false);
                convertView.setTag(new ViewHolder(convertView));
            }
            if (selectIndex == position) {
                ((ViewHolder) convertView.getTag()).iv_star.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                ((ViewHolder) convertView.getTag()).iv_star.setImageResource(android.R.drawable.btn_star_big_off);
            }

            ((ViewHolder) convertView.getTag()).tv_name.setText(filters.get(position).name);
            return convertView;
        }

        class ViewHolder {
            TextView tv_name;
            ImageView iv_star;

            public ViewHolder(View v) {
                this.tv_name = (TextView) v.findViewById(R.id.tv_name);
                this.iv_star = (ImageView) v.findViewById(R.id.iv_star);
            }
        }
    }
    //========GPUImage suff==============
    protected float range(final int percentage, final float start, final float end) {
        return (end - start) * percentage / 100.0f + start;
    }

    protected int range(final int percentage, final int start, final int end) {
        return (end - start) * percentage / 100 + start;
    }
    public static final String SKETCH_FRAGMENT_SHADER = "" +
            "precision highp float;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "varying vec2 leftTextureCoordinate;\n" +
            "varying vec2 rightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 topTextureCoordinate;\n" +
            "varying vec2 topLeftTextureCoordinate;\n" +
            "varying vec2 topRightTextureCoordinate;\n" +
            "\n" +
            "varying vec2 bottomTextureCoordinate;\n" +
            "varying vec2 bottomLeftTextureCoordinate;\n" +
            "varying vec2 bottomRightTextureCoordinate;\n" +
            "\n" +
            "uniform sampler2D inputImageTexture;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "float bottomLeftIntensity = texture2D(inputImageTexture, bottomLeftTextureCoordinate).r;\n" +
            "float topRightIntensity = texture2D(inputImageTexture, topRightTextureCoordinate).r;\n" +
            "float topLeftIntensity = texture2D(inputImageTexture, topLeftTextureCoordinate).r;\n" +
            "float bottomRightIntensity = texture2D(inputImageTexture, bottomRightTextureCoordinate).r;\n" +
            "float leftIntensity = texture2D(inputImageTexture, leftTextureCoordinate).r;\n" +
            "float rightIntensity = texture2D(inputImageTexture, rightTextureCoordinate).r;\n" +
            "float bottomIntensity = texture2D(inputImageTexture, bottomTextureCoordinate).r;\n" +
            "float topIntensity = texture2D(inputImageTexture, topTextureCoordinate).r;\n" +
            "float h = -topLeftIntensity - 2.0 * topIntensity - topRightIntensity + bottomLeftIntensity + 2.0 * bottomIntensity + bottomRightIntensity;\n" +
            "float v = -bottomLeftIntensity - 2.0 * leftIntensity - topLeftIntensity + bottomRightIntensity + 2.0 * rightIntensity + topRightIntensity;\n" +
            "\n" +
            "float mag = 1.0 - length(vec2(h, v));\n" +
            "\n" +
            "gl_FragColor = vec4(vec3(mag), 1.0);\n" +
            "}\n";
}
