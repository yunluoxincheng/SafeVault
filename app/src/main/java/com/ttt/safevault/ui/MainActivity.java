package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ttt.safevault.R;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.utils.SearchHistoryManager;
import com.ttt.safevault.viewmodel.PasswordListViewModel;

/**
 * 主Activity
 * 作为应用的主容器，承载各个Fragment，支持底部导航
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private PasswordListViewModel listViewModel;
    private BackendService backendService;
    private SearchHistoryManager searchHistoryManager;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabAddPassword;

    // Search debounce handler
    private Handler searchDebounceHandler;
    private Runnable searchDebounceRunnable;
    private static final int SEARCH_DEBOUNCE_DELAY_MS = 150; // 150ms debounce delay

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 防止截图
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);

        // 获取BackendService实例
        backendService = com.ttt.safevault.ServiceLocator.getInstance().getBackendService();

        // 初始化搜索 debounce handler
        searchDebounceHandler = new Handler(Looper.getMainLooper());

        // 初始化搜索历史管理器
        searchHistoryManager = SearchHistoryManager.getInstance(this);

        initNavigation();
        initToolbar();
        initBottomNavigation();
        initFab();
        initViewModel();
        
        // 处理从自动填充返回的意图
        handleAutofillIntent();
    }

    private void initNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // 设置顶级目的地（底部导航的四个选项卡）
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.nav_passwords,
                    R.id.nav_share_history,
                    R.id.nav_generator,
                    R.id.nav_settings
            ).build();
        }
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (navController != null) {
            // NavigationUI 会自动根据导航图配置 toolbar
            NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration);
        }

        // 监听导航变化，动态设置标题和菜单
        if (navController != null) {
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // 清除并重新创建菜单
                invalidateOptionsMenu();

                // 动态设置编辑页面的标题
                if (destination.getId() == R.id.editPasswordFragment && arguments != null) {
                    int passwordId = arguments.getInt("passwordId", -1);
                    boolean isNew = passwordId == -1;
                    String title = isNew ? "添加密码" : "编辑密码";
                    toolbar.setTitle(title);
                }
            });
        }
    }

    private void initBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null && navController != null) {
            // 设置底部导航与 NavController 的关联
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // 监听导航变化，控制底部导航显示
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // 只在顶级目的地显示底部导航
                int destinationId = destination.getId();
                boolean isTopLevelDestination = destinationId == R.id.nav_passwords
                        || destinationId == R.id.nav_share_history
                        || destinationId == R.id.nav_generator
                        || destinationId == R.id.nav_settings;

                bottomNavigationView.setVisibility(isTopLevelDestination ? View.VISIBLE : View.GONE);
            });
        }
    }

    private void initFab() {
        fabAddPassword = findViewById(R.id.fab_add_password);
        if (fabAddPassword != null && navController != null) {
            fabAddPassword.setOnClickListener(v -> {
                // 导航到编辑密码页面（新建模式）
                navController.navigate(R.id.action_passwordListFragment_to_editPasswordFragment);
            });

            // 监听导航变化，只在密码库页面显示 FAB
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                int destinationId = destination.getId();
                boolean shouldShowFab = destinationId == R.id.nav_passwords;

                if (shouldShowFab) {
                    fabAddPassword.show();
                } else {
                    fabAddPassword.hide();
                }
            });
        }
    }

    private void initViewModel() {
        // 通过ViewModelFactory获取ViewModel
        ViewModelProvider.Factory factory = new com.ttt.safevault.viewmodel.ViewModelFactory(getApplication());
        listViewModel = new ViewModelProvider(this, factory).get(PasswordListViewModel.class);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 根据当前页面决定显示哪些菜单项
        int currentDestinationId = 0;
        if (navController != null) {
            currentDestinationId = navController.getCurrentDestination().getId();
        }

        // 只在密码库页面显示搜索
        if (currentDestinationId == R.id.nav_passwords) {
            // 检查菜单是否已经存在，避免重复添加
            if (menu.findItem(R.id.action_search) == null) {
                getMenuInflater().inflate(R.menu.main_menu, menu);

                // 设置搜索菜单项
                MenuItem searchItem = menu.findItem(R.id.action_search);
                if (searchItem != null) {
                    androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
                    if (searchView != null) {
                        setupSearchView(searchView);
                    }
                }
            }
        } else {
            // 非密码库页面，清空菜单
            menu.clear();
        }

        return true;
    }

    private void setupSearchView(androidx.appcompat.widget.SearchView searchView) {
        searchView.setQueryHint("搜索密码");

        // 设置搜索建议（从搜索历史）
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && searchHistoryManager != null) {
                // 可以在这里显示搜索建议
                // SearchView 不直接支持下拉建议，需要自定义实现
            }
        });

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 取消待执行的 debounce 搜索
                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }

                if (listViewModel != null) {
                    listViewModel.search(query);
                }

                // 添加到搜索历史
                if (searchHistoryManager != null && query != null && !query.trim().isEmpty()) {
                    searchHistoryManager.addSearchQuery(query.trim());
                }

                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 取消之前的搜索任务
                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }

                // 创建新的延迟搜索任务
                searchDebounceRunnable = () -> {
                    if (listViewModel != null) {
                        listViewModel.search(newText);
                    }
                };

                // 延迟执行搜索（debounce）
                searchDebounceHandler.postDelayed(searchDebounceRunnable, SEARCH_DEBOUNCE_DELAY_MS);

                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            try {
                // 取消待执行的 debounce 搜索
                if (searchDebounceRunnable != null) {
                    searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
                }

                // 清除搜索
                if (listViewModel != null) {
                    listViewModel.clearSearch();
                }
            } catch (Exception e) {
                // 忽略异常，防止闪退
            }
            return true;
        });

        // 设置搜索历史（最近搜索）
        if (searchHistoryManager != null && !searchHistoryManager.isEmpty()) {
            List<String> recentSearches = searchHistoryManager.getSearchHistoryQueries();
            if (!recentSearches.isEmpty()) {
                searchView.setQuery(recentSearches.get(0), false);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        // 如果在搜索状态，退出搜索
        Boolean isSearching = listViewModel != null ? listViewModel.isSearching.getValue() : null;
        if (isSearching != null && isSearching) {
            // 取消待执行的 debounce 搜索
            if (searchDebounceRunnable != null) {
                searchDebounceHandler.removeCallbacks(searchDebounceRunnable);
            }

            if (listViewModel != null) {
                listViewModel.clearSearch();
            }
            return;
        }

        // 如果在非顶级页面，正常返回
        if (navController != null && navController.getCurrentDestination() != null) {
            int currentDestinationId = navController.getCurrentDestination().getId();
            boolean isTopLevelDestination = currentDestinationId == R.id.nav_passwords
                    || currentDestinationId == R.id.nav_share_history
                    || currentDestinationId == R.id.nav_generator
                    || currentDestinationId == R.id.nav_settings;

            if (!isTopLevelDestination) {
                super.onBackPressed();
                return;
            }
        }

        // 如果在顶级页面，退出应用
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 应用从后台返回时，检查是否需要重新锁定
        checkAutoLock();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 记录进入后台的时间
        if (backendService != null) {
            backendService.recordBackgroundTime();
        }
    }

    private void checkAutoLock() {
        if (backendService != null) {
            long backgroundTime = backendService.getBackgroundTime();
            int autoLockTimeout = backendService.getAutoLockTimeout(); // 分钟

            if (backgroundTime > 0 && autoLockTimeout > 0) {
                long backgroundMinutes = (System.currentTimeMillis() - backgroundTime) / (60 * 1000);
                if (backgroundMinutes >= autoLockTimeout) {
                    // 超时，需要重新锁定
                    lockApp();
                }
            }
        }
    }

    private void lockApp() {
        if (backendService != null) {
            backendService.lock();
        }

        // 跳转到登录页面
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void handleAutofillIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            boolean isFromAutofill = intent.getBooleanExtra("from_autofill", false);
            String autofillDomain = intent.getStringExtra("autofill_domain");
            
            if (isFromAutofill && autofillDomain != null) {
                // 从自动填充返回，如果有域名参数，导航到密码列表并搜索
                if (navController != null) {
                    // 确保导航到密码列表页面
                    navController.navigate(R.id.nav_passwords);
                    
                    // 等待导航完成后再执行搜索
                    new Handler().postDelayed(() -> {
                        if (listViewModel != null) {
                            listViewModel.search(autofillDomain);
                        }
                    }, 300); // 延迟300毫秒确保页面已加载
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 清理 debounce handler
        if (searchDebounceHandler != null) {
            searchDebounceHandler.removeCallbacksAndMessages(null);
        }
    }
}