package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 自动填充过滤Activity
 * 当有多个匹配项时显示选择界面
 */
public class AutofillFilterActivity extends AppCompatActivity {

    private static final int REQUEST_LOGIN = 1001;

    private RecyclerView recyclerView;
    private PasswordListAdapter adapter;
    private BackendService backendService;
    private android.view.autofill.AutofillManager autofillManager;
    private AutofillResult autofillResult;
    private List<PasswordItem> allItems;
    private String domain;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_autofill_filter);

        // 防止截图
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);

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

            // 获取所有匹配的密码项
            if (intent.hasExtra("passwordItems")) {
                ArrayList<PasswordItem> items = intent.getParcelableArrayListExtra("passwordItems");
                if (items != null) {
                    allItems = items;
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
        Log.d("AutofillFilter", "loadData called");
        Log.d("AutofillFilter", "backendService: " + (backendService != null));
        Log.d("AutofillFilter", "isUnlocked: " + (backendService != null && backendService.isUnlocked()));
        
        if (allItems != null && !allItems.isEmpty()) {
            Log.d("AutofillFilter", "Using passed items: " + allItems.size());
            adapter.submitList(allItems);
            return;
        }
        
        if (backendService == null) {
            Log.e("AutofillFilter", "backendService is null");
            Toast.makeText(this, "服务未初始化", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        if (!backendService.isUnlocked()) {
            Log.e("AutofillFilter", "backendService is not unlocked!");
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        try {
            List<PasswordItem> items;
            
            // 如果有域名，先尝试匹配
            if (domain != null && !domain.isEmpty()) {
                items = backendService.getCredentialsForDomain(domain);
                Log.d("AutofillFilter", "Domain matched: " + items.size());
            } else {
                items = new ArrayList<>();
            }
            
            // 如果没有匹配结果，加载所有凭据
            if (items.isEmpty()) {
                items = backendService.getAllItems();
                Log.d("AutofillFilter", "Loading all items: " + items.size());
            }
            
            if (items != null && !items.isEmpty()) {
                allItems = items;
                adapter.submitList(new ArrayList<>(items));  // 创建新列表确保更新
                Log.d("AutofillFilter", "Submitted " + items.size() + " items to adapter");
            } else {
                Log.w("AutofillFilter", "No items found");
                Toast.makeText(this, "没有保存的密码", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("AutofillFilter", "Error loading data", e);
            Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void fillCredentials(PasswordItem item) {
        if (autofillResult != null) {
            // 构建填充响应
            Intent replyIntent = new Intent();
            replyIntent.putExtra("username", item.getUsername());
            replyIntent.putExtra("password", item.getPassword());

            setResult(RESULT_OK, replyIntent);

            // 完成自动填充
            if (autofillManager != null && autofillManager.isEnabled()) {
                try {
                    autofillManager.commit();
                } catch (Exception e) {
                    // 忽略自动填充提交错误
                }
            }
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        // 用户取消自动填充
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}