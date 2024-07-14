package com.github.tvbox.osc.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.catvod.crawler.JsLoader;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.bean.AbsXml;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.adapter.SearchAdapter;
import com.github.tvbox.osc.ui.adapter.SearchKeyAdapter;
import com.github.tvbox.osc.ui.dialog.RemoteDialog;
import com.github.tvbox.osc.ui.dialog.SearchCheckboxDialog;
import com.github.tvbox.osc.ui.tv.widget.SearchKeyboard;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.SearchHelper;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.AbsCallback;
import com.lzy.okgo.model.Response;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pj567
 * @date :2020/12/23
 * @description:
 */
public class SearchActivity extends BaseActivity {
    private LinearLayout llLayout;
    private TvRecyclerView mGridView;
    private TvRecyclerView mGridViewWord;
    SourceViewModel sourceViewModel;
    private RemoteDialog remoteDialog;
    private EditText searchText;
    private ImageView tvSearch;
    private TextView keyDeleteAll;
    private TextView keyDelete;
    private TextView remoteSearch;
    private SearchKeyboard keyBoard;
    private SearchAdapter searchAdapter;
    private SearchKeyAdapter hotKeyAdapter;
    private String searchTitle = "";
    private TextView tvSearchCheckboxBtn;
    private static HashMap<String, String> mCheckSources = null;
    private SearchCheckboxDialog mSearchCheckboxDialog = null;

    private static Boolean hasKeyBoard;
    private static Boolean isSearchBack;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_search;
    }

    @Override
    protected void init() {
        hasKeyBoard = true;
        isSearchBack = false;

        initView();
        initData();
        initListeningEvents();
    }

    /**
     * 初始化监听
     */
    private void initListeningEvents() {
        changedHotKey();
        changedSearch();
        changedTVSearch();
        changedKeyDelete();
        changedKeyDeleteAll();
        changedRemoteSearch();
        changedKeyBoard();
        changedTvSearchCheckbox();
    }

    private void changedHotKey() {
        hotKeyAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("title", hotKeyAdapter.getItem(position));
                    jumpActivity(FastSearchActivity.class, bundle);
                } else {
                    search(hotKeyAdapter.getItem(position));
                }
            }
        });
    }

    private void changedSearch() {
        searchAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                FastClickCheckUtil.check(view);
                Movie.Video video = searchAdapter.getData().get(position);
                if (video != null) {
                    if (searchExecutorService != null) {
                        pauseRunnable = searchExecutorService.shutdownNow();
                        searchExecutorService = null;
                        JsLoader.load();
                    }

                    hasKeyBoard = false;
                    isSearchBack = true;
                    Bundle bundle = new Bundle();
                    bundle.putString("id", video.id);
                    bundle.putString("sourceKey", video.sourceKey);
                    jumpActivity(DetailActivity.class, bundle);
                }
            }
        });
    }

    private void changedTVSearch() {
        tvSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                hasKeyBoard = true;
                String keyWord = searchText.getText().toString().trim();
                if (TextUtils.isEmpty(keyWord)) {
                    return;
                }

                if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                    Bundle bundle = new Bundle();
                    bundle.putString("title", keyWord);
                    jumpActivity(FastSearchActivity.class, bundle);
                } else {
                    search(keyWord);
                }
            }
        });
    }

    private void changedKeyDelete() {
        changedHotKey();

        keyDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = searchText.getText().toString().trim();
                if (StringUtils.isBlank(text)) {
                    return;
                }

                String searckText = text.substring(0, text.length() - 1);
                searchText.setText(searckText);
                querySimilarText(text);
            }
        });
    }

    private void changedKeyDeleteAll() {
        keyDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                searchText.setText("");
            }
        });
    }

    private void changedRemoteSearch() {
        remoteSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remoteDialog = new RemoteDialog(mContext);
                remoteDialog.show();
            }
        });
    }

    private void changedKeyBoard() {
        keyBoard.setOnSearchKeyListener(new SearchKeyboard.OnSearchKeyListener() {
            @Override
            public void onSearchKey(int pos, String key) {
                String text = searchText.getText().toString().trim();
                text += key;

                searchText.setText(text);
                querySimilarText(text);
            }
        });
    }

    private void changedTvSearchCheckbox() {
        tvSearchCheckboxBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSearchCheckboxDialog == null) {
                    List<SourceBean> allSourceBean = ApiConfig.get().getSourceBeanList();
                    List<SourceBean> searchAbleSource = new ArrayList<>();
                    for (SourceBean sourceBean : allSourceBean) {
                        if (sourceBean.isSearchable()) {
                            searchAbleSource.add(sourceBean);
                        }
                    }
                    mSearchCheckboxDialog = new SearchCheckboxDialog(SearchActivity.this, searchAbleSource, mCheckSources);
                }
                mSearchCheckboxDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                });
                mSearchCheckboxDialog.show();
            }
        });
    }

    /*
     * 禁止软键盘
     * @param activity Activity
     */
    public static void disableKeyboard(Activity activity) {
        hasKeyBoard = false;
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    /*
     * 启用软键盘
     * @param activity Activity
     */
    public static void enableKeyboard(Activity activity) {
        hasKeyBoard = true;
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
    }

    public void openSystemKeyBoard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this.getCurrentFocus(), InputMethodManager.SHOW_FORCED);
    }

    private List<Runnable> pauseRunnable = null;

    @Override
    protected void onResume() {
        super.onResume();
        if (pauseRunnable != null && pauseRunnable.size() > 0) {
            searchExecutorService = Executors.newFixedThreadPool(5);
            allRunCount.set(pauseRunnable.size());
            for (Runnable runnable : pauseRunnable) {
                searchExecutorService.execute(runnable);
            }
            pauseRunnable.clear();
            pauseRunnable = null;
        }

        if (hasKeyBoard) {
            String keyWord = searchText.getText().toString().trim();
            if (TextUtils.isEmpty(keyWord)) {
                searchText.requestFocus();
                searchText.requestFocusFromTouch();
            } else {
                tvSearch.requestFocus();
                tvSearch.requestFocusFromTouch();
            }

        } else {
            if (!isSearchBack) {
                tvSearch.requestFocus();
                tvSearch.requestFocusFromTouch();
            }
        }
    }

    private void initView() {
        EventBus.getDefault().register(this);
        llLayout = findViewById(R.id.llLayout);
        searchText = findViewById(R.id.searchText);
        tvSearch = findViewById(R.id.tvSearch);
        tvSearchCheckboxBtn = findViewById(R.id.tvSearchCheckboxBtn);
        keyDeleteAll = findViewById(R.id.keyDeleteAll);
        remoteSearch = findViewById(R.id.remoteSearch);
        keyDelete = findViewById(R.id.keyDelete);
        mGridView = findViewById(R.id.mGridView);
        keyBoard = findViewById(R.id.keyBoardRoot);
        mGridViewWord = findViewById(R.id.mGridViewWord);

        mGridViewWord.setHasFixedSize(true);
        mGridViewWord.setLayoutManager(new V7LinearLayoutManager(this.mContext, 1, false));

        hotKeyAdapter = new SearchKeyAdapter();
        mGridViewWord.setAdapter(hotKeyAdapter);
        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);

        mGridView.setHasFixedSize(true);
        mGridView.setLayoutManager(new V7GridLayoutManager(this.mContext, 3));

        searchAdapter = new SearchAdapter();
        mGridView.setAdapter(searchAdapter);

