package com.github.tvbox.osc.ui.adapter;

import android.view.View;
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
public class SeriesFlagAdapter extends BaseQuickAdapter<VideoInfo.VodSeriesFlag, BaseViewHolder> {
    public SeriesFlagAdapter() {
        super(R.layout.activity_detail_play_source, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, VideoInfo.VodSeriesFlag item) {
//        TextView tvSeries = helper.getView(R.id.playSource);
        View select = helper.getView(R.id.playSourceSelect);
        if (item.selected) {
            select.setVisibility(View.VISIBLE);
        } else {
            select.setVisibility(View.GONE);
        }
        helper.setText(R.id.playSource, item.name);
    }
}