package com.ttt.safevault.ui.share;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.ttt.safevault.R;
import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.databinding.ActivityShareBinding;
import com.ttt.safevault.model.Friend;
import com.ttt.safevault.model.SharePermission;
import com.ttt.safevault.viewmodel.ShareViewModel;
import com.ttt.safevault.viewmodel.ViewModelFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码分享配置界面
 */
public class ShareActivity extends AppCompatActivity {

    private ActivityShareBinding binding;
    private ShareViewModel viewModel;
    private int passwordId;
    private List<Friend> friendList = new ArrayList<>();
    private String selectedFriendId = null;
    
    // 传输方式
    private enum TransmissionMethod {
        QR_CODE, BLUETOOTH, NFC, CLOUD
    }
    private TransmissionMethod selectedTransmissionMethod = TransmissionMethod.QR_CODE;

    // 过期时间选项（分钟）
    private final String[] expireTimeOptions = {
        "1小时", "1天", "3天", "7天", "永久"
    };
    private final int[] expireTimeValues = {
        60, 1440, 4320, 10080, 0
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        binding = ActivityShareBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 获取要分享的密码ID
        passwordId = getIntent().getIntExtra("PASSWORD_ID", -1);
        if (passwordId == -1) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupViewModel();
        setupToolbar();
        setupViews();
        observeViewModel();
    }

    private void setupViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
            getApplication(),
            ServiceLocator.getInstance().getBackendService()
        );
        viewModel = new ViewModelProvider(this, factory).get(ShareViewModel.class);
        
        // 加载密码信息和好友列表
        viewModel.loadPasswordItem(passwordId);
        viewModel.loadFriendList();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViews() {
        // 设置过期时间下拉菜单
        ArrayAdapter<String> expireAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            expireTimeOptions
        );
        binding.autoCompleteExpireTime.setAdapter(expireAdapter);
        binding.autoCompleteExpireTime.setText(expireTimeOptions[1], false); // 默认1天

        // 分享方式切换
        binding.radioGroupShareMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioShareToFriend) {
                binding.cardFriendSelection.setVisibility(View.VISIBLE);
            } else {
                binding.cardFriendSelection.setVisibility(View.GONE);
            }
        });

        // 按钮点击事件
        binding.btnCancel.setOnClickListener(v -> finish());
        binding.btnShare.setOnClickListener(v -> createShare());
        
        // 传输方式切换
        binding.chipGroupTransmissionMethod.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                int checkedId = checkedIds.get(0);
                if (checkedId == R.id.chipQrCode) {
                    selectedTransmissionMethod = TransmissionMethod.QR_CODE;
                    binding.textBluetoothHint.setVisibility(View.GONE);
                } else if (checkedId == R.id.chipBluetooth) {
                    selectedTransmissionMethod = TransmissionMethod.BLUETOOTH;
                    binding.textBluetoothHint.setVisibility(View.VISIBLE);
                    binding.textBluetoothHint.setText(R.string.bluetooth_share_hint);
                } else if (checkedId == R.id.chipNfc) {
                    selectedTransmissionMethod = TransmissionMethod.NFC;
                    binding.textBluetoothHint.setVisibility(View.VISIBLE);
                    binding.textBluetoothHint.setText(R.string.nfc_hint);
                } else if (checkedId == R.id.chipCloud) {
                    selectedTransmissionMethod = TransmissionMethod.CLOUD;
                    binding.textBluetoothHint.setVisibility(View.VISIBLE);
                    binding.textBluetoothHint.setText(R.string.cloud_hint);
                }
            }
        });
    }

    private void observeViewModel() {
        // 观察好友列表
        viewModel.friendList.observe(this, friends -> {
            if (friends != null && !friends.isEmpty()) {
                friendList = friends;
                setupFriendSpinner();
            }
        });

        // 观察加载状态
        viewModel.isLoading.observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.btnShare.setEnabled(!isLoading);
        });

        // 观察错误信息
        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // 观察分享成功
        viewModel.shareSuccess.observe(this, success -> {
            if (success) {
                viewModel.shareResult.observe(this, shareToken -> {
                    if (shareToken != null) {
                        // 跳转到分享结果页面
                        Intent intent = new Intent(this, ShareResultActivity.class);
                        intent.putExtra("SHARE_TOKEN", shareToken);
                        intent.putExtra("PASSWORD_ID", passwordId);
                        intent.putExtra("TRANSMISSION_METHOD", selectedTransmissionMethod.name());
                        
                        // 如果是离线分享，传递分享密码
                        Boolean isOffline = viewModel.isOfflineShare.getValue();
                        if (isOffline != null && isOffline) {
                            intent.putExtra("IS_OFFLINE_SHARE", true);
                            String sharePassword = viewModel.sharePassword.getValue();
                            if (sharePassword != null) {
                                intent.putExtra("SHARE_PASSWORD", sharePassword);
                            }
                        }
                        
                        startActivity(intent);
                        finish();
                    }
                });
            }
        });
    }

    private void setupFriendSpinner() {
        List<String> friendNames = new ArrayList<>();
        for (Friend friend : friendList) {
            friendNames.add(friend.getDisplayName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            friendNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerFriends.setAdapter(adapter);
    }

    private void createShare() {
        // 获取选择的过期时间
        String selectedExpireTime = binding.autoCompleteExpireTime.getText().toString();
        int expireInMinutes = getExpireTimeValue(selectedExpireTime);

        // 创建分享权限
        SharePermission permission = new SharePermission();
        permission.setCanView(true);
        permission.setCanSave(binding.switchAllowSave.isChecked());
        permission.setRevocable(binding.switchRevocable.isChecked());

        // 根据分享方式创建分享
        if (binding.radioShareToFriend.isChecked()) {
            // 分享给好友
            int selectedPosition = binding.spinnerFriends.getSelectedItemPosition();
            if (selectedPosition >= 0 && selectedPosition < friendList.size()) {
                Friend selectedFriend = friendList.get(selectedPosition);
                viewModel.createShareToFriend(
                    passwordId,
                    selectedFriend.getFriendId(),
                    expireInMinutes,
                    permission
                );
            } else {
                Toast.makeText(this, "请选择好友", Toast.LENGTH_SHORT).show();
            }
        } else {
            // 直接分享（使用离线分享的实现）
            viewModel.createOfflineShare(passwordId, expireInMinutes, permission);
        }
    }

    private int getExpireTimeValue(String expireTimeText) {
        for (int i = 0; i < expireTimeOptions.length; i++) {
            if (expireTimeOptions[i].equals(expireTimeText)) {
                return expireTimeValues[i];
            }
        }
        return expireTimeValues[1]; // 默认1天
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