//        searchText.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                enableKeyboard(SearchActivity.this);
//                openSystemKeyBoard();//再次尝试拉起键盘
//                SearchActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
//            }
//        });

//        searchText.setOnFocusChangeListener(tvSearchFocusChangeListener);

        setLoadSir(llLayout);
    }

    /**
     * 拼音联想
     *
     * @param key
     */
    private void querySimilarText(String key) {
        if (StringUtils.isBlank(key)) {
            return;
        }

//        OkGo.<String>get("https://s.video.qq.com/smartbox")
//                .params("plat", 2)
//                .params("ver", 0)
//                .params("num", 20)
//                .params("otype", "json")
//                .params("query", key)
//                .execute(new AbsCallback<String>() {
//                    @Override
//                    public void onSuccess(Response<String> response) {
//                        try {
//                            ArrayList<String> hots = new ArrayList<>();
//                            String result = response.body();
//                            JsonObject json = JsonParser.parseString(result.substring(result.indexOf("{"), result.lastIndexOf("}") + 1)).getAsJsonObject();
//                            JsonArray itemList = json.get("item").getAsJsonArray();
//                            for (JsonElement ele : itemList) {
//                                JsonObject obj = (JsonObject) ele;
//                                hots.add(obj.get("word").getAsString().trim().replaceAll("<|>|《|》|-", "").split(" ")[0]);
//                            }
//                            hotKeyAdapter.setNewData(hots);
//                        } catch (Throwable th) {
//                            th.printStackTrace();
//                        }
//                    }
//
//                    @Override
//                    public String convertResponse(okhttp3.Response response) throws Throwable {
//                        return response.body().string();
//                    }
//                });
        OkGo.<String>get("https://suggest.video.iqiyi.com/")
                .params("if", "mobile")
                .params("key", key)
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        ArrayList<String> hots = new ArrayList<>();
                        String result = response.body();
                        JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                        JsonArray itemList = json.get("data").getAsJsonArray();
                        for (JsonElement ele : itemList) {
                            JsonObject obj = (JsonObject) ele;
                            hots.add(obj.get("name").getAsString().trim().replaceAll("<|>|《|》|-", ""));
                        }
                        hotKeyAdapter.setNewData(hots);
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });
    }

    private void initData() {
        initCheckedSourcesForSearch();
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("title")) {
            String title = intent.getStringExtra("title");
            showLoading();
            if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                jumpActivity(FastSearchActivity.class, bundle);
            } else {
                search(title);
            }
        }

        // 加载热词
        OkGo.<String>get("https://node.video.qq.com/x/api/hot_search")
//        OkGo.<String>get("https://api.web.360kan.com/v1/rank")
//                .params("cat", "1")
                .params("channdlId", "0")
                .params("_", System.currentTimeMillis())
                .execute(new AbsCallback<String>() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        try {
                            ArrayList<String> hots = new ArrayList<>();
                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonObject().get("mapResult").getAsJsonObject().get("0").getAsJsonObject().get("listInfo").getAsJsonArray();
//                            JsonArray itemList = JsonParser.parseString(response.body()).getAsJsonObject().get("data").getAsJsonArray();
                            for (JsonElement ele : itemList) {
                                JsonObject obj = (JsonObject) ele;
                                String hotKey = obj.get("title").getAsString().trim().replaceAll("<|>|《|》|-", "").split(" ")[0];
                                // 去重
                                if (!hots.contains(hotKey)) {
                                    hots.add(hotKey);
                                }
                            }
                            hotKeyAdapter.setNewData(hots);
                        } catch (Throwable th) {
                            th.printStackTrace();
                        }
                    }

                    @Override
                    public String convertResponse(okhttp3.Response response) throws Throwable {
                        return response.body().string();
                    }
                });

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void server(ServerEvent event) {
        if (event.type == ServerEvent.SERVER_SEARCH) {
            String title = (String) event.obj;
            showLoading();
            if (Hawk.get(HawkConfig.FAST_SEARCH_MODE, false)) {
                Bundle bundle = new Bundle();
                bundle.putString("title", title);
                jumpActivity(FastSearchActivity.class, bundle);
            } else {
                search(title);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_SEARCH_RESULT) {
            try {
                searchData(event.obj == null ? null : (AbsXml) event.obj);
            } catch (Exception e) {
                searchData(null);
            }
        }
    }

    private void initCheckedSourcesForSearch() {
        mCheckSources = SearchHelper.getSourcesForSearch();
    }

    public static void setCheckedSourcesForSearch(HashMap<String, String> checkedSources) {
        mCheckSources = checkedSources;
    }

    private void search(String title) {
        cancel();
        if (remoteDialog != null) {
            remoteDialog.dismiss();
            remoteDialog = null;
        }
        showLoading();
        searchText.setText(title);
        this.searchTitle = title;
        mGridView.setVisibility(View.INVISIBLE);
        searchAdapter.setNewData(new ArrayList<>());
        searchResult();
    }

    private ExecutorService searchExecutorService = null;
    private AtomicInteger allRunCount = new AtomicInteger(0);

    private void searchResult() {
        try {
            if (searchExecutorService != null) {
                searchExecutorService.shutdownNow();
                searchExecutorService = null;
                JsLoader.load();
            }
        } catch (Throwable th) {
            th.printStackTrace();
        } finally {
            searchAdapter.setNewData(new ArrayList<>());
            allRunCount.set(0);
        }
        searchExecutorService = Executors.newFixedThreadPool(5);
        List<SourceBean> searchRequestList = new ArrayList<>();
        searchRequestList.addAll(ApiConfig.get().getSourceBeanList());
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        searchRequestList.remove(home);
        searchRequestList.add(0, home);

        ArrayList<String> siteKey = new ArrayList<>();
        for (SourceBean bean : searchRequestList) {
            if (!bean.isSearchable()) {
                continue;
            }
            if (mCheckSources != null && !mCheckSources.containsKey(bean.getKey())) {
                continue;
            }
            siteKey.add(bean.getKey());
            allRunCount.incrementAndGet();
        }
        if (siteKey.size() <= 0) {
            Toast.makeText(mContext, "没有指定搜索源", Toast.LENGTH_SHORT).show();
            showEmpty();
            return;
        }
        for (String key : siteKey) {
            searchExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    sourceViewModel.gsearchText(key, searchTitle);
                }
            });
        }
    }

    private boolean matchSearchResult(String name, String searchTitle) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(searchTitle)) return false;
        searchTitle = searchTitle.trim();
        String[] arr = searchTitle.split("\\s+");
        int matchNum = 0;
        for (String one : arr) {
            if (name.contains(one)) matchNum++;
        }
        return matchNum == arr.length ? true : false;
    }

    private void searchData(AbsXml absXml) {
        if (absXml != null && absXml.movie != null && absXml.movie.videoList != null && absXml.movie.videoList.size() > 0) {
            List<Movie.Video> data = new ArrayList<>();
            for (Movie.Video video : absXml.movie.videoList) {
                if (matchSearchResult(video.name, searchTitle)) data.add(video);
            }
            if (searchAdapter.getData().size() > 0) {
                searchAdapter.addData(data);
            } else {
                closeLoading();
                mGridView.setVisibility(View.VISIBLE);
                searchAdapter.setNewData(data);
            }
        }

        int count = allRunCount.decrementAndGet();
        if (count <= 0) {
            if (searchAdapter.getData().size() <= 0) {
                showEmpty();
            }
            cancel();
        }
    }


    private void cancel() {
        OkGo.getInstance().cancelTag("search");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel();

        if (searchExecutorService != null) {
            searchExecutorService.shutdownNow();
            searchExecutorService = null;
            JsLoader.load();
        }

        EventBus.getDefault().unregister(this);
    }
}
