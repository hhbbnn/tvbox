package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.VideoInfo;

import java.util.ArrayList;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */
public class SeriesAdapter extends BaseQuickAdapter<VideoInfo.VodSeries, BaseViewHolder> {
    public SeriesAdapter() {
        super(R.layout.activity_detail_episode, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VideoInfo.VodSeries item) {
        helper.setText(R.id.tvSeries, item.name);
    }
}