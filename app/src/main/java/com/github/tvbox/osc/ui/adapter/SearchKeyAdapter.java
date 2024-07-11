package com.github.tvbox.osc.ui.adapter;

import android.view.View;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;

import java.util.ArrayList;

public class SearchKeyAdapter extends BaseQuickAdapter<String, BaseViewHolder> {
    public SearchKeyAdapter() {
        super(R.layout.activity_search_hot_key, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tvSearchWord, item);

        // 设置焦点变化事件监听器
        helper.getView(R.id.tvSearchWord).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
//                    ViewAnimate.animateSearchHotKeyFocus(view);
                    TextView textView = (TextView) view;
                    textView.setTextColor(mContext.getResources().getColor(R.color.color_1890FF));
                    textView.setAlpha(1F);
                } else {
                    TextView textView = view.findViewById(R.id.tvSearchWord);
                    textView.setTextColor(mContext.getResources().getColor(R.color.color_FFFFFF));
                    textView.setAlpha(0.7F);
                }
            }
        });
    }
}
