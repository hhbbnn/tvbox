package com.github.tvbox.osc.ui.adapter;

import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD5;
import com.orhanobut.hawk.Hawk;
import com.squareup.picasso.Picasso;
import me.jessyan.autosize.utils.AutoSizeUtils;

import java.util.ArrayList;

public class HomeHotVideoAdapter extends BaseQuickAdapter<Movie.Video, BaseViewHolder> {
    public HomeHotVideoAdapter() {
        super(R.layout.activity_home_video_list, new ArrayList<>());
    }

    @Override
    protected void convert(BaseViewHolder helper, Movie.Video item) {
        // takagen99: Add Delete Mode
        FrameLayout tvDel = helper.getView(R.id.delFrameLayout);
        if (HawkConfig.hotVodDelete) {
            tvDel.setVisibility(View.VISIBLE);
        } else {
            tvDel.setVisibility(View.GONE);
        }

        TextView tvRate = helper.getView(R.id.tvRate);
        if (Hawk.get(HawkConfig.HOME_REC, 0) == 2) {
            tvRate.setText(ApiConfig.get().getSource(item.sourceKey).getName());
        } else if (Hawk.get(HawkConfig.HOME_REC, 0) == 0) {
            tvRate.setText("豆瓣热播");
        } else {
            tvRate.setVisibility(View.GONE);
        }

        TextView tvNote = helper.getView(R.id.tvNote);
        if (item.note == null || item.note.isEmpty()) {
            tvNote.setVisibility(View.GONE);
        } else {
            tvNote.setText(item.note);
            tvNote.setVisibility(View.VISIBLE);
        }
        helper.setText(R.id.tvName, item.name);

        ImageView videoPoster = helper.getView(R.id.videoPoster);
        //由于部分电视机使用glide报错
        if (!TextUtils.isEmpty(item.pic)) {
            item.pic = item.pic.trim();
//            System.out.println(item.pic);
            Picasso.get()
                    .load(DefaultConfig.checkReplaceProxy(item.pic))
                    .placeholder(R.drawable.default_poster)
                    .noFade()
                    .error(R.drawable.default_poster_fail)
                    .into(videoPoster);
        } else {
            videoPoster.setImageResource(R.drawable.default_poster);
        }
    }
}