package com.ttt.safevault.ui.share;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ttt.safevault.R;
import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.databinding.ActivityReceiveShareBinding;
import com.ttt.safevault.dto.response.ReceivedShareResponse;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.model.PasswordShare;
import com.ttt.safevault.model.SharePermission;
import com.ttt.safevault.utils.NFCTransferManager;
import com.ttt.safevault.viewmodel.ReceiveShareViewModel;
import com.ttt.safevault.viewmodel.ViewModelFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 接收分享界面
 * 显示分享的密码并允许保存
 * 支持离线分享和云端分享
 */
public class ReceiveShareActivity extends AppCompatActivity {

    private ActivityReceiveShareBinding binding;
    private ReceiveShareViewModel viewModel;
    private String shareId;
    private boolean isPasswordVisible = false;
    private boolean isCloudShare = false;  // 是否为云端分享
    private String actualPassword = "";
    private NFCTransferManager nfcManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置FLAG_SECURE防止截屏
        getWindow().setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        );

        binding = ActivityReceiveShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 初始化NFC管理器
        nfcManager = new NFCTransferManager(this);

        // 获取分享ID，支持多种方式：
        // 1. 通过Intent Extra传递：getStringExtra("SHARE_ID") 或 "SHARE_TOKEN"
        // 2. 通过URI传递：safevault://share/{shareId} 或 safevault://offline/{data}
        // 3. 通过NFC传递
        shareId = getIntent().getStringExtra("SHARE_ID");
        
        // 尝试从SHARE_TOKEN获取（蓝牙/NFC传输）
        if (shareId == null || shareId.isEmpty()) {
            shareId = getIntent().getStringExtra("SHARE_TOKEN");
        }
        
        // 如果没有Extra，尝试从 URI 中解析
        if (shareId == null || shareId.isEmpty()) {
            android.net.Uri uri = getIntent().getData();
            if (uri != null && "safevault".equals(uri.getScheme())) {
                if ("offline".equals(uri.getHost())) {
                    // 离线分享：safevault://offline/{data}
                    shareId = uri.toString();
                } else if ("share".equals(uri.getHost())) {
                    // 在线分享：/shareId
                    String path = uri.getPath();
                    if (path != null && path.startsWith("/")) {
                        shareId = path.substring(1);
                    }
                }
            }
        }
        
        // 尝试从NFC Intent中获取数据
        if (shareId == null || shareId.isEmpty()) {
            shareId = handleNfcIntent(getIntent());
        }
        
        if (shareId == null || shareId.isEmpty()) {
            Toast.makeText(this, "分享链接无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViewModel();
        setupToolbar();
        setupViews();
        observeViewModel();

        // 检查是否为离线分享
        if (com.ttt.safevault.utils.OfflineShareUtils.isOfflineShare(shareId)) {
            // 离线分享需要密码
            isCloudShare = false;
            showPasswordInputDialog();
        } else {
            // 云端分享，直接请求
            isCloudShare = true;
            viewModel.receiveCloudShare(shareId);
        }
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
            getApplication(),
            ServiceLocator.getInstance().getBackendService()
        );
        viewModel = new ViewModelProvider(this, factory).get(ReceiveShareViewModel.class);
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViews() {
        // 密码显示/隐藏切换
        binding.btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // 拒绝按钮
        binding.btnReject.setOnClickListener(v -> {
            Toast.makeText(this, "已拒绝接收", Toast.LENGTH_SHORT).show();
            finish();
        });

        // 保存到本地按钮
        binding.btnSaveToLocal.setOnClickListener(v -> saveToLocal());
    }

    private void observeViewModel() {
        // 观察加载状态
        viewModel.isLoading.observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            if (!isLoading) {
                binding.scrollView.setVisibility(View.VISIBLE);
            }
        });

        // 观察错误信息
        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                viewModel.clearError();
                finish();
            }
        });

        // 观察分享的密码
        viewModel.sharedPassword.observe(this, passwordItem -> {
            if (passwordItem != null) {
                displayPasswordItem(passwordItem);
            }
        });

        // 观察分享详情（离线分享）
        viewModel.shareDetails.observe(this, shareDetails -> {
            if (shareDetails != null) {
                displayShareDetails(shareDetails);
            }
        });

        // 观察云端分享详情
        viewModel.cloudShareDetails.observe(this, cloudShareResponse -> {
            if (cloudShareResponse != null) {
                displayCloudShareDetails(cloudShareResponse);
            }
        });

        // 观察保存成功
        viewModel.saveSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "已保存到密码库", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayPasswordItem(PasswordItem item) {
        binding.textTitle.setText(item.getTitle() != null ? item.getTitle() : "无标题");
        binding.textUsername.setText(item.getUsername() != null ? item.getUsername() : "无用户名");
        
        // 保存实际密码
        actualPassword = item.getPassword() != null ? item.getPassword() : "";
        binding.textPassword.setText("••••••••");

        // URL
        if (item.getUrl() != null && !item.getUrl().isEmpty()) {
            binding.labelUrl.setVisibility(View.VISIBLE);
            binding.textUrl.setVisibility(View.VISIBLE);
            binding.textUrl.setText(item.getUrl());
        } else {
            binding.labelUrl.setVisibility(View.GONE);
            binding.textUrl.setVisibility(View.GONE);
        }
    }

    private void displayShareDetails(PasswordShare share) {
        // 显示分享者（这里需要从UserProfile获取，暂时显示ID）
        binding.textSharer.setText(share.getFromUserId() != null ? 
            share.getFromUserId() : "未知用户");

        // 显示过期时间
        if (share.getExpireTime() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String expireDate = sdf.format(new Date(share.getExpireTime()));
            long remainingDays = (share.getExpireTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
            binding.textExpireTime.setText("有效期：" + remainingDays + "天后过期 (" + expireDate + ")");
        } else {
            binding.textExpireTime.setText("有效期：永久有效");
        }

        // 显示权限
        SharePermission permission = share.getPermission();
        if (permission != null) {
            StringBuilder permText = new StringBuilder("权限：");
            if (permission.isCanView()) {
                permText.append("可查看");
            }
            if (permission.isCanSave()) {
                permText.append("、可保存");
            }
            if (permission.isRevocable()) {
                permText.append("、可撤销");
            }
            binding.textPermissions.setText(permText.toString());

            // 根据权限控制保存按钮
            binding.btnSaveToLocal.setEnabled(permission.isCanSave());
        }
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            binding.textPassword.setText(actualPassword);
            binding.btnTogglePassword.setText("隐藏");
        } else {
            binding.textPassword.setText("••••••••");
            binding.btnTogglePassword.setText("显示");
        }
    }

    private void saveToLocal() {
        if (shareId != null) {
            if (isCloudShare) {
                viewModel.saveCloudShare(shareId);
            } else {
                viewModel.saveSharedPassword(shareId);
            }
        }
    }

    /**
     * 显示密码输入对话框（用于离线分享）
     */
    private void showPasswordInputDialog() {
        // 创建输入框
        EditText input = new EditText(this);
        input.setHint(getString(R.string.enter_share_password));
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding, padding, padding);

        new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.share_password_required)
            .setView(input)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                String password = input.getText().toString();
                if (password.isEmpty()) {
                    Toast.makeText(this, "请输入分享密码", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    viewModel.receiveOfflineShare(shareId, password);
                }
            })
            .setNegativeButton(R.string.cancel, (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
    
    /**
     * 处理NFC Intent
     * @param intent 从 NFC 获取的 Intent
     * @return 解析出的分享数据，失败返回null
     */
    private String handleNfcIntent(Intent intent) {
        String action = intent.getAction();
        
        // 检查是否是NDEF发现事件
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMessages != null && rawMessages.length > 0) {
                NdefMessage message = (NdefMessage) rawMessages[0];
                String data = nfcManager.extractDataFromMessage(message);
                
                if (data != null && !data.isEmpty()) {
                    Toast.makeText(this, R.string.nfc_read_success, Toast.LENGTH_SHORT).show();
                    return data;
                } else {
                    Toast.makeText(this, R.string.nfc_read_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
        
        return null;
    }

    /**
     * 显示云端分享详情
     */
    private void displayCloudShareDetails(ReceivedShareResponse response) {
        // 显示密码信息
        binding.textTitle.setText(response.getTitle() != null ? response.getTitle() : "无标题");
        binding.textUsername.setText(response.getUsername() != null ? response.getUsername() : "无用户名");
        
        // 保存实际密码
        actualPassword = response.getDecryptedPassword() != null ? response.getDecryptedPassword() : "";
        binding.textPassword.setText("••••••••");

        // URL
        if (response.getUrl() != null && !response.getUrl().isEmpty()) {
            binding.labelUrl.setVisibility(View.VISIBLE);
            binding.textUrl.setVisibility(View.VISIBLE);
            binding.textUrl.setText(response.getUrl());
        } else {
            binding.labelUrl.setVisibility(View.GONE);
            binding.textUrl.setVisibility(View.GONE);
        }

        // 显示分享者
        binding.textSharer.setText(response.getFromUserDisplayName() != null ? 
            response.getFromUserDisplayName() : response.getFromUserId());

        // 显示过期时间
        if (response.getExpireTime() != null && response.getExpireTime() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String expireDate = sdf.format(new Date(response.getExpireTime()));
            long remainingDays = (response.getExpireTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
            binding.textExpireTime.setText("有效期：" + remainingDays + "天后过期 (" + expireDate + ")");
        } else {
            binding.textExpireTime.setText("有效期：永久有效");
        }

        // 显示权限
        SharePermission permission = response.getPermission();
        if (permission != null) {
            StringBuilder permText = new StringBuilder("权限：");
            if (permission.isCanView()) {
                permText.append("可查看");
            }
            if (permission.isCanSave()) {
                permText.append("、可保存");
            }
            if (permission.isRevocable()) {
                permText.append("、可撤销");
            }
            binding.textPermissions.setText(permText.toString());

            // 根据权限控制保存按钮
            binding.btnSaveToLocal.setEnabled(permission.isCanSave());
        }

        // 显示分享类型
        String shareType = response.getShareType();
        if (shareType != null) {
            String typeText = "";
            switch (shareType) {
                case "DIRECT":
                    typeText = "直接分享";
                    break;
                case "USER_TO_USER":
                    typeText = "用户对用户";
                    break;
                case "NEARBY":
                    typeText = "附近用户";
                    break;
            }
            if (!typeText.isEmpty()) {
                binding.textShareType.setVisibility(View.VISIBLE);
                binding.textShareType.setText("分享方式：" + typeText);
            }
        }
    }
}
