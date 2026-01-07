package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ttt.safevault.R;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.utils.SearchHistoryManager;
import com.ttt.safevault.viewmodel.PasswordListViewModel;

/**
 * 主Activity
 * 作为应用的主容器，承载各个Fragment
 */
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private PasswordListViewModel listViewModel;
    private BackendService backendService;
    private SearchHistoryManager searchHistoryManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 防止截图
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);

        // 获取BackendService实例
        backendService = com.ttt.safevault.ServiceLocator.getInstance().getBackendService();

        // 初始化搜索历史管理器
        searchHistoryManager = SearchHistoryManager.getInstance(this);

        initNavigation();
        initToolbar();
        initFab();
        initViewModel();
    }

    private void initNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // 设置顶级目标
            appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.passwordListFragment
            ).build();
        }
    }

    private void initToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (navController != null) {
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        }
    }

    private void initFab() {
        FloatingActionButton fab = findViewById(R.id.fab_add);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                // 导航到添加密码页面
                if (navController != null) {
                    navController.navigate(R.id.action_passwordListFragment_to_editPasswordFragment);
                }
            });
        }
    }

    private void initViewModel() {
        // 通过ViewModelFactory获取ViewModel
        ViewModelProvider.Factory factory = new com.ttt.safevault.viewmodel.ViewModelFactory(getApplication());
        listViewModel = new ViewModelProvider(this, factory).get(PasswordListViewModel.class);

        // 观察搜索状态，控制FAB显示
        listViewModel.isSearching.observe(this, isSearching -> {
            FloatingActionButton fab = findViewById(R.id.fab_add);
            if (fab != null) {
                fab.setVisibility(isSearching ? View.GONE : View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        // 设置搜索菜单项
        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null) {
            androidx.appcompat.widget.SearchView searchView = (androidx.appcompat.widget.SearchView) searchItem.getActionView();
            if (searchView != null) {
                setupSearchView(searchView);
            }
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
                if (listViewModel != null) {
                    listViewModel.search(newText);
                }

                // 可以在这里实现搜索建议功能
                // List<String> suggestions = searchHistoryManager != null ?
                //         searchHistoryManager.getSearchSuggestions(newText) : new ArrayList<>();

                return true;
            }
        });

        searchView.setOnCloseListener(() -> {
            if (listViewModel != null) {
                listViewModel.clearSearch();
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // 导航到设置页面
            if (navController != null) {
                navController.navigate(R.id.action_passwordListFragment_to_settingsFragment);
            }
            return true;
        }

        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        // 如果在搜索状态，退出搜索
        Boolean isSearching = listViewModel.isSearching.getValue();
        if (isSearching != null && isSearching) {
            listViewModel.clearSearch();
            return;
        }

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
}