package com.github.tvbox.osc.ui.activity;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.bean.VideoInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.picasso.RoundTransformation;
import com.github.tvbox.osc.ui.adapter.SeriesAdapter;
import com.github.tvbox.osc.ui.adapter.SeriesFlagAdapter;
import com.github.tvbox.osc.ui.dialog.QuickSearchDialog;
import com.github.tvbox.osc.ui.fragment.PlayFragment;
import com.github.tvbox.osc.util.CollectionUtils;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.MD5;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.util.SubtitleHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author pj567
 * @date :2020/12/22
 * @description:
 */

public class DetailActivity extends BaseActivity {
    private LinearLayout contentLayout;
    private FragmentContainerView llPlayerFragmentContainer;
    private View llPlayerFragmentContainerBlock;
    private View playPreview;
    private PlayFragment playFragment = null;
    private ImageView videoPoster;
    private TextView videoName;
    private TextView tvLang;
    private TextView tvActor;
    //    private TextView tvPlayUrl;
    private TextView videoDesc;
    private TextView tvPlay;
    private TextView tvSort;
    private TextView tvQuickSearch;
    private TextView tvCollect;
    private TvRecyclerView playSourceView;
    // 剧集
    private TvRecyclerView episodeView;
    private TvRecyclerView mSeriesGroupView;
    private LinearLayout emptyPlayList;
    private SourceViewModel sourceViewModel;
    private Movie.Video video;
    private VideoInfo videoInfo;
    public String videoId;
    public String sourceKey;
    public String firstsourceKey;
    boolean seriesSelect = false;
    private View seriesFlagFocus = null;
    private boolean isReverse;
    private String preFlag = "";
    private boolean firstReverse;
    // 每行显示多少个集数
    private int episodeLineItemCount = 10;
    // 集分组
    private int groupCount = 20;

    private SeriesFlagAdapter seriesFlagAdapter;
    private BaseQuickAdapter<String, BaseViewHolder> seriesGroupAdapter;
    private SeriesAdapter seriesAdapter;
    private V7GridLayoutManager episodeViewLayoutMgr = null;
    private HashMap<String, String> mCheckSources = null;
    private final ArrayList<String> seriesGroupOptions = new ArrayList<>();
    private View currentSeriesGroupView;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_detail;
    }

    @Override
    protected void init() {
        EventBus.getDefault().register(this);

        initView();
        initListeningEvents();
        initData();
    }

    private void initView() {
        contentLayout = findViewById(R.id.contentLayout);
        playPreview = findViewById(R.id.playPreview);
        llPlayerFragmentContainer = findViewById(R.id.previewPlayer);
        llPlayerFragmentContainerBlock = findViewById(R.id.previewPlayerBlock);
        videoPoster = findViewById(R.id.videoPoster);
        videoName = findViewById(R.id.videoName);
        tvLang = findViewById(R.id.tvLang);
        tvActor = findViewById(R.id.tvActor);
//        tvPlayUrl = findViewById(R.id.tvPlayUrl);
        videoDesc = findViewById(R.id.videoDesc);
        tvPlay = findViewById(R.id.tvPlay);
        tvSort = findViewById(R.id.tvSort);
        tvCollect = findViewById(R.id.tvCollect);
        tvQuickSearch = findViewById(R.id.tvQuickSearch);
        emptyPlayList = findViewById(R.id.emptyPlaylist);

        playPreview.setVisibility(showPreview ? View.VISIBLE : View.GONE);
        videoPoster.setVisibility(!showPreview ? View.VISIBLE : View.GONE);

        episodeView = findViewById(R.id.episodeView);
        episodeView.setHasFixedSize(false);
        this.episodeViewLayoutMgr = new V7GridLayoutManager(this.mContext, episodeLineItemCount);
        episodeView.setLayoutManager(this.episodeViewLayoutMgr);
        seriesAdapter = new SeriesAdapter();
        episodeView.setAdapter(seriesAdapter);

        playSourceView = findViewById(R.id.playSourceView);
        playSourceView.setHasFixedSize(true);
        playSourceView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        seriesFlagAdapter = new SeriesFlagAdapter();
        playSourceView.setAdapter(seriesFlagAdapter);
        isReverse = false;
        firstReverse = false;
        preFlag = "";
        if (showPreview) {
            playFragment = new PlayFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.previewPlayer, playFragment).commit();
            getSupportFragmentManager().beginTransaction().show(playFragment).commitAllowingStateLoss();
            tvPlay.setText("全屏");
        }

        mSeriesGroupView = findViewById(R.id.mSeriesGroupView);
        mSeriesGroupView.setHasFixedSize(true);
        mSeriesGroupView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        seriesGroupAdapter = new BaseQuickAdapter<String, BaseViewHolder>(R.layout.activity_detail_episode_group, seriesGroupOptions) {
            @Override
            protected void convert(BaseViewHolder helper, String item) {
                TextView tvSeries = helper.getView(R.id.tvSeriesGroup);
                tvSeries.setText(item);
            }
        };
        mSeriesGroupView.setAdapter(seriesGroupAdapter);

        //禁用播放地址焦点
//        tvPlayUrl.setFocusable(false);

        llPlayerFragmentContainerBlock.setOnClickListener((view -> toggleFullPreview()));

//        tvPlayUrl.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //获取剪切板管理器
//                ClipboardManager cm = (ClipboardManager)getSystemService(mContext.CLIPBOARD_SERVICE);
//                //设置内容到剪切板
//                cm.setPrimaryClip(ClipData.newPlainText(null, tvPlayUrl.getText().toString().replace("播放地址：","")));
//                Toast.makeText(DetailActivity.this, "已复制", Toast.LENGTH_SHORT).show();
//            }
//        });

        seriesAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                if (videoInfo != null && videoInfo.seriesMap.get(videoInfo.playFlag).size() > 0) {
                    boolean reload = false;
                    for (int j = 0; j < videoInfo.seriesMap.get(videoInfo.playFlag).size(); j++) {
                        seriesAdapter.getData().get(j).selected = false;
                        seriesAdapter.notifyItemChanged(j);
                    }
                    //解决倒叙不刷新
                    if (videoInfo.playIndex != position) {
                        seriesAdapter.getData().get(position).selected = true;
                        seriesAdapter.notifyItemChanged(position);
                        videoInfo.playIndex = position;

                        reload = true;
                    }
                    //解决当前集不刷新的BUG
                    if (!preFlag.isEmpty() && !videoInfo.playFlag.equals(preFlag)) {
                        reload = true;
                    }

                    seriesAdapter.getData().get(videoInfo.playIndex).selected = true;
                    seriesAdapter.notifyItemChanged(videoInfo.playIndex);
                    //选集全屏 想选集不全屏的注释下面一行
                    if (showPreview && !fullWindows) toggleFullPreview();
                    if (!showPreview || reload) {
                        onPlayVideo();
                        firstReverse = false;
                    }
                }
            }
        });

        mSeriesGroupView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                if (videoInfo != null && CollectionUtils.isNotEmpty(videoInfo.seriesMap.get(videoInfo.playFlag))) {
                    int targetPos = position * groupCount + 1;
                    episodeView.smoothScrollToPosition(targetPos);
                }

                currentSeriesGroupView = itemView;
                currentSeriesGroupView.isSelected();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
            }
        });

        episodeView.setOnFocusChangeListener((view, b) -> onGridViewFocusChange(view, b));

        setLoadSir(contentLayout);
    }

    private void initData() {
        showLoading();

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            loadDetail(bundle.getString("id", null), bundle.getString("sourceKey", ""));
        }
    }

    private void initListeningEvents() {
        changedSort();
        changedPlay();
        changedQuickSearch();
        changedCollect();
        changedEpisodeView();
        changedPlaySourceView();

        changedVideoDetail();
//        changedSort();
    }

    private void changedSort() {
        tvSort.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onClick(View v) {
                if (videoInfo != null && videoInfo.seriesMap.size() > 0) {
                    videoInfo.reverseSort = !videoInfo.reverseSort;
                    isReverse = !isReverse;
                    videoInfo.reverse();
                    videoInfo.playIndex = (videoInfo.seriesMap.get(videoInfo.playFlag).size() - 1) - videoInfo.playIndex;
//                    insertVod(sourceKey, videoInfo);
                    firstReverse = true;

                    tvSort.setText(isReverse ? "正序" : "倒序");
                    formatEpisodeGroupList();
                    seriesAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    private void changedPlay() {
        tvPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                if (showPreview) {
                    toggleFullPreview();
                    if (firstReverse) {
                        onPlayVideo();
                        firstReverse = false;
                    }
                } else {
                    onPlayVideo();
                }
            }
        });
    }

    private void changedQuickSearch() {
        tvQuickSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQuickSearch();
                QuickSearchDialog quickSearchDialog = new QuickSearchDialog(DetailActivity.this);
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, quickSearchData));
                EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, quickSearchWord));
                quickSearchDialog.show();
                if (pauseRunnable != null && pauseRunnable.size() > 0) {
                    searchExecutorService = Executors.newFixedThreadPool(5);
                    for (Runnable runnable : pauseRunnable) {
                        searchExecutorService.execute(runnable);
                    }
                    pauseRunnable.clear();
                    pauseRunnable = null;
                }
                quickSearchDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        try {
                            if (searchExecutorService != null) {
                                pauseRunnable = searchExecutorService.shutdownNow();
                                searchExecutorService = null;
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }
                });
            }
        });
    }

    private void changedCollect() {
        tvCollect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = tvCollect.getText().toString();
                if ("收藏".equals(text)) {
                    RoomDataManger.insertVodCollect(sourceKey, videoInfo);
                    tvCollect.setText("已收藏");
                } else {
                    RoomDataManger.deleteVodCollect(sourceKey, videoInfo);
                    tvCollect.setText("收藏");
                }
            }
        });
    }

    private void changedEpisodeView() {
        episodeView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
                seriesSelect = false;
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                seriesSelect = true;
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
            }
        });
    }

    private void changedPlaySourceView() {
        playSourceView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            private void refresh(View itemView, int position) {
                String newFlag = seriesFlagAdapter.getData().get(position).name;
                if (videoInfo != null && !videoInfo.playFlag.equals(newFlag)) {
                    for (int i = 0; i < videoInfo.seriesFlags.size(); i++) {
                        VideoInfo.VodSeriesFlag flag = videoInfo.seriesFlags.get(i);
                        if (flag.name.equals(videoInfo.playFlag)) {
                            flag.selected = false;
                            seriesFlagAdapter.notifyItemChanged(i);
                            break;
                        }
                    }
                    VideoInfo.VodSeriesFlag flag = videoInfo.seriesFlags.get(position);
                    flag.selected = true;
                    // clean pre flag select status
                    if (videoInfo.seriesMap.get(videoInfo.playFlag).size() > videoInfo.playIndex) {
                        videoInfo.seriesMap.get(videoInfo.playFlag).get(videoInfo.playIndex).selected = false;
                    }
                    videoInfo.playFlag = newFlag;
                    seriesFlagAdapter.notifyItemChanged(position);
                    formatEpisodeList();
                }
                seriesFlagFocus = itemView;
            }

            @Override
            public void onItemPreSelected(TvRecyclerView parent, View itemView, int position) {
//                seriesSelect = false;
            }

            @Override
            public void onItemSelected(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
//                if(isReverse)videoInfo.reverse();
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                refresh(itemView, position);
//                if(isReverse)videoInfo.reverse();
            }
        });
    }

    private void onGridViewFocusChange(View view, boolean hasFocus) {
        if (llPlayerFragmentContainerBlock.getVisibility() != View.VISIBLE) return;
        llPlayerFragmentContainerBlock.setFocusable(!hasFocus);
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    private List<Runnable> pauseRunnable = null;

    private void onPlayVideo() {
        if (videoInfo != null && videoInfo.seriesMap.get(videoInfo.playFlag).size() > 0) {
            preFlag = videoInfo.playFlag;
            //更新播放地址
//            setTextShow(tvPlayUrl, "播放地址：", videoInfo.seriesMap.get(videoInfo.playFlag).get(videoInfo.playIndex).url);
            Bundle bundle = new Bundle();
            //保存历史
            insertVod(firstsourceKey, videoInfo);
            //   insertVod(sourceKey, videoInfo);
            bundle.putString("sourceKey", sourceKey);
//            bundle.putSerializable("VodInfo", videoInfo);
            App.getInstance().setVodInfo(videoInfo);
            if (showPreview) {
                if (previewVideoInfo == null) {
                    try {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(videoInfo);
                        oos.flush();
                        oos.close();
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
                        previewVideoInfo = (VideoInfo) ois.readObject();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (previewVideoInfo != null) {
                    previewVideoInfo.playerCfg = videoInfo.playerCfg;
                    previewVideoInfo.playFlag = videoInfo.playFlag;
                    previewVideoInfo.playIndex = videoInfo.playIndex;
                    previewVideoInfo.seriesMap = videoInfo.seriesMap;
//                    bundle.putSerializable("VodInfo", previewVodInfo);
                    App.getInstance().setVodInfo(previewVideoInfo);
                }

                playFragment.setData(bundle);
            } else {
                jumpActivity(PlayActivity.class, bundle);
            }
        }
    }

    /**
     * 格式化剧集列表
     */
    private void formatEpisodeList() {
        // 获取剧集
        List<VideoInfo.VodSeries> episodeList = videoInfo.seriesMap.get(videoInfo.playFlag);
        if (episodeList.size() <= videoInfo.playIndex) {
            videoInfo.playIndex = 0;
        }

        episodeList.get(videoInfo.playIndex).selected = true;
        /**
         * 重设剧集名称：1、2、3...
         */
        for (int j = 0, size = episodeList.size(); j < size; j++) {
            VideoInfo.VodSeries videoEpisode = episodeList.get(j);
            videoEpisode.name = String.valueOf(j + 1);
        }

        episodeViewLayoutMgr.setSpanCount(episodeLineItemCount);
        seriesAdapter.setNewData(videoInfo.seriesMap.get(videoInfo.playFlag));

        formatEpisodeGroupList();

        episodeView.postDelayed(new Runnable() {
            @Override
            public void run() {
                episodeView.smoothScrollToPosition(videoInfo.playIndex);
            }
        }, 100);
    }

    /**
     * 设置剧集分组
     */
    private void formatEpisodeGroupList() {
        List<VideoInfo.VodSeries> seriesList;
        if (videoInfo.seriesMap.containsKey(videoInfo.playFlag)) {
            seriesList = videoInfo.seriesMap.get(videoInfo.playFlag);
        } else {
            seriesList = Collections.emptyList();
        }

        int listSize = seriesList.size();
        mSeriesGroupView.setVisibility(listSize <= groupCount ? View.GONE : View.VISIBLE);

        seriesGroupOptions.clear();

        if (!videoInfo.reverseSort) {
            for (int i = 0; i < listSize; i += groupCount) {
                int start = i + 1;
                int end = (i + groupCount > listSize) ? listSize : i + groupCount;

                if (start == end) {
                    seriesGroupOptions.add(String.valueOf(start));
                } else {
                    seriesGroupOptions.add(start + "-" + end);
                }
            }
        } else {
            for (int i = listSize; i > 0; i -= groupCount) {
                int start = i;
                int end = (start - groupCount < 0) ? 1 : start - groupCount + 1;

                if (start == end) {
                    seriesGroupOptions.add(String.valueOf(start));
                } else {
                    seriesGroupOptions.add(start + "-" + end);
                }
            }
        }

        seriesGroupAdapter.notifyDataSetChanged();
    }

    private void changedVideoDetail() {
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.detailResult.observe(this, new Observer<AbsXml>() {
            @Override
            public void onChanged(AbsXml absXml) {
                closeLoading();
                if (absXml == null || absXml.movie == null || CollectionUtils.isEmpty(absXml.movie.videoList)) {
                    emptyPlayList.setVisibility(View.VISIBLE);
                    findViewById(R.id.topLayout).setVisibility(View.GONE);
                    llPlayerFragmentContainer.setVisibility(View.GONE);
                    llPlayerFragmentContainerBlock.setVisibility(View.GONE);
                    return;
                }
                if (StringUtils.isNotBlank(absXml.msg) && !absXml.msg.equals("数据列表")) {
                    Toast.makeText(DetailActivity.this, absXml.msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                video = absXml.movie.videoList.get(0);
                video.id = videoId;

                videoInfo = new VideoInfo();
                videoInfo.setVideo(video);
                videoInfo.sourceKey = video.sourceKey;
                sourceKey = video.sourceKey;

                videoName.setText(video.name);
                String year = video.year == 0 ? null : String.valueOf(video.year);
                String type = com.github.tvbox.osc.util.StringUtils.replaceSymbolsWithSpace(video.type);
                tvLang.setText(getAppendStr(video.lang, video.area, year, type));
                String actor = getAppendStr(video.director, video.actor);
                String note = getAppendStr(com.github.tvbox.osc.util.StringUtils.replaceSymbolsWithPoint(ApiConfig.get().getSource(sourceKey).getName()), video.note);
                // 如果没有导演和演员数据，显示 来源和note
                tvActor.setText(StringUtils.isBlank(actor) ? note : actor);
                videoDesc.setText(removeHtmlTag(video.des));

                if (TextUtils.isEmpty(video.pic)) {
                    videoPoster.setImageResource(R.drawable.img_loading_placeholder);
                } else {
                    Picasso.get()
                            .load(DefaultConfig.checkReplaceProxy(video.pic))
                            .transform(new RoundTransformation(MD5.string2MD5(video.pic + video.name))
                                    .centerCorp(true))
                            .into(videoPoster);
                }

                // 剧集
                if (videoInfo.seriesMap != null && videoInfo.seriesMap.size() > 0) {
                    playSourceView.setVisibility(View.VISIBLE);
                    episodeView.setVisibility(View.VISIBLE);
                    tvPlay.setVisibility(View.VISIBLE);
                    emptyPlayList.setVisibility(View.GONE);

                    VideoInfo videoInfoRecord = RoomDataManger.getVodInfo(sourceKey, videoId);
                    // 读取历史记录
                    if (videoInfoRecord != null) {
                        videoInfo.playIndex = Math.max(videoInfoRecord.playIndex, 0);
                        videoInfo.playFlag = videoInfoRecord.playFlag;
                        videoInfo.playerCfg = videoInfoRecord.playerCfg;
                        videoInfo.reverseSort = videoInfoRecord.reverseSort;
                    } else {
                        videoInfo.playIndex = 0;
                        videoInfo.playFlag = null;
                        videoInfo.playerCfg = "";
                        videoInfo.reverseSort = false;
                    }

                    if (videoInfo.reverseSort) {
                        videoInfo.reverse();
                    }

                    if (videoInfo.playFlag == null || !videoInfo.seriesMap.containsKey(videoInfo.playFlag))
                        videoInfo.playFlag = (String) videoInfo.seriesMap.keySet().toArray()[0];

                    int flagScrollTo = 0;
                    for (int j = 0; j < videoInfo.seriesFlags.size(); j++) {
                        VideoInfo.VodSeriesFlag flag = videoInfo.seriesFlags.get(j);
                        if (flag.name.equals(videoInfo.playFlag)) {
                            flagScrollTo = j;
                            flag.selected = true;
                        } else
                            flag.selected = false;
                    }

                    //设置播放地址
//                        setTextShow(tvPlayUrl, "播放地址：", videoInfo.seriesMap.get(videoInfo.playFlag).get(0).url);
                    seriesFlagAdapter.setNewData(videoInfo.seriesFlags);
                    playSourceView.scrollToPosition(flagScrollTo);

                    formatEpisodeList();
                    if (showPreview) {
                        onPlayVideo();
                        llPlayerFragmentContainer.setVisibility(View.VISIBLE);
                        llPlayerFragmentContainerBlock.setVisibility(View.VISIBLE);
                        toggleSubtitleTextSize();
                    }
                    // startQuickSearch();
                } else {
                    playSourceView.setVisibility(View.GONE);
                    episodeView.setVisibility(View.GONE);
                    mSeriesGroupView.setVisibility(View.GONE);
                    tvPlay.setVisibility(View.GONE);
                    emptyPlayList.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void loadDetail(String id, String key) {
        if (StringUtils.isBlank(id)) {
            return;
        }

        videoId = id;
        sourceKey = key;
        firstsourceKey = key;

        sourceViewModel.getDetail(sourceKey, videoId);
        boolean isCollect = RoomDataManger.isVodCollect(sourceKey, videoId);
        if (isCollect) {
            tvCollect.setText("已收藏");
        } else {
            tvCollect.setText("收藏");
        }
    }

    /**
     * 将多个属性拼接成：A | B | C
     *
     * @param strs
     * @return
     */
    private String getAppendStr(String... strs) {
        String result = "";
        for (String str : strs) {
            if (StringUtils.isNotBlank(str)) {
                if (!result.isEmpty()) {
                    result += "  |  ";
                }
                result += str;
            }
        }

        return result;
    }

    private String removeHtmlTag(String info) {
        if (info == null)
            return "";
        return info.replaceAll("\\<.*?\\>", "").replaceAll("\\s", "");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_REFRESH) {
            if (event.obj != null) {
                if (event.obj instanceof Integer) {
                    int index = (int) event.obj;
                    for (int j = 0; j < videoInfo.seriesMap.get(videoInfo.playFlag).size(); j++) {
                        seriesAdapter.getData().get(j).selected = false;
                        seriesAdapter.notifyItemChanged(j);
                    }
                    seriesAdapter.getData().get(index).selected = true;
                    seriesAdapter.notifyItemChanged(index);
                    episodeView.setSelection(index);
                    videoInfo.playIndex = index;
                    //保存历史
                    insertVod(firstsourceKey, videoInfo);
                    //   insertVod(sourceKey, videoInfo);
                } else if (event.obj instanceof JSONObject) {
                    videoInfo.playerCfg = ((JSONObject) event.obj).toString();
                    //保存历史
                    insertVod(firstsourceKey, videoInfo);
                    //        insertVod(sourceKey, videoInfo);
                }

            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_SELECT) {
            if (event.obj != null) {
                Movie.Video video = (Movie.Video) event.obj;
                loadDetail(video.id, video.sourceKey);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_WORD_CHANGE) {
            if (event.obj != null) {
                String word = (String) event.obj;
                switchSearchWord(word);
            }
        } else if (event.type == RefreshEvent.TYPE_QUICK_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private String searchTitle = "";
    private boolean hadQuickStart = false;
    private final List<Movie.Video> quickSearchData = new ArrayList<>();
    private final List<String> quickSearchWord = new ArrayList<>();
    private ExecutorService searchExecutorService = null;

    private void switchSearchWord(String word) {
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchData.clear();
        searchTitle = word;
        searchResult();
    }

    private void startQuickSearch() {
        initCheckedSourcesForSearch();
        if (hadQuickStart)
            return;
        hadQuickStart = true;
        OkGo.getInstance().cancelTag("quick_search");
        quickSearchWord.clear();
        searchTitle = video.name;
        quickSearchData.clear();
        quickSearchWord.addAll(SearchHelper.splitWords(searchTitle));
        // 分词
        OkGo.<String>get("http://api.pullword.com/get.php?source=" + URLEncoder.encode(searchTitle) + "&param1=0&param2=0&json=1")
                .tag("fenci")
                .execute(new AbsCallback<String>() {
                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        if (response.body() != null) {
                            return response.body().string();
                        } else {
                            throw new IllegalStateException("网络请求错误");
                        }
                    }

                    @Override
                    public void onSuccess(Response<String> response) {
                        String json = response.body();
                        try {
                            for (JsonElement je : new Gson().fromJson(json, JsonArray.class)) {
                                quickSearchWord.add(je.getAsJsonObject().get("t").getAsString());
                            }
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                        List<String> words = new ArrayList<>(new HashSet<>(quickSearchWord));
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH_WORD, words));
                    }

                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                    }
                });

        searchResult();
    }

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable() || !bean.isQuickSearch()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
        }
        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    sourceViewModel.getQuickSearch(key, searchTitle);
                }
            });
        }
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                // 去除当前相同的影片
                if (video.sourceKey.equals(sourceKey) && video.id.equals(videoId))
                    continue;
                data.add(video);
            }
            quickSearchData.addAll(data);
            EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_QUICK_SEARCH, data));
        }
    }

    private void insertVod(String sourceKey, VideoInfo videoInfo) {
        try {
            videoInfo.playNote = videoInfo.seriesMap.get(videoInfo.playFlag).get(videoInfo.playIndex).name;
        } catch (Throwable th) {
            videoInfo.playNote = "";
        }
        RoomDataManger.insertVodRecord(sourceKey, videoInfo);
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_HISTORY_REFRESH));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        OkGo.getInstance().cancelTag("fenci");
        OkGo.getInstance().cancelTag("detail");
        OkGo.getInstance().cancelTag("quick_search");
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (fullWindows) {
            if (playFragment.onBackPressed())
                return;
            toggleFullPreview();
            episodeView.requestFocus();
            List<VideoInfo.VodSeries> list = videoInfo.seriesMap.get(videoInfo.playFlag);
            mSeriesGroupView.setVisibility(list.size() > groupCount ? View.VISIBLE : View.GONE);
            return;
        }
        if (seriesSelect) {
            if (seriesFlagFocus != null && !seriesFlagFocus.isFocused()) {
                seriesFlagFocus.requestFocus();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null && playFragment != null && fullWindows) {
            if (playFragment.dispatchKeyEvent(event)) {
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    // preview
    VideoInfo previewVideoInfo = null;
    boolean showPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true);
    // true 开启 false 关闭
    boolean fullWindows = false;
    ViewGroup.LayoutParams windowsPreview = null;
    ViewGroup.LayoutParams windowsFull = null;

    void toggleFullPreview() {
        if (windowsPreview == null) {
            windowsPreview = llPlayerFragmentContainer.getLayoutParams();
        }
        if (windowsFull == null) {
            windowsFull = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        fullWindows = !fullWindows;
        llPlayerFragmentContainer.setLayoutParams(fullWindows ? windowsFull : windowsPreview);
        llPlayerFragmentContainerBlock.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        episodeView.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        playSourceView.setVisibility(fullWindows ? View.GONE : View.VISIBLE);
        mSeriesGroupView.setVisibility(fullWindows ? View.GONE : View.VISIBLE);

        //全屏下禁用详情页几个按键的焦点 防止上键跑过来
        tvPlay.setFocusable(!fullWindows);
        tvSort.setFocusable(!fullWindows);
        tvCollect.setFocusable(!fullWindows);
        tvQuickSearch.setFocusable(!fullWindows);
        toggleSubtitleTextSize();
    }

    void toggleSubtitleTextSize() {
        int subtitleTextSize = SubtitleHelper.getTextSize(this);
        if (!fullWindows) {
            subtitleTextSize *= 0.6;
        }
        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_SUBTITLE_SIZE_CHANGE, subtitleTextSize));
    }
}
