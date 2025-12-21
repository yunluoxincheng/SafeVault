package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.ttt.safevault.R;
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

        // TODO: 获取BackendService实例
        // backendService = Injector.get().getBackendService();

        initViews();
        initIntentData();
        setupToolbar();
        setupRecyclerView();
        loadData();
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
        }, false); // 禁用动画

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        if (allItems != null) {
            adapter.submitList(allItems);
        } else if (backendService != null && domain != null) {
            // 从后端加载
            try {
                List<PasswordItem> items = backendService.getCredentialsForDomain(domain);
                if (items != null) {
                    allItems = items;
                    adapter.submitList(items);
                }
            } catch (Exception e) {
                finish();
            }
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