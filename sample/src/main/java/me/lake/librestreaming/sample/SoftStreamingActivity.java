package me.lake.librestreaming.sample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import me.lake.librestreaming.filter.videofilter.BaseVideoFilter;
import me.lake.librestreaming.model.RESConfig;
import me.lake.librestreaming.sample.softfilter.AnimationFilter;
import me.lake.librestreaming.sample.softfilter.BlackWhiteFilter;
import me.lake.librestreaming.sample.softfilter.BlurFilter;
import me.lake.librestreaming.sample.softfilter.DoNothingFilter;
import me.lake.librestreaming.sample.softfilter.FixYFilter;
import me.lake.librestreaming.sample.softfilter.GrayFilter;
import me.lake.librestreaming.sample.softfilter.IconFilter;
import me.lake.librestreaming.sample.softfilter.SkinBlurFilter;

/**
 * Created by lake on 16-5-31.
 */
public class SoftStreamingActivity extends BaseStreamingActivity {
    protected FilterAdapter filterAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        filtermode = RESConfig.FilterMode.SOFT;
        super.onCreate(savedInstanceState);
        /**
         * filters just for demo
         */
        ArrayList<FilterItem> filterItems = new ArrayList<FilterItem>();
        filterItems.add(new FilterItem("nofilter", null));
        filterItems.add(new FilterItem("DoNothingFilter", new DoNothingFilter()));
        filterItems.add(new FilterItem("GrayFilter", new GrayFilter()));
        filterItems.add(new FilterItem("BlackWhiteFilter", new BlackWhiteFilter((byte) 0x80)));
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        filterItems.add(new FilterItem("IconFilter", new IconFilter(bitmap)));
        filterItems.add(new FilterItem("BlurFilter", new BlurFilter(this)));
        filterItems.add(new FilterItem("SkinBlurFilter", new SkinBlurFilter(this)));
        filterItems.add(new FilterItem("FixYFilter", new FixYFilter((byte) 0)));
        Bitmap[] ans = new Bitmap[4];
        ans[0] = BitmapFactory.decodeResource(getResources(), R.drawable.animationa);
        ans[1] = BitmapFactory.decodeResource(getResources(), R.drawable.animationb);
        ans[2] = BitmapFactory.decodeResource(getResources(), R.drawable.animationc);
        ans[3] = BitmapFactory.decodeResource(getResources(), R.drawable.animationd);
        filterItems.add(new FilterItem("AnimationFilter", new AnimationFilter(ans)));
        filterAdapter = new FilterAdapter();
        filterAdapter.updateFilters(filterItems);
        lv_filter.setAdapter(filterAdapter);
        lv_filter.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (filterAdapter.selectItem(position)) {
                    //changeFilter
                    BaseVideoFilter filter = ((FilterItem) filterAdapter.getItem(position)).filter;
                    resClient.setVideoFilter(filter);
                }
            }
        });
        sb_attr.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                BaseVideoFilter filter = resClient.acquireVideoFilter();
                if (filter != null) {
                    if (filter instanceof BlackWhiteFilter) {
                        BlackWhiteFilter blackWhiteFilter = (BlackWhiteFilter) filter;
                        blackWhiteFilter.setGap((byte) ((255 * progress) / 100));
                    }
                    if (filter instanceof IconFilter) {
                        IconFilter iconFilter = (IconFilter) filter;
                        iconFilter.setXPos(progress * 5);
                    }
                    if (filter instanceof FixYFilter) {
                        FixYFilter fixYFilter = (FixYFilter) filter;
                        fixYFilter.setY((byte) ((progress / 100.0) * 255));
                    }
                    if (filter instanceof BlurFilter) {
                        BlurFilter blurFilter = (BlurFilter) filter;
                        blurFilter.setRadius(progress / 4);
                    }
                    if (filter instanceof SkinBlurFilter) {
                        SkinBlurFilter skinBlurFilter = (SkinBlurFilter) filter;
                        skinBlurFilter.setRadius(progress / 4);
                    }
                    if (filter instanceof AnimationFilter) {
                        AnimationFilter animationFilter = (AnimationFilter) filter;
                        animationFilter.setXPos(progress * 5);
                    }
                }
                resClient.releaseVideoFilter();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sb_zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                resClient.setZoomByPercent(progress / 100.0f);
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
        BaseVideoFilter filter;

        public FilterItem(String name, BaseVideoFilter filter) {
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
                convertView = LayoutInflater.from(SoftStreamingActivity.this).inflate(R.layout.item_filter, parent, false);
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
}
