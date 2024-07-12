package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.StringUtils;
import com.github.tvbox.osc.util.VideoUtils;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;

public class SearchAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {
    public SearchAdapter() {
        super(Hawk.get(HawkConfig.SEARCH_VIEW, 0) == 0 ? R.layout.item_search_lite : R.layout.activity_common_video_list, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video video) {
        String srcName = StringUtils.replaceSymbolsWithPoint(ApiConfig.get().getSource(video.sourceKey).getName());

        VideoUtils.ListItem.setCommonParam(helper, video.name, video.note, srcName);
        VideoUtils.ListItem.setImageStyle(helper, mContext, video.pic, 0);
        VideoUtils.reSizeVideoItemFrame(helper, mContext, 200, 300);

        // lite
        if (Hawk.get(HawkConfig.SEARCH_VIEW, 0) == 0) {
            helper.setText(R.id.tvName, String.format("%s  %s %s %s", ApiConfig.get().getSource(video.sourceKey).getName(), video.name, video.type == null ? "" : video.type, video.note == null ? "" : video.note));
        }
    }
}