package me.lake.librestreaming.sample.ui;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import me.lake.librestreaming.sample.R;

/**
 * Created by lake on 02/12/16.
 * librestreaming project.
 */
public class FakeView extends FrameLayout {
    Button btn_test;
    TextView tv_test;
    ImageView iv_test;
    SeekBar sb_test;
    private int VWidth, VHeight;

    public FakeView(Context context) {
        super(context);
    }

    public void init(int w, int h) {
        VWidth = w;
        VHeight = h;
        this.setBackgroundColor(Color.argb(0, 0, 0, 0));
        tv_test = new TextView(this.getContext());
        FrameLayout.LayoutParams testLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        testLayoutParams.gravity = Gravity.TOP | Gravity.LEFT;
        tv_test.setLayoutParams(testLayoutParams);
        tv_test.setText("TextView");
        tv_test.setTextSize(10);
        tv_test.setTextColor(Color.RED);
        tv_test.setBackgroundColor(Color.argb(255, 150, 150, 150));
        this.addView(tv_test);

        btn_test = new Button(this.getContext());
        testLayoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        testLayoutParams.gravity = Gravity.TOP | Gravity.RIGHT;
        btn_test.setLayoutParams(testLayoutParams);
        btn_test.setText("Button");
        btn_test.setTextSize(10);
        btn_test.setTextColor(Color.RED);
        this.addView(btn_test);

        iv_test = new ImageView(this.getContext());
        int small = VWidth>VHeight?VHeight:VWidth;
        testLayoutParams = new FrameLayout.LayoutParams(small/2, small/2);
        testLayoutParams.gravity = Gravity.CENTER;
        iv_test.setImageResource(R.mipmap.cat);
        iv_test.setLayoutParams(testLayoutParams);
        this.addView(iv_test);

        sb_test = new SeekBar(this.getContext());
        testLayoutParams = new FrameLayout.LayoutParams(VWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT);
        testLayoutParams.gravity = Gravity.BOTTOM|Gravity.LEFT;
        sb_test.setLayoutParams(testLayoutParams);
        this.addView(sb_test);

        AlwaysMarqueeTextView tv_marqueetest = new AlwaysMarqueeTextView(this.getContext());
        testLayoutParams = new FrameLayout.LayoutParams(VWidth/2, ViewGroup.LayoutParams.WRAP_CONTENT);
        testLayoutParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        tv_marqueetest.setSingleLine();
        tv_marqueetest.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        tv_marqueetest.setMarqueeRepeatLimit(-1);
        tv_marqueetest.setText("MarqueeLonnnnnnnngTextView");
        tv_marqueetest.setTextSize(10);
        tv_marqueetest.setTextColor(Color.BLUE);
        tv_marqueetest.setBackgroundColor(Color.argb(255, 150, 150, 150));
        tv_marqueetest.setLayoutParams(testLayoutParams);
        this.addView(tv_marqueetest);

        update();
    }

    private void update() {
        this.measure(VWidth, VHeight);
        this.layout(0, 0, VWidth, VHeight);
    }

    public void destroy() {
        this.removeAllViews();
    }

    ObjectAnimator translationAnim;
    ObjectAnimator rotatioinAnim;
    AnimatorSet totationSetAnim;
    ValueAnimator seekAnim;

    public void startAnim() {
        translationAnim = ObjectAnimator.ofFloat(tv_test, "Y",
                0f, 300f);
        translationAnim.setInterpolator(new DecelerateInterpolator());
        translationAnim.setDuration(3000);
        translationAnim.setRepeatCount(ValueAnimator.INFINITE);
        translationAnim.setRepeatMode(Animation.REVERSE);//设置动画循环模式。
        translationAnim.start();

        rotatioinAnim = ObjectAnimator.ofFloat(btn_test, "rotation", 0f, 360f);
        rotatioinAnim.setDuration(3000);
        rotatioinAnim.setRepeatCount(ValueAnimator.INFINITE);
        rotatioinAnim.start();

        totationSetAnim = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(iv_test, "rotationX", 0f, 180f);
        anim.setDuration(3000);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setRepeatMode(ValueAnimator.REVERSE);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(iv_test, "rotationY", 0f, 180f);
        anim3.setDuration(3000);
        anim3.setRepeatCount(ValueAnimator.INFINITE);
        anim3.setRepeatMode(ValueAnimator.REVERSE);
        totationSetAnim.play(anim);
        totationSetAnim.play(anim3);
        totationSetAnim.start();

        seekAnim = ValueAnimator.ofInt(0, sb_test.getMax());
        seekAnim.setDuration(3000);
        seekAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int animProgress = (Integer) animation.getAnimatedValue();
                sb_test.setProgress(animProgress);
            }
        });
        seekAnim.setRepeatCount(ValueAnimator.INFINITE);
        seekAnim.setRepeatMode(ValueAnimator.REVERSE);
        seekAnim.start();
    }

    public void stopAnim() {
        translationAnim.cancel();
        rotatioinAnim.cancel();
        totationSetAnim.cancel();
        seekAnim.cancel();
    }
}
