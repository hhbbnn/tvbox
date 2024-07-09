package com.github.tvbox.osc.ui.adapter;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.bean.MovieSort;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pj567
 * @date :2020/12/21
 * @description:
 */
public class HomeMenuAdapter extends BaseQuickAdapter<MovieSort.SortData, BaseViewHolder> {
    public HomeMenuAdapter() {
        super(R.layout.activity_home_menus, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, MovieSort.SortData item) {
        helper.setText(R.id.tvTitle, item.name);
    }
}