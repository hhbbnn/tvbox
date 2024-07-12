package com.github.tvbox.osc.util;

import android.content.Context;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;

import me.jessyan.autosize.utils.AutoSizeUtils;

/**
 *
 */
public class VideoUtils {

    /**
     * 视频列表项处理
     */
    public static class ListItem {
        /**
         * 设置视频列表项的参数
         *
         * @param baseViewHolder
         * @param videoName
         * @param videoTag
         * @param videoRemark
         */
        public static void setCommonParam(BaseViewHolder baseViewHolder, String videoName, String videoRemark, String videoTag) {
            if (StringUtils.isBlank(videoTag)) {
                baseViewHolder.setVisible(R.id.videoTag, false);
            } else {
                baseViewHolder.setText(R.id.videoTag, videoTag);
            }

            if (StringUtils.isBlank(videoRemark)) {
                baseViewHolder.setVisible(R.id.videoRemark, false);
            } else {
                baseViewHolder.setText(R.id.videoRemark, videoRemark);
            }

            baseViewHolder.setText(R.id.videoName, videoName);
        }

        /**
         * 设置视频列表项的图片
         *
         * @param baseViewHolder
         * @param context
         * @param pic
         * @param radius
         */
        public static void setImageStyle(BaseViewHolder baseViewHolder, Context context, String pic, int radius) {
            ImageView videoPoster = baseViewHolder.getView(R.id.videoPoster);
            if (TextUtils.isEmpty(pic)) {
                videoPoster.setImageResource(R.drawable.jpg_vertical_default);
                return;
            }

            Picasso.get()
                    .load(pic)
                    .transform(new RoundTransformation(MD5.string2MD5(pic))
                            .roundRadius(AutoSizeUtils.mm2px(context, radius), RoundTransformation.RoundType.ALL)) // 保持圆角处理
                    .placeholder(R.drawable.jpg_vertical_default)
                    .noFade()
                    .error(R.drawable.jpg_vertical_404)
                    .into(videoPoster);
        }
    }

    /**
     * 调整视频列表项的图片呈现大小
     *
     * @param baseViewHolder
     * @param context
     * @param frameWidth：呈现宽度
     * @param videoPosterHeight: 海报高度
     */
    public static void reSizeVideoItemFrame(BaseViewHolder baseViewHolder, Context context, float frameWidth, float videoPosterHeight) {
        // 获取videoItemFrame
        FrameLayout videoItemFrame = baseViewHolder.getView(R.id.videoItemFrame);
        // 获取并更新布局参数
        ViewGroup.LayoutParams videoItemLayoutParams = videoItemFrame.getLayoutParams();
        videoItemLayoutParams.width = AutoSizeUtils.mm2px(context, frameWidth);
        videoItemFrame.setLayoutParams(videoItemLayoutParams); // 应用新的布局参数

        // 获取videoItemFrame
        LinearLayout videoPosterLinear = baseViewHolder.getView(R.id.videoPosterBox);
        // 获取并更新布局参数
        ViewGroup.LayoutParams layoutParams = videoPosterLinear.getLayoutParams();
        layoutParams.height = AutoSizeUtils.mm2px(context, videoPosterHeight);
        videoPosterLinear.setLayoutParams(layoutParams); // 应用新的布局参数

    }
}
