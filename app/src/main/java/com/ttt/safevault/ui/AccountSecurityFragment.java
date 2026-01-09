package com.ttt.safevault.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import android.text.InputType;
import android.widget.EditText;
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

        // 生物识别开关 - 使用点击监听器而不是状态改变监听器
        // 这样只有用户主动点击时才会触发，避免初始化时触发
        binding.switchBiometric.setOnClickListener(v -> {
            boolean newState = binding.switchBiometric.isChecked();

            if (newState) {
                // 用户想要开启生物识别（点击后状态变为true）
                // 检查设备是否支持生物识别
                if (com.ttt.safevault.security.BiometricAuthHelper.isBiometricSupported(requireContext())) {
                    // 先将开关恢复为关闭状态，等待验证成功后再开启
                    binding.switchBiometric.setChecked(false);

                    // 启用生物识别前要求用户验证身份
                    // 直接触发生物识别验证
                    showBiometricOnlyAuthentication(() -> {
                        // 验证成功，开启生物识别
                        binding.switchBiometric.setChecked(true);
                        securityConfig.setBiometricEnabled(true);
                        Toast.makeText(requireContext(), "生物识别已启用", Toast.LENGTH_SHORT).show();
                    }, () -> {
                        // 验证失败，保持开关关闭状态
                        binding.switchBiometric.setChecked(false);
                    });
                } else {
                    // 设备不支持生物识别，显示提示并恢复开关状态
                    binding.switchBiometric.setChecked(false);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("生物识别不可用")
                            .setMessage("您的设备不支持生物识别认证或未设置生物识别信息")
                            .setPositiveButton("确定", null)
                            .show();
                }
            } else {
                // 用户想要关闭生物识别（点击后状态变为false）- 直接关闭，不需要验证
                binding.switchBiometric.setChecked(false);
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

        new MaterialAlertDialogBuilder(requireContext())
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

    /**
     * 只使用生物识别验证身份（用于启用生物识别功能）
     * @param onSuccess 验证成功回调
     * @param onFailure 验证失败回调
     */
    private void showBiometricOnlyAuthentication(Runnable onSuccess, Runnable onFailure) {
        com.ttt.safevault.security.BiometricAuthHelper biometricHelper =
            new com.ttt.safevault.security.BiometricAuthHelper(
                (androidx.fragment.app.FragmentActivity) requireActivity());
        biometricHelper.authenticate(new com.ttt.safevault.security.BiometricAuthHelper.BiometricAuthCallback() {
            @Override
            public void onSuccess() {
                if (onSuccess != null) {
                    requireActivity().runOnUiThread(onSuccess);
                }
            }

            @Override
            public void onFailure(String error) {
                if (onFailure != null) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "生物识别验证失败: " + error, Toast.LENGTH_SHORT).show();
                        onFailure.run();
                    });
                }
            }

            @Override
            public void onCancel() {
                if (onFailure != null) {
                    requireActivity().runOnUiThread(onFailure);
                }
            }
        });
    }

    /**
     * 提示用户验证身份
     * @param onSuccess 验证成功回调
     * @param onFailure 验证失败回调
     */
    private void promptUserAuthentication(Runnable onSuccess, Runnable onFailure) {
        // 如果已经启用了生物识别，优先使用生物识别验证
        if (securityConfig.isBiometricEnabled()) {
            com.ttt.safevault.security.BiometricAuthHelper biometricHelper =
                new com.ttt.safevault.security.BiometricAuthHelper(
                    (androidx.fragment.app.FragmentActivity) requireActivity());
            biometricHelper.authenticate(new com.ttt.safevault.security.BiometricAuthHelper.BiometricAuthCallback() {
                @Override
                public void onSuccess() {
                    if (onSuccess != null) {
                        requireActivity().runOnUiThread(onSuccess);
                    }
                }

                @Override
                public void onFailure(String error) {
                    // 生物识别失败，回退到主密码验证
                    requireActivity().runOnUiThread(() -> showPasswordAuthenticationDialog(onSuccess, onFailure));
                }

                @Override
                public void onCancel() {
                    if (onFailure != null) {
                        requireActivity().runOnUiThread(onFailure);
                    }
                }
            });
        } else {
            // 未启用生物识别，使用主密码验证
            showPasswordAuthenticationDialog(onSuccess, onFailure);
        }
    }

    /**
     * 显示主密码验证对话框
     * @param onSuccess 验证成功回调
     * @param onFailure 验证失败回调
     */
    private void showPasswordAuthenticationDialog(Runnable onSuccess, Runnable onFailure) {
        EditText passwordInput = new EditText(requireContext());
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordInput.setHint("请输入主密码以验证身份");

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("验证身份")
                .setMessage("为了安全起见，请验证您的身份以启用生物识别")
                .setView(passwordInput)
                .setPositiveButton("验证", (dialog, which) -> {
                    String password = passwordInput.getText().toString();
                    if (password.isEmpty()) {
                        Toast.makeText(requireContext(), "密码不能为空", Toast.LENGTH_SHORT).show();
                        if (onFailure != null) {
                            onFailure.run();
                        }
                        return;
                    }

                    // 调用后端服务验证密码
                    com.ttt.safevault.model.BackendService backendService =
                        com.ttt.safevault.ServiceLocator.getInstance().getBackendService();
                    try {
                        boolean authenticated = backendService.unlock(password);
                        if (authenticated) {
                            if (onSuccess != null) {
                                onSuccess.run();
                            }
                        } else {
                            Toast.makeText(requireContext(), "密码错误，验证失败", Toast.LENGTH_SHORT).show();
                            if (onFailure != null) {
                                onFailure.run();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "验证时发生错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        if (onFailure != null) {
                            onFailure.run();
                        }
                    }
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    if (onFailure != null) {
                        onFailure.run();
                    }
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}