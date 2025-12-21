package com.ttt.safevault.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ttt.safevault.R;
import com.ttt.safevault.databinding.FragmentSettingsBinding;

/**
 * 设置页面 Fragment
 * 显示应用设置选项
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
        // 设置项点击事件
        // TODO: 实现具体设置功能
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}