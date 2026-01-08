package com.ttt.safevault.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ttt.safevault.R;
import com.ttt.safevault.ServiceLocator;
import com.ttt.safevault.databinding.FragmentShareSettingsBinding;
import com.ttt.safevault.security.SecurityConfig;

/**
 * 分享设置 Fragment
 */
public class ShareSettingsFragment extends BaseFragment {

    private FragmentShareSettingsBinding binding;
    private SecurityConfig securityConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShareSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        securityConfig = ServiceLocator.getInstance().getSecurityConfig();
        
        setupViews();
        setupClickListeners();
    }

    private void setupViews() {
        // 设置当前值
        updateTransmissionMethodText();
        updateExpireTimeText();
        binding.switchDefaultSaveable.setChecked(securityConfig.isDefaultShareSaveable());
        updatePasswordLengthText();
        binding.switchAutoRevoke.setChecked(securityConfig.isAutoRevokeAfterView());
    }

    private void setupClickListeners() {
        // 默认传输方式
        binding.layoutTransmissionMethod.setOnClickListener(v -> showTransmissionMethodDialog());
        
        // 默认过期时间
        binding.layoutExpireTime.setOnClickListener(v -> showExpireTimeDialog());
        
        // 默认允许保存
        binding.switchDefaultSaveable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityConfig.setDefaultShareSaveable(isChecked);
        });
        
        // 分享密码长度
        binding.layoutPasswordLength.setOnClickListener(v -> showPasswordLengthDialog());
        
        // 查看后自动撤销
        binding.switchAutoRevoke.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityConfig.setAutoRevokeAfterView(isChecked);
        });
    }

    private void showTransmissionMethodDialog() {
        String[] methods = {
            getString(R.string.qr_code_transmission),
            getString(R.string.bluetooth_transmission),
            getString(R.string.nfc_transmission),
            getString(R.string.cloud_transmission)
        };
        String[] methodValues = {"QR_CODE", "BLUETOOTH", "NFC", "CLOUD"};
        
        String currentMethod = securityConfig.getDefaultTransmissionMethod();
        int selectedIndex = 0;
        for (int i = 0; i < methodValues.length; i++) {
            if (methodValues[i].equals(currentMethod)) {
                selectedIndex = i;
                break;
            }
        }
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.default_transmission_method)
            .setSingleChoiceItems(methods, selectedIndex, (dialog, which) -> {
                securityConfig.setDefaultTransmissionMethod(methodValues[which]);
                updateTransmissionMethodText();
                dialog.dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showExpireTimeDialog() {
        String[] times = {
            getString(R.string.expire_time_minutes, 30),
            getString(R.string.expire_time_minutes, 60),
            getString(R.string.expire_time_minutes, 120),
            getString(R.string.expire_time_minutes, 1440) // 24小时
        };
        int[] timeValues = {30, 60, 120, 1440};
        
        int currentTime = securityConfig.getDefaultShareExpireTime();
        int selectedIndex = 1; // 默认60分钟
        for (int i = 0; i < timeValues.length; i++) {
            if (timeValues[i] == currentTime) {
                selectedIndex = i;
                break;
            }
        }
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.default_expire_time)
            .setSingleChoiceItems(times, selectedIndex, (dialog, which) -> {
                securityConfig.setDefaultShareExpireTime(timeValues[which]);
                updateExpireTimeText();
                dialog.dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void showPasswordLengthDialog() {
        String[] lengths = {"6", "8", "10", "12", "16"};
        int[] lengthValues = {6, 8, 10, 12, 16};
        
        int currentLength = securityConfig.getSharePasswordLength();
        int selectedIndex = 1; // 默认8位
        for (int i = 0; i < lengthValues.length; i++) {
            if (lengthValues[i] == currentLength) {
                selectedIndex = i;
                break;
            }
        }
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.share_password_length)
            .setSingleChoiceItems(lengths, selectedIndex, (dialog, which) -> {
                securityConfig.setSharePasswordLength(lengthValues[which]);
                updatePasswordLengthText();
                dialog.dismiss();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void updateTransmissionMethodText() {
        String method = securityConfig.getDefaultTransmissionMethod();
        String text;
        switch (method) {
            case "BLUETOOTH":
                text = getString(R.string.bluetooth_transmission);
                break;
            case "NFC":
                text = getString(R.string.nfc_transmission);
                break;
            case "CLOUD":
                text = getString(R.string.cloud_transmission);
                break;
            case "QR_CODE":
            default:
                text = getString(R.string.qr_code_transmission);
                break;
        }
        binding.textTransmissionMethod.setText(text);
    }

    private void updateExpireTimeText() {
        int minutes = securityConfig.getDefaultShareExpireTime();
        binding.textExpireTime.setText(getString(R.string.expire_time_minutes, minutes));
    }

    private void updatePasswordLengthText() {
        int length = securityConfig.getSharePasswordLength();
        binding.textPasswordLength.setText(getString(R.string.password_length_chars, length));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
