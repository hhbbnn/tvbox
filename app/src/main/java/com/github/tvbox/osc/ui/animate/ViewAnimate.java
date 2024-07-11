package com.github.tvbox.osc.ui.animate;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ViewAnimate {

    /**
     * 热搜词条项-聚焦动画
     *
     * @param view
     */
    public static void animateSearchHotKeyFocus(View view) {
        floatToRight(view, 12, 150);
    }

    /**
     * 影视列表项-聚焦动画
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

    /**
     * 向右浮动的动画
     *
     * @param view
     * @param distanceMM 向右浮动的距离
     * @param duration   动画持续时间-毫秒
     */
    public static void floatToRight(View view, int distanceMM, int duration) {
        // 获取View的初始X坐标
        float startTranslationX = view.getTranslationX();

        // 创建向右浮动的动画
        ObjectAnimator moveRight = ObjectAnimator.ofFloat(view, "translationX", startTranslationX + distanceMM);
        moveRight.setDuration(duration); // 动画持续时间为0.5秒，即500毫秒

        // 创建返回原位的动画
        ObjectAnimator moveBack = ObjectAnimator.ofFloat(view, "translationX", startTranslationX);
        moveBack.setDuration(duration);

        // 使用AnimatorSet顺序执行动画
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playSequentially(moveRight, moveBack);

        // 开始动画
        animatorSet.start();
    }
}
