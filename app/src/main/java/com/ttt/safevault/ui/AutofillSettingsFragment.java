package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.ttt.safevault.R;
import com.ttt.safevault.databinding.FragmentAutofillSettingsBinding;
import com.ttt.safevault.security.SecurityConfig;

/**
 * 自动填充设置 Fragment
 * 管理自动填充服务配置
 */
public class AutofillSettingsFragment extends BaseFragment {

    private FragmentAutofillSettingsBinding binding;
    private SecurityConfig securityConfig;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAutofillSettingsBinding.inflate(inflater, container, false);
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
        // 检查自动填充服务状态
        updateAutofillStatus();

        // 自动填充建议
        binding.switchAutofillSuggestions.setChecked(securityConfig.isAutofillSuggestionsEnabled());

        // 复制到剪贴板
        binding.switchAutofillCopy.setChecked(securityConfig.isAutofillCopyToClipboard());
    }

    private void setupClickListeners() {
        // 打开系统自动填充设置
        binding.btnOpenAutofillSettings.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Intent intent = new Intent("android.settings.AUTOFILL_SETTINGS");
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "无法打开自动填充设置", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(requireContext(), "自动填充仅支持 Android 8.0 及以上版本", Toast.LENGTH_SHORT).show();
            }
        });

        // 自动填充建议开关
        binding.switchAutofillSuggestions.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityConfig.setAutofillSuggestionsEnabled(isChecked);
        });

        // 复制到剪贴板开关
        binding.switchAutofillCopy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            securityConfig.setAutofillCopyToClipboard(isChecked);
        });
    }

    private void updateAutofillStatus() {
        // TODO: 实际检查自动填充服务状态
        // 这里简化为固定显示"未开启"
        binding.tvAutofillStatus.setText(R.string.autofill_status_disabled);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 从系统设置返回时更新状态
        updateAutofillStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
