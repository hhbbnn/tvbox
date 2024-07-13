package com.github.tvbox.osc.ui.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.SourceBean;
import com.github.tvbox.osc.constans.SystemConstants;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.server.ControlManager;
import com.github.tvbox.osc.ui.adapter.HomeMenuAdapter;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.animate.ViewAnimate;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.TipDialog;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.fragment.UserFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.AppManager;
import com.github.tvbox.osc.util.DefaultConfig;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.LocalIPAddress;
import com.github.tvbox.osc.viewmodel.SourceViewModel;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7GridLayoutManager;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class HomeActivity extends BaseActivity implements View.OnClickListener {
    private LinearLayout tvLive;
    private LinearLayout tvSearch;
    private LinearLayout tvSetting;
    private LinearLayout tvPush;

    private LinearLayout contentLayout;
    private TextView tvDate;
    private TextView tvName;

    private SourceViewModel sourceViewModel;
    private TvRecyclerView menuView;
    private NoScrollViewPager videoView;
    private HomeMenuAdapter menuAdapter;
    private View currentView;
    private List<BaseLazyFragment> fragments = new ArrayList<>();

    // 主页菜单
    private int menuSelected = 2;
    private int menuFocused = 2;
    public View menuFocusView = null;
    private Handler handler = new Handler();
    private long mExitTime = 0;
    private Runnable timeRunnable = null;

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_home;
    }

    boolean useCacheConfig = false;

    @Override
    protected void init() {
        EventBus.getDefault().register(this);
        ControlManager.get().startServer();

        initView();
        initData();
        initListeningEvents();
    }

    private void initView() {
        tvLive = findViewById(R.id.tvLive);
        tvSearch = findViewById(R.id.tvSearch);
        tvSetting = findViewById(R.id.tvSetting);
        tvPush = findViewById(R.id.tvPush);

        tvLive.setOnClickListener(this);
        tvSearch.setOnClickListener(this);
        tvSetting.setOnClickListener(this);
        tvPush.setOnClickListener(this);

        tvLive.setOnFocusChangeListener(focusChangeListener);
        tvSearch.setOnFocusChangeListener(focusChangeListener);
        tvSetting.setOnFocusChangeListener(focusChangeListener);
        tvPush.setOnFocusChangeListener(focusChangeListener);

        this.tvDate = findViewById(R.id.tvDate);
        this.tvName = findViewById(R.id.tvName);
        this.contentLayout = findViewById(R.id.contentLayout);
        this.videoView = findViewById(R.id.videoView);

        this.menuView = findViewById(R.id.menuView);
        this.menuAdapter = new HomeMenuAdapter();
        this.menuView.setLayoutManager(new V7LinearLayoutManager(this.mContext, 0, false));
        this.menuView.setSpacingWithMargins(0, AutoSizeUtils.dp2px(this.mContext, 10.0f));
        this.menuView.setAdapter(this.menuAdapter);

        setLoadSir(this.contentLayout);
        //handler.postDelayed(mFindFocus, 500);
    }

    private void initData() {
        // 判断网络
        if (!LocalIPAddress.isNetworkAvailable(this)) {
            Toast.makeText(HomeActivity.this, "网络连接失败，请检查网络连接", Toast.LENGTH_SHORT).show();
            closeLoading();
        }

        Intent intent = getIntent();
        if (intent != null && intent.getExtras() != null) {
            Bundle bundle = intent.getExtras();
            useCacheConfig = bundle.getBoolean("useCache", false);
        }

        this.initTime();
        this.initMenu();
        this.check();
        this.initVideoList();
    }

    /**
     * 初始化监听
     */
    private void initListeningEvents() {
        changedMenu();
//        changedSource();
    }


    /**
     * 初始化时间
     */
    private void initTime() {
        timeRunnable = new Runnable() {
            @Override
            public void run() {
                Date date = new Date();
                // 动态获取系统默认的地区设置
                Locale locale = Locale.getDefault();
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE HH:mm", locale);
                tvDate.setText(dateFormat.format(date).replace("星期", "周"));
                handler.postDelayed(this, 1000);
            }
        };
    }

    private void check() {
        // 判断权限
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(HomeActivity.this, "无权限：向设备的存储器写入数据", Toast.LENGTH_SHORT).show();
        }
    }

    private void initLogo() {
        /**
         * 一：获取首页数据源信息
         */
        SourceBean home = ApiConfig.get().getHomeSourceBean();
        // 设置Logo名称
        if (!StringUtils.isBlank(home.getName())) {
            tvName.setText(com.github.tvbox.osc.util.StringUtils.replaceSymbolsWithPoint(home.getName()));
        }
    }

    private void initMenu() {
        List<MovieSort.SortData> menuList = new ArrayList<>();
        menuList.add(new MovieSort.SortData("menuCollection", "收藏", R.id.tvFilterCollect));
        menuList.add(new MovieSort.SortData("menuHistory", "观看历史", R.id.tvFilterHistory));
        menuList.add(new MovieSort.SortData("menuHome", "精选", R.id.tvFilterHome));
        menuAdapter.setNewData(menuList);

        sourceViewModel = new ViewModelProvider(this).get(SourceViewModel.class);
        sourceViewModel.sortResult.observe(this, new Observer<AbsSortXml>() {
            @Override
            public void onChanged(AbsSortXml absXml) {
                List<MovieSort.SortData> sortList = (absXml == null || absXml.classes == null) ? new ArrayList<>() : absXml.classes.sortList;
                // 设置动态菜单列表数据
                DefaultConfig.setSourceMenuList(ApiConfig.get().getHomeSourceBean().getKey(), sortList, menuList);
                menuAdapter.setNewData(menuList);

                for (MovieSort.SortData data : menuAdapter.getData()) {
                    if (data.id.equals("menuHome")) {
                        if (Hawk.get(HawkConfig.HOME_REC, 0) == SystemConstants.Setting.HomeRecType.SOURCE_REC.getCode() && absXml != null && absXml.videoList != null && absXml.videoList.size() > 0) {
                            fragments.add(UserFragment.newInstance(absXml.videoList));
                        } else {
                            fragments.add(UserFragment.newInstance(null));
                        }
                    } else {
                        fragments.add(GridFragment.newInstance(data));
                    }
                }

                HomePageAdapter pageAdapter = new HomePageAdapter(getSupportFragmentManager(), fragments);
                videoView.setPageTransformer(true, new DefaultTransformer());
                videoView.setAdapter(pageAdapter);
//                videoView.setCurrentItem(menuSelected, false);
                menuView.setSelection(menuSelected);
            }
        });
    }

    /**
     * 一：获取首页数据源：Y：设置Logo名称
     * 二：加载远程配置
     * 三：加载配置里的jar配置
     * 四：根据数据源获取首页列表信息
     */
    private void initVideoList() {
        showLoading();
        /**
         * 二：加载远程配置
         */
        ApiConfig.get().loadConfig(useCacheConfig, new ApiConfig.LoadConfigCallback() {
            @Override
            public void retry() {
                initVideoList();
            }

            @Override
            public void success() {
                if (StringUtils.isBlank(ApiConfig.get().getSpider())) {
                    Toast.makeText(HomeActivity.this, "配置加载成功，但spider是空", Toast.LENGTH_SHORT).show();
                }

                /**
                 * 三：加载logo、加载jar
                 */
                initLogo();
                loadJAR();
            }

            TipDialog dialog = null;

            @Override
            public void error(String msg) {
                if (msg.equalsIgnoreCase("-1")) {
                    Toast.makeText(HomeActivity.this, "未配置数据源", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog = new TipDialog(HomeActivity.this, msg, "重试", "取消", new TipDialog.OnListener() {
                    @Override
                    public void left() {
                        initVideoList();
                        dialog.hide();
                    }

                    @Override
                    public void right() {
                        dialog.hide();
                    }

                    @Override
                    public void cancel() {
                        dialog.hide();
                    }
                });

//                if (!dialog.isShowing()) {
                dialog.show();
            }
        });
    }

    private void loadJAR() {
        ApiConfig.get().loadJAR(useCacheConfig, ApiConfig.get().getSpider(), new ApiConfig.LoadConfigCallback() {
            @Override
            public void success() {
                if (!useCacheConfig) {
                    Toast.makeText(HomeActivity.this, "Jar加载成功", Toast.LENGTH_SHORT).show();
                }

                /**
                 * 四：根据数据源获取首页列表信息
                 */
                sourceViewModel.getVideoList(ApiConfig.get().getHomeSourceBean().getKey());

                closeLoading();
            }

            @Override
            public void retry() {
            }

            @Override
            public void error(String msg) {
                Toast.makeText(HomeActivity.this, msg, Toast.LENGTH_SHORT).show();
                closeLoading();
            }
        });
    }

    private void changedMenu() {
        this.menuView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            /**
             * 上一次选中
             * @param tvRecyclerView
             * @param view
             * @param position
             */
            public void onItemPreSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                TextView textView = view.findViewById(R.id.tvTitle);
                textView.setTextColor(HomeActivity.this.getResources().getColor(R.color.color_BBFFFFFF));
                // 图标隐藏
                view.findViewById(R.id.tvFilter).setVisibility(View.GONE);
                view.findViewById(R.id.tvFilterActive).setVisibility(View.GONE);
                hideMenuIcon(menuAdapter.getItem(position));
            }

            public void onItemSelected(TvRecyclerView tvRecyclerView, View view, int position) {
//                Toast.makeText(mContext, "changedMenu.onItemSelected:" + position, Toast.LENGTH_SHORT).show();
                HomeActivity.this.currentView = view;
                TextView textView = view.findViewById(R.id.tvTitle);
                ViewAnimate.animateHomeMenuFocus(view);
                textView.setTextColor(HomeActivity.this.getResources().getColor(R.color.color_FFFFFF));
                showMenuIcon(menuAdapter.getItem(position));

                if (position == 0 || position == 1) {
//                    Toast.makeText(HomeActivity.this, "按OK键进入", Toast.LENGTH_SHORT).show();
                    return;
                }

                HomeActivity.this.menuFocusView = view;
                HomeActivity.this.menuFocused = position;
                HomeActivity.this.menuSelected = position;
                videoView.setCurrentItem(menuSelected, false);
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
//                Toast.makeText(mContext, "changedMenu.onItemClick:" + position, Toast.LENGTH_SHORT).show();
                if (position == 0) {
                    jumpActivity(CollectActivity.class);
                    return;
                }
                if (position == 1) {
                    jumpActivity(HistoryActivity.class);
                    return;
                }

                if (itemView != null && menuSelected == position) {
                    BaseLazyFragment baseLazyFragment = fragments.get(menuSelected);
                    if ((baseLazyFragment instanceof GridFragment) && !menuAdapter.getItem(position).filters.isEmpty()) {// 弹出筛选
                        ((GridFragment) baseLazyFragment).showFilter();
                    } else if (baseLazyFragment instanceof UserFragment) {
                        showSiteSwitch();
                    }
                }
            }
        });

        this.menuView.setOnInBorderKeyEventListener(new TvRecyclerView.OnInBorderKeyEventListener() {
            public final boolean onInBorderKeyEvent(int direction, View view) {
                if (direction != View.FOCUS_DOWN) {
                    return false;
                }

                BaseLazyFragment baseLazyFragment = fragments.get(menuFocused);
                if (!(baseLazyFragment instanceof GridFragment)) {
                    return false;
                }
                if (!((GridFragment) baseLazyFragment).isLoad()) {
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        int i;
        if (this.fragments.size() <= 0 || this.menuFocused >= this.fragments.size() || (i = this.menuFocused) < 0) {
            exit();
            return;
        }
        BaseLazyFragment baseLazyFragment = this.fragments.get(i);
        if (baseLazyFragment instanceof GridFragment) {
            View view = this.menuFocusView;
            GridFragment grid = (GridFragment) baseLazyFragment;
            if (grid.restoreView()) {
                return;
            }// 还原上次保存的UI内容
            if (view != null && !view.isFocused()) {
                this.menuFocusView.requestFocus();
            } else if (this.menuFocused != 2) {
                this.menuView.setSelection(2);
            } else {
                exit();
            }
        } else if (baseLazyFragment instanceof UserFragment && UserFragment.videoHotList.canScrollVertically(-1)) {
            UserFragment.videoHotList.scrollToPosition(2);
            this.menuView.setSelection(2);
        } else {
            exit();
        }
    }

    private void exit() {
        if (System.currentTimeMillis() - mExitTime < 2000) {
            //这一段借鉴来自 q群老哥 IDCardWeb
            EventBus.getDefault().unregister(this);
            AppManager.getInstance().appExit(0);
            ControlManager.get().stopServer();
            finish();
            super.onBackPressed();
        } else {
            mExitTime = System.currentTimeMillis();
            Toast.makeText(mContext, "再按一次返回键退出应用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(timeRunnable);
    }


    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_PUSH_URL) {
            if (ApiConfig.get().getSource("push_agent") != null) {
                Intent newIntent = new Intent(mContext, DetailActivity.class);
                newIntent.putExtra("id", (String) event.obj);
                newIntent.putExtra("sourceKey", "push_agent");
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                HomeActivity.this.startActivity(newIntent);
            }
        } else if (event.type == RefreshEvent.TYPE_FILTER_CHANGE) {
        }
    }

    private void showMenuIcon(MovieSort.SortData menuData) {
        if (null == menuData) {
            return;
        }

        // 如果自带图标
        if (menuData.iconResId != null) {
            currentView.findViewById(menuData.iconResId).setVisibility(View.VISIBLE);
            return;
        }

        // 通用筛选图标
        boolean visible = menuData.filterSelectCount() > 0;
        currentView.findViewById(R.id.tvFilterActive).setVisibility(visible ? View.VISIBLE : View.GONE);
        currentView.findViewById(R.id.tvFilter).setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void hideMenuIcon(MovieSort.SortData menuData) {
        // 如果自带图标
        if (menuData != null && menuData.iconResId != null) {
            currentView.findViewById(menuData.iconResId).setVisibility(View.GONE);
        }
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_MENU) {
                showSiteSwitch();
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {

        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        AppManager.getInstance().appExit(0);
        ControlManager.get().stopServer();
    }

    void showSiteSwitch() {
        List<SourceBean> sites = ApiConfig.get().getSourceBeanList();
        if (sites.size() == 0) {
            Toast.makeText(HomeActivity.this, "没有数据源", Toast.LENGTH_SHORT).show();
            return;
        }

        SelectDialog<SourceBean> dialog = new SelectDialog<>(HomeActivity.this);
        TvRecyclerView tvRecyclerView = dialog.findViewById(R.id.list);
        int spanCount;
        spanCount = (int) Math.floor(sites.size() / 60);
        spanCount = Math.min(spanCount, 2);
        tvRecyclerView.setLayoutManager(new V7GridLayoutManager(dialog.getContext(), spanCount + 1));
        ConstraintLayout cl_root = dialog.findViewById(R.id.cl_root);
        ViewGroup.LayoutParams clp = cl_root.getLayoutParams();
        clp.width = AutoSizeUtils.mm2px(dialog.getContext(), 380 + 200 * spanCount);
        dialog.setTip("请选择首页数据源");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<SourceBean>() {
            @Override
            public void click(SourceBean value, int pos) {
                ApiConfig.get().setSourceBean(value);
                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                Bundle bundle = new Bundle();
                bundle.putBoolean("useCache", true);
                intent.putExtras(bundle);
                HomeActivity.this.startActivity(intent);
            }

            @Override
            public String getDisplay(SourceBean val) {
                return val.getName();
            }
        }, new DiffUtil.ItemCallback<SourceBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull @NotNull SourceBean oldItem, @NonNull @NotNull SourceBean newItem) {
                return oldItem.getKey().equals(newItem.getKey());
            }
        }, sites, sites.indexOf(ApiConfig.get().getHomeSourceBean()));
        dialog.show();
    }

    private View.OnFocusChangeListener focusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (hasFocus) {
                ViewAnimate.animateHomeToolFocus(view);
            }
        }
    };

    @Override
    public void onClick(View v) {
        FastClickCheckUtil.check(v);

        if (v.getId() == R.id.tvLive) {
            jumpActivity(LivePlayActivity.class);
        } else if (v.getId() == R.id.tvSearch) {
            jumpActivity(SearchActivity.class);
        } else if (v.getId() == R.id.tvSetting) {
            jumpActivity(SettingActivity.class);
        } else if (v.getId() == R.id.tvPush) {
            jumpActivity(PushActivity.class);
        }
    }
}
