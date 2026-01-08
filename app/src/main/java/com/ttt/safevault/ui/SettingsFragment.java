package com.ttt.safevault.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.Navigation;

import com.ttt.safevault.R;
import com.ttt.safevault.databinding.FragmentSettingsBinding;

/**
 * 设置主页面 Fragment
 * 显示四个主要设置分类的入口
 */
public class SettingsFragment extends BaseFragment {

    private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
    }

    private void setupClickListeners() {
        // 账户安全
        binding.cardAccountSecurity.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_settings_to_accountSecurity));

        // 自动填充
        binding.cardAutofill.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_settings_to_autofillSettings));

        // 外观设置
        binding.cardAppearance.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_settings_to_appearanceSettings));

        // 关于
        binding.cardAbout.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_settings_to_about));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
