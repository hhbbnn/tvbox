package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.FrameLayout;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.util.VideoUtils;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class HistoryAdapter extends BaseQuickAdapter<VideoInfo, BaseViewHolder> {
    public HistoryAdapter() {
        super(R.layout.activity_common_video_list, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VideoInfo item) {
        // takagen99: Add Delete Mode
        FrameLayout tvDel = helper.getView(R.id.delFrameLayout);
        if (HawkConfig.hotVodDelete) {
            tvDel.setVisibility(View.VISIBLE);
        } else {
            tvDel.setVisibility(View.GONE);
        }

        String srcName = StringUtils.replaceSymbolsWithPoint(ApiConfig.get().getSource(item.sourceKey).getName());

        VideoUtils.ListItem.setCommonParam(helper, item.name, item.note, srcName);
        VideoUtils.ListItem.setImageStyle(helper, mContext, item.pic, 0);
        VideoUtils.reSizeVideoItemFrame(helper, mContext, 200, 300);
    }
}