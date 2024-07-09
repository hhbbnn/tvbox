package com.github.tvbox.osc.ui.animate;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ViewAnimate {

    /**
     * 列表影视项聚焦动画
     *
     * @param view
     */
    public static void animateVideoItemFocus(View view) {
        animateScaling(view, 1f, 1.05f, 150);
    }

    /**
     * 视图缩放动画(放大)
     *
     * @param view
     */
    public static void animateHomeMenuFocus(View view) {
        animateScaling(view, 1f, 1.1f, 150);
    }

    /**
     * 视图缩放动画(缩小)
     *
     * @param view
     */
    public static void animateHomeToolFocus(View view) {
        animateScaling(view, 1f, 0.9f, 150);
    }

    /**
     * 视图缩放动画
     *
     * @param targetView
     * @param beforeSize
     * @param afterSize
     * @param duration
     */
    public static void animateScaling(View targetView, float beforeSize, float afterSize, int duration) {
        // 创建一个ObjectAnimator来控制View的放大动画
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(targetView, "scaleX", beforeSize, afterSize);
        scaleUp.setDuration(duration); // 放大过程持续150毫秒
        scaleUp.setInterpolator(new LinearInterpolator());

        ObjectAnimator scaleYUp = ObjectAnimator.ofFloat(targetView, "scaleY", beforeSize, afterSize);
        scaleYUp.setDuration(duration);
        scaleYUp.setInterpolator(new LinearInterpolator());

        // 将两个动画组合起来，实现同时在X轴和Y轴上放大
        scaleUp.start();
        scaleYUp.start();

        // 使用监听器，在放大动画结束后开始缩小动画
        scaleUp.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                // 创建缩小的动画
                ObjectAnimator scaleDown = ObjectAnimator.ofFloat(targetView, "scaleX", afterSize, beforeSize);
                scaleDown.setDuration(duration);

                ObjectAnimator scaleYDown = ObjectAnimator.ofFloat(targetView, "scaleY", afterSize, beforeSize);
                scaleYDown.setDuration(duration);

                // 开始缩小动画
                scaleDown.start();
                scaleYDown.start();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }
}
