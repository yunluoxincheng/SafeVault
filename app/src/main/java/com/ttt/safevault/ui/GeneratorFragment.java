package com.ttt.safevault.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.ttt.safevault.R;
import com.ttt.safevault.adapter.GeneratedPasswordsAdapter;
import com.ttt.safevault.databinding.FragmentGeneratorBinding;
import com.ttt.safevault.viewmodel.GeneratorViewModel;
import com.ttt.safevault.utils.ClipboardManager;

/**
 * 密码生成器页面 Fragment
 * 提供独立的密码生成功能，包括实时预览、历史记录等
 */
public class GeneratorFragment extends BaseFragment {

    private FragmentGeneratorBinding binding;
    private GeneratorViewModel viewModel;
    private ClipboardManager clipboardManager;

    // UI 组件
    private TextInputEditText generatedPasswordText;
    private Slider lengthSlider;
    private MaterialSwitch uppercaseSwitch;
    private MaterialSwitch lowercaseSwitch;
    private MaterialSwitch numbersSwitch;
    private MaterialSwitch symbolsSwitch;
    private Button regenerateButton;
    private Button copyButton;
    private Button saveButton;
    private MaterialCardView pinPresetCard;
    private MaterialCardView strongPresetCard;
    private MaterialCardView memorablePresetCard;
    private RecyclerView historyRecyclerView;
    private GeneratedPasswordsAdapter historyAdapter;
    private View strengthBar1;
    private View strengthBar2;
    private View strengthBar3;
    private View strengthBar4;
    private TextView strengthText;
    private Button clearHistoryButton;
    private View historyEmptyLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGeneratorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews();
        initViewModel();
        setupClickListeners();
        initClipboardManager();
        generateInitialPassword();
    }

    private void initViews() {
        generatedPasswordText = binding.generatedPasswordText;
        lengthSlider = binding.lengthSlider;
        uppercaseSwitch = binding.uppercaseSwitch;
        lowercaseSwitch = binding.lowercaseSwitch;
        numbersSwitch = binding.numbersSwitch;
        symbolsSwitch = binding.symbolsSwitch;
        regenerateButton = binding.regenerateButton;
        copyButton = binding.copyButton;
        saveButton = binding.saveButton;
        pinPresetCard = binding.pinPresetCard;
        strongPresetCard = binding.strongPresetCard;
        memorablePresetCard = binding.memorablePresetCard;
        historyRecyclerView = binding.historyRecyclerView;
        strengthBar1 = binding.strengthBar1;
        strengthBar2 = binding.strengthBar2;
        strengthBar3 = binding.strengthBar3;
        strengthBar4 = binding.strengthBar4;
        strengthText = binding.strengthText;
        clearHistoryButton = binding.clearHistoryButton;
        historyEmptyLayout = binding.historyEmptyLayout;

        // 设置默认值
        lengthSlider.setValue(16);
        uppercaseSwitch.setChecked(true);
        lowercaseSwitch.setChecked(true);
        numbersSwitch.setChecked(true);
        symbolsSwitch.setChecked(false);

        // 设置历史记录 RecyclerView
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        historyAdapter = new GeneratedPasswordsAdapter(
            password -> {
                // 点击历史记录复制密码
                copyToClipboard(password);
            },
            position -> {
                // 删除历史记录项（可选功能，暂时不实现）
                // viewModel.deleteFromHistory(position);
            }
        );
        historyRecyclerView.setAdapter(historyAdapter);
    }

    private void initClipboardManager() {
        clipboardManager = new ClipboardManager(requireContext());
    }

    private void initViewModel() {
        ViewModelProvider.Factory factory = new com.ttt.safevault.viewmodel.ViewModelFactory(
                requireActivity().getApplication());
        viewModel = new ViewModelProvider(this, factory).get(GeneratorViewModel.class);

        // 观察生成的密码
        viewModel.getGeneratedPassword().observe(getViewLifecycleOwner(), password -> {
            generatedPasswordText.setText(password);
        });

        // 观察密码强度
        viewModel.getPasswordStrength().observe(getViewLifecycleOwner(), strength -> {
            updateStrengthIndicator(strength);
        });

        // 观察历史记录
        viewModel.getGeneratedHistory().observe(getViewLifecycleOwner(), history -> {
            historyAdapter.submitList(history);

            // 更新空状态
            if (historyEmptyLayout != null) {
                boolean isEmpty = history == null || history.isEmpty();
                historyEmptyLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                historyRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            }
        });
    }

    private void setupClickListeners() {
        // 滑块变化时重新生成
        lengthSlider.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                generatePassword();
            }
        });

        // 开关变化时重新生成
        uppercaseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());
        lowercaseSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());
        numbersSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());
        symbolsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> generatePassword());

        // 重新生成按钮
        regenerateButton.setOnClickListener(v -> generatePassword());

        // 复制按钮
        copyButton.setOnClickListener(v -> copyPasswordToClipboard());

        // 保存按钮
        saveButton.setOnClickListener(v -> savePassword());

        // 预设配置
        pinPresetCard.setOnClickListener(v -> applyPreset(GeneratorViewModel.Preset.PIN));
        strongPresetCard.setOnClickListener(v -> applyPreset(GeneratorViewModel.Preset.STRONG));
        memorablePresetCard.setOnClickListener(v -> applyPreset(GeneratorViewModel.Preset.MEMORABLE));

        // 清除历史
        clearHistoryButton.setOnClickListener(v -> clearHistory());
    }

    private void generateInitialPassword() {
        generatePassword();
    }

    private void generatePassword() {
        int length = (int) lengthSlider.getValue();
        boolean uppercase = uppercaseSwitch.isChecked();
        boolean lowercase = lowercaseSwitch.isChecked();
        boolean numbers = numbersSwitch.isChecked();
        boolean symbols = symbolsSwitch.isChecked();

        viewModel.generatePassword(length, uppercase, lowercase, numbers, symbols);
    }

    private void copyPasswordToClipboard() {
        String password = generatedPasswordText.getText() != null
                ? generatedPasswordText.getText().toString()
                : "";
        if (!password.isEmpty()) {
            copyToClipboard(password);
            viewModel.addToHistory(password);
        }
    }

    private void copyToClipboard(String password) {
        // 使用自定义 ClipboardManager，支持 30 秒自动清除
        clipboardManager.copySensitiveText(password, "password");

        // 使用 Snackbar 显示复制成功提示
        Snackbar.make(binding.getRoot(), R.string.copied, Snackbar.LENGTH_SHORT).show();
    }

    private void savePassword() {
        String password = generatedPasswordText.getText() != null
                ? generatedPasswordText.getText().toString()
                : "";
        if (!password.isEmpty()) {
            // TODO: 实现导航到编辑页面并传递生成的密码
            // 暂时只复制到剪贴板
            copyPasswordToClipboard();
            Toast.makeText(requireContext(), "请手动粘贴到新密码项", Toast.LENGTH_LONG).show();
        }
    }

    private void applyPreset(GeneratorViewModel.Preset preset) {
        viewModel.applyPreset(preset);

        // 更新 UI
        switch (preset) {
            case PIN:
                lengthSlider.setValue(4);
                uppercaseSwitch.setChecked(false);
                lowercaseSwitch.setChecked(false);
                numbersSwitch.setChecked(true);
                symbolsSwitch.setChecked(false);
                break;
            case STRONG:
                lengthSlider.setValue(16);
                uppercaseSwitch.setChecked(true);
                lowercaseSwitch.setChecked(true);
                numbersSwitch.setChecked(true);
                symbolsSwitch.setChecked(true);
                break;
            case MEMORABLE:
                lengthSlider.setValue(12);
                uppercaseSwitch.setChecked(true);
                lowercaseSwitch.setChecked(true);
                numbersSwitch.setChecked(true);
                symbolsSwitch.setChecked(false);
                break;
        }
    }

    private void updateStrengthIndicator(int strength) {
        if (strengthBar1 == null || strengthBar2 == null ||
            strengthBar3 == null || strengthBar4 == null || strengthText == null) return;

        // strength 范围 0-100
        // 重置所有条
        strengthBar1.setAlpha(0.3f);
        strengthBar2.setAlpha(0.3f);
        strengthBar3.setAlpha(0.3f);
        strengthBar4.setAlpha(0.3f);

        // 根据强度点亮相应的段
        String strengthLabel;
        int strengthColor;

        if (strength < 25) {
            // 弱 - 只点亮第1段
            strengthBar1.setAlpha(1.0f);
            strengthLabel = "弱";
            strengthColor = getResources().getColor(R.color.strength_weak, null);
        } else if (strength < 50) {
            // 中等 - 点亮第1-2段
            strengthBar1.setAlpha(1.0f);
            strengthBar2.setAlpha(1.0f);
            strengthLabel = "中等";
            strengthColor = getResources().getColor(R.color.strength_medium, null);
        } else if (strength < 75) {
            // 强 - 点亮第1-3段
            strengthBar1.setAlpha(1.0f);
            strengthBar2.setAlpha(1.0f);
            strengthBar3.setAlpha(1.0f);
            strengthLabel = "强";
            strengthColor = getResources().getColor(R.color.strength_strong, null);
        } else {
            // 很强 - 点亮所有段
            strengthBar1.setAlpha(1.0f);
            strengthBar2.setAlpha(1.0f);
            strengthBar3.setAlpha(1.0f);
            strengthBar4.setAlpha(1.0f);
            strengthLabel = "很强";
            strengthColor = getResources().getColor(R.color.strength_very_strong, null);
        }

        strengthText.setText(strengthLabel);
        strengthText.setTextColor(strengthColor);
    }

    private void clearHistory() {
        viewModel.clearHistory();
        Toast.makeText(requireContext(), "历史记录已清除", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
