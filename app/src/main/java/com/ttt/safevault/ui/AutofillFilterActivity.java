package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.autofill.Dataset;
import android.util.Log;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.ttt.safevault.R;
import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.adapter.PasswordListAdapter;
import com.ttt.safevault.autofill.AutofillResult;
import com.ttt.safevault.data.AppDatabase;
import com.ttt.safevault.data.PasswordDao;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自动填充过滤Activity
 * 当有多个匹配项时显示选择界面
 */
public class AutofillFilterActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN = 1001;
    private ExecutorService executor;

    private RecyclerView recyclerView;
    private PasswordListAdapter adapter;
    private BackendService backendService;
    private AutofillId usernameId;
    private AutofillId passwordId;
    private AutofillManager autofillManager;
    private boolean isFinishing = false;
    private AutofillResult autofillResult;
    private List<PasswordItem> allItems;
    private String domain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autofill_filter);

        // 防止截图
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);

        // 初始化线程池
        executor = Executors.newSingleThreadExecutor();

        // 初始化AutofillManager
        autofillManager = getSystemService(android.view.autofill.AutofillManager.class);

        // 获取BackendService实例
        backendService = ServiceLocator.getInstance().getBackendService();

        initViews();
        initIntentData();
        setupToolbar();
        setupRecyclerView();
        
        // 检查是否已解锁
        checkAndLoad();
    }
    
    private void checkAndLoad() {
        if (backendService == null) {
            Toast.makeText(this, "服务未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 检查是否已解锁
        if (!backendService.isUnlocked()) {
            // 未解锁，跳转到登录界面
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.putExtra("from_autofill", true);
            startActivityForResult(loginIntent, REQUEST_LOGIN);
        } else {
            loadData();
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOGIN) {
            if (resultCode == RESULT_OK) {
                // 登录成功，重新获取 backendService 确保状态同步
                backendService = ServiceLocator.getInstance().getBackendService();
                Log.d("AutofillFilter", "Login success, isUnlocked: " + backendService.isUnlocked());
                loadData();
            } else {
                // 登录取消，关闭界面
                finish();
            }
        }
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view);
    }

    private void initIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            domain = intent.getStringExtra("domain");
            autofillResult = intent.getParcelableExtra("autofillResult");
            
            // 获取自动填充字段ID
            usernameId = intent.getParcelableExtra("usernameId");
            passwordId = intent.getParcelableExtra("passwordId");
            
            Log.d("AutofillFilter", "Received intent data:");
            Log.d("AutofillFilter", "  domain: " + domain);
            Log.d("AutofillFilter", "  usernameId: " + usernameId);
            Log.d("AutofillFilter", "  passwordId: " + passwordId);
            
            // 显示字段ID状态
            String idStatus = "字段ID: 用户名=" + (usernameId != null ? "有" : "无") + 
                            ", 密码=" + (passwordId != null ? "有" : "无");
            Toast.makeText(this, idStatus, Toast.LENGTH_LONG).show();

            // 获取所有匹配的密码项
            if (intent.hasExtra("passwordItems")) {
                ArrayList<PasswordItem> items = intent.getParcelableArrayListExtra("passwordItems");
                if (items != null) {
                    allItems = items;
                    Log.d("AutofillFilter", "  received " + items.size() + " items");
                }
            }
        }
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle("选择要填充的账号");
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void setupRecyclerView() {
        adapter = new PasswordListAdapter(new PasswordListAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PasswordItem item) {
                // 防止重复点击
                if (isFinishing) {
                    return;
                }
                isFinishing = true;
                fillCredentials(item);
            }

            @Override
            public void onItemCopyClick(PasswordItem item) {
                // 不支持在自动填充界面复制
            }

            @Override
            public void onItemEditClick(PasswordItem item) {
                // 不支持在自动填充界面编辑
            }

            @Override
            public void onItemDeleteClick(PasswordItem item) {
                // 不支持在自动填充界面删除
            }

            @Override
            public void onItemLongClick(PasswordItem item) {
                // 不支持长按操作
            }

            @Override
            public void onItemMoreClick(PasswordItem item, android.view.View anchorView) {
                // 不支持更多选项
            }
        }, false); // 禁用动画

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        if (allItems != null && !allItems.isEmpty()) {
            adapter.submitList(allItems);
            return;
        }
        
        if (backendService == null) {
            Toast.makeText(this, "服务未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        boolean unlocked = backendService.isUnlocked();
        if (!unlocked) {
            Toast.makeText(this, "未解锁，请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // 异步加载数据
        Toast.makeText(this, "加载中...", Toast.LENGTH_SHORT).show();
        
        executor.execute(() -> {
            try {
                // 直接查询数据库记录数
                PasswordDao dao = AppDatabase.getInstance(getApplicationContext()).passwordDao();
                int dbCount = dao.getCount();
                
                List<PasswordItem> items = backendService.getAllItems();
                int decryptedCount = items.size();
                
                runOnUiThread(() -> {
                    if (decryptedCount > 0) {
                        allItems = items;
                        adapter.submitList(new java.util.ArrayList<>(items));
                        Toast.makeText(this, "找到 " + decryptedCount + " 个密码", Toast.LENGTH_SHORT).show();
                    } else {
                        // 显示数据库记录数 vs 解密数
                        Toast.makeText(this, "数据库:" + dbCount + " 解密:" + decryptedCount, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void fillCredentials(PasswordItem item) {
        String debugInfo = "填充: " + item.getUsername() + "\n";
        debugInfo += "用户名ID=" + (usernameId != null ? "有" : "无");
        debugInfo += ", 密码ID=" + (passwordId != null ? "有" : "无");
        Toast.makeText(this, debugInfo, Toast.LENGTH_LONG).show();
        
        Log.d("AutofillFilter", "fillCredentials called for: " + item.getUsername());
        Log.d("AutofillFilter", "usernameId: " + usernameId + ", passwordId: " + passwordId);
        
        // 构建 Dataset 返回给自动填充框架
        if (usernameId == null && passwordId == null) {
            Log.e("AutofillFilter", "No field IDs available");
            Toast.makeText(this, "无法填充：未找到字段ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        try {
            // 创建展示视图
            RemoteViews presentation = new RemoteViews(getPackageName(), android.R.layout.simple_list_item_1);
            presentation.setTextViewText(android.R.id.text1, item.getDisplayName());
            
            Dataset.Builder builder = new Dataset.Builder(presentation);
            
            int fieldCount = 0;
            // 填充用户名
            if (usernameId != null && item.getUsername() != null && !item.getUsername().isEmpty()) {
                builder.setValue(usernameId, AutofillValue.forText(item.getUsername()));
                Log.d("AutofillFilter", "Set username: " + item.getUsername());
                fieldCount++;
            }
            
            // 填充密码
            if (passwordId != null && item.getPassword() != null && !item.getPassword().isEmpty()) {
                builder.setValue(passwordId, AutofillValue.forText(item.getPassword()));
                Log.d("AutofillFilter", "Set password: (hidden)");
                fieldCount++;
            }
            
            if (fieldCount == 0) {
                Log.e("AutofillFilter", "No fields to fill");
                Toast.makeText(this, "没有可填充的数据", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            Dataset dataset = builder.build();
            
            // 返回 Dataset 给自动填充框架
            Intent replyIntent = new Intent();
            replyIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset);
            // 添加必要的 flags
            replyIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            setResult(RESULT_OK, replyIntent);
            
            Log.d("AutofillFilter", "Dataset ready with " + fieldCount + " fields, finishing activity");
            Toast.makeText(this, "正在填充 " + fieldCount + " 个字段", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("AutofillFilter", "Fill failed", e);
            Toast.makeText(this, "填充失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        
        // 立即关闭界面，不给其他导航机会
        finish();
        Log.d("AutofillFilter", "Activity finished");
    }

    @Override
    public void onBackPressed() {
        // 用户取消自动填充
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}