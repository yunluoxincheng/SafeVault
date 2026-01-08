package com.ttt.safevault.ui.autofill;

import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ttt.safevault.R;
import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.databinding.ActivityAutofillSaveBinding;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 自动填充保存凭据Activity
 * 当用户在第三方应用输入新凭据时，弹出此界面让用户确认保存
 */
public class AutofillSaveActivity extends AppCompatActivity {
    private static final String TAG = "AutofillSaveActivity";
    
    private ActivityAutofillSaveBinding binding;
    private BackendService backendService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置FLAG_SECURE防止截屏
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        binding = ActivityAutofillSaveBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取BackendService
        backendService = ServiceLocator.getInstance().getBackendService();

        // 设置Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.autofill_save_credential);
        }

        // 获取Intent数据
        String username = getIntent().getStringExtra("username");
        String password = getIntent().getStringExtra("password");
        String domain = getIntent().getStringExtra("domain");
        String packageName = getIntent().getStringExtra("packageName");
        boolean isWeb = getIntent().getBooleanExtra("isWeb", false);

        // 填充表单
        if (username != null) {
            binding.usernameInput.setText(username);
        }

        if (password != null) {
            binding.passwordInput.setText(password);
        }

        // 设置标题（网站名或应用名）
        String title;
        if (isWeb && domain != null) {
            title = domain;
            binding.websiteInput.setText(domain);
        } else if (packageName != null) {
            title = packageName;
            binding.websiteInput.setText("android://" + packageName);
        } else {
            title = "";
        }
        binding.titleInput.setText(title);

        // 保存按钮
        binding.saveButton.setOnClickListener(v -> saveCredential());

        // 取消按钮
        binding.cancelButton.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });
    }

    /**
     * 保存凭据
     */
    private void saveCredential() {
        String title = binding.titleInput.getText().toString().trim();
        String username = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        String website = binding.websiteInput.getText().toString().trim();
        String notes = binding.notesInput.getText().toString().trim();

        // 验证必填字段
        if (username.isEmpty()) {
            binding.usernameInputLayout.setError(getString(R.string.error_username_required));
            return;
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.setError(getString(R.string.error_password_required));
            return;
        }

        // 清除错误提示
        binding.usernameInputLayout.setError(null);
        binding.passwordInputLayout.setError(null);

        // 禁用按钮防止重复点击
        binding.saveButton.setEnabled(false);
        binding.cancelButton.setEnabled(false);

        // 异步保存
        executor.execute(() -> {
            try {
                // 检查是否已解锁
                if (backendService == null || !backendService.isUnlocked()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, R.string.error_app_locked, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                // 创建PasswordItem
                PasswordItem item = new PasswordItem();
                item.setTitle(title.isEmpty() ? username : title);
                item.setUsername(username);
                item.setPassword(password);
                item.setUrl(website);
                item.setNotes(notes);

                // 保存
                int savedId = backendService.saveItem(item);

                runOnUiThread(() -> {
                    if (savedId > 0) {
                        Toast.makeText(this, R.string.autofill_save_success, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                    } else {
                        Toast.makeText(this, R.string.autofill_save_failed, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                    }
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.autofill_save_failed, Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        setResult(RESULT_CANCELED);
        finish();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
