package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.constans.SystemConstants;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.VideoUtils;
import com.orhanobut.hawk.Hawk;

import java.util.ArrayList;

public class HomeHotVideoAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {
    public HomeHotVideoAdapter() {
        super(R.layout.activity_common_video_list, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video video) {
        String remark = Hawk.get(HawkConfig.HOME_REC, 0) == SystemConstants.Setting.HomeRecType.DOUBAN.getCode()
                ? "豆瓣热门" : null;

        VideoUtils.ListItem.setCommonParam(helper, video.name, remark, video.note);
        VideoUtils.ListItem.setImageStyle(helper, mContext, video.pic, 0);
        VideoUtils.reSizeVideoItemFrame(helper, mContext, 200, 300);
    }
}