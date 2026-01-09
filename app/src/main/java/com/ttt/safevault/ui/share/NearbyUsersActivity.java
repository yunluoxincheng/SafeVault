package com.ttt.safevault.ui.share;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.ttt.safevault.R;
import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.dto.response.NearbyUserResponse;
import com.ttt.safevault.viewmodel.NearbyUsersViewModel;
import com.ttt.safevault.viewmodel.ViewModelFactory;

import java.util.ArrayList;

/**
 * 附近用户发现界面
 * 显示附近的SafeVault用户并支持选择
 */
public class NearbyUsersActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int DEFAULT_RADIUS_METERS = 1000; // 默认搜索半径1公里
    private static final long HEARTBEAT_INTERVAL_MS = 30000; // 心跳间隔30秒

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private CircularProgressIndicator progressBar;
    private View emptyView;
    private Toolbar toolbar;
    private NearbyUsersViewModel viewModel;
    private NearbyUsersAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    
    private double currentLatitude;
    private double currentLongitude;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_nearby_users);
        
        // 初始化视图
        toolbar = findViewById(R.id.toolbar);
        recyclerView = findViewById(R.id.recycler_view);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        progressBar = findViewById(R.id.progress_bar);
        emptyView = findViewById(R.id.empty_view);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        heartbeatHandler = new Handler(Looper.getMainLooper());

        setupViewModel();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        observeViewModel();
        
        // 检查并请求位置权限
        checkAndRequestLocationPermission();
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
            getApplication(),
            ServiceLocator.getInstance().getBackendService()
        );
        viewModel = new ViewModelProvider(this, factory).get(NearbyUsersViewModel.class);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nearby_users);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new NearbyUsersAdapter(new ArrayList<>(), this::onUserSelected);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::refreshNearbyUsers);
    }

    private void observeViewModel() {
        // 观察加载状态
        viewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                if (isLoading && !swipeRefreshLayout.isRefreshing()) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        // 观察附近用户列表
        viewModel.getNearbyUsers().observe(this, users -> {
            if (users != null) {
                adapter.updateUsers(users);
                updateEmptyState(users.isEmpty());
            }
        });

        // 观察位置注册状态
        viewModel.getLocationRegistered().observe(this, registered -> {
            if (registered != null && registered) {
                // 位置注册成功，开始获取附近用户
                getNearbyUsers();
                // 开始心跳
                startHeartbeat();
            }
        });

        // 观察错误信息
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // 权限已授予，获取位置
            getLocationAndRegister();
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限已授予
                getLocationAndRegister();
            } else {
                // 权限被拒绝
                showPermissionDeniedDialog();
            }
        }
    }

    private void showPermissionDeniedDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_required)
            .setMessage(R.string.location_permission_required_message)
            .setPositiveButton(R.string.ok, (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void getLocationAndRegister() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                    
                    // 注册位置到服务器
                    viewModel.registerLocation(currentLatitude, currentLongitude, DEFAULT_RADIUS_METERS);
                } else {
                    Toast.makeText(this, R.string.cannot_get_location, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, R.string.location_error, Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            });
    }

    private void getNearbyUsers() {
        viewModel.getNearbyUsers(currentLatitude, currentLongitude, DEFAULT_RADIUS_METERS);
    }

    private void refreshNearbyUsers() {
        getNearbyUsers();
    }

    private void updateEmptyState(boolean isEmpty) {
        if (isEmpty) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void onUserSelected(NearbyUserResponse user) {
        // 返回选中的用户
        Intent resultIntent = new Intent();
        resultIntent.putExtra("SELECTED_USER_ID", user.getUserId());
        resultIntent.putExtra("SELECTED_USER_NAME", user.getDisplayName());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void startHeartbeat() {
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                viewModel.sendHeartbeat();
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL_MS);
            }
        };
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    private void stopHeartbeat() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopHeartbeat();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopHeartbeat();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentLatitude != 0 && currentLongitude != 0) {
            startHeartbeat();
        }
    }
}
