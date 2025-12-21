package com.ttt.safevault.ui;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.ttt.safevault.security.SecurityManager;

/**
 * 基础Fragment
 * 所有Fragment都应该继承此类以自动应用安全措施
 */
public abstract class BaseFragment extends Fragment {

    protected SecurityManager securityManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取Activity的SecurityManager
        if (getActivity() instanceof BaseActivity) {
            securityManager = ((BaseActivity) getActivity()).getSecurityManager();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 应用安全措施
        applySecurityMeasures(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 更新最后交互时间
        if (securityManager != null) {
            securityManager.updateLastInteraction();
        }
    }

    /**
     * 应用安全措施
     */
    protected void applySecurityMeasures(@NonNull View view) {
        if (securityManager != null) {
            securityManager.applySecurityMeasures(view);
        }
    }

    /**
     * 获取安全管理器
     */
    protected SecurityManager getSecurityManager() {
        return securityManager;
    }
}