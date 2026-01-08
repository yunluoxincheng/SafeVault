package com.ttt.safevault.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.ttt.safevault.R;
import com.ttt.safevault.databinding.FragmentAccountSecurityBinding;
import com.ttt.safevault.security.SecurityConfig;

/**
 * 账户安全设置 Fragment
 * 管理解锁选项、生物识别、PIN码、主密码等安全设置
 */
public class AccountSecurityFragment extends BaseFragment {

    private FragmentAccountSecurityBinding binding;
    private SecurityConfig securityConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountSecurityBinding.inflate(inflater, container, false);
        securityConfig = new SecurityConfig(requireContext());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
        loadSettings();
    }

    private void loadSettings() {
        // 显示当前自动锁定选项
        SecurityConfig.AutoLockMode autoLockMode = securityConfig.getAutoLockMode();
        binding.tvAutoLockValue.setText(autoLockMode.getDisplayName());

        // 生物识别开关状态
        binding.switchBiometric.setChecked(securityConfig.isBiometricEnabled());

        // PIN码状态
        if (securityConfig.isPinCodeEnabled()) {
            binding.tvPinStatus.setText("已启用");
        } else {
            binding.tvPinStatus.setText("未启用");
        }
    }

    private void setupClickListeners() {
        // 自动锁定选项
        binding.cardAutoLock.setOnClickListener(v -> showAutoLockDialog());

        // 生物识别开关
        binding.switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 检查设备是否支持生物识别
                if (com.ttt.safevault.security.BiometricAuthHelper.isBiometricSupported(requireContext())) {
                    // 启用生物识别前，确保用户已经设置了主密码或其他认证方式
                    securityConfig.setBiometricEnabled(true);
                    Toast.makeText(requireContext(), "生物识别已启用", Toast.LENGTH_SHORT).show();
                } else {
                    // 设备不支持生物识别，显示提示并恢复开关状态
                    ((MaterialSwitch) buttonView).setChecked(false);
                    new AlertDialog.Builder(requireContext())
                            .setTitle("生物识别不可用")
                            .setMessage("您的设备不支持生物识别认证或未设置生物识别信息")
                            .setPositiveButton("确定", null)
                            .show();
                }
            } else {
                securityConfig.setBiometricEnabled(false);
                Toast.makeText(requireContext(), "生物识别已禁用", Toast.LENGTH_SHORT).show();
            }
        });

        // PIN码设置
        binding.cardPinCode.setOnClickListener(v -> {
            if (securityConfig.isPinCodeEnabled()) {
                showPinCodeOptionsDialog();
            } else {
                // TODO: 显示设置PIN码对话框
                Toast.makeText(requireContext(), "设置PIN码功能待实现", Toast.LENGTH_SHORT).show();
            }
        });

        // 更改主密码
        binding.cardChangePassword.setOnClickListener(v -> {
            // TODO: 显示更改主密码对话框
            Toast.makeText(requireContext(), "更改主密码功能待实现", Toast.LENGTH_SHORT).show();
        });

        // 注销登录
        binding.cardLogout.setOnClickListener(v -> showLogoutDialog());

        // 删除账户
        binding.cardDeleteAccount.setOnClickListener(v -> {
            // TODO: 显示多重确认对话框
            Toast.makeText(requireContext(), "删除账户功能待实现", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAutoLockDialog() {
        SecurityConfig.AutoLockMode[] modes = SecurityConfig.AutoLockMode.values();
        String[] options = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            options[i] = modes[i].getDisplayName();
        }

        int currentSelection = securityConfig.getAutoLockMode().ordinal();

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.auto_lock)
                .setSingleChoiceItems(options, currentSelection, (dialog, which) -> {
                    SecurityConfig.AutoLockMode selectedMode = modes[which];
                    securityConfig.setAutoLockMode(selectedMode);
                    binding.tvAutoLockValue.setText(selectedMode.getDisplayName());
                    dialog.dismiss();
                    Toast.makeText(requireContext(), "自动锁定已设置为: " + selectedMode.getDisplayName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showPinCodeOptionsDialog() {
        String[] options = {"更改PIN码", "移除PIN码"};

        new AlertDialog.Builder(requireContext())
                .setTitle("PIN码选项")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 更改PIN码
                        Toast.makeText(requireContext(), "更改PIN码功能待实现", Toast.LENGTH_SHORT).show();
                    } else {
                        // 移除PIN码
                        showRemovePinDialog();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showRemovePinDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("移除PIN码")
                .setMessage("确定要移除PIN码吗？")
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    securityConfig.setPinCodeEnabled(false);
                    binding.tvPinStatus.setText("未启用");
                    Toast.makeText(requireContext(), R.string.pin_removed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    // TODO: 调用后端服务注销登录
                    Toast.makeText(requireContext(), R.string.logged_out, Toast.LENGTH_SHORT).show();
                    // 返回登录页面
                    requireActivity().finish();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}