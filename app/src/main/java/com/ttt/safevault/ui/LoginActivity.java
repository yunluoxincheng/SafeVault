package com.ttt.safevault.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ttt.safevault.R;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.viewmodel.LoginViewModel;

/**
 * 登录/解锁页面
 * 处理主密码输入和生物识别解锁
 */
public class LoginActivity extends AppCompatActivity {

    private LoginViewModel viewModel;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private Button loginButton;
    private Button biometricButton;
    private TextView errorText;
    private TextView titleText;
    private ProgressBar progressBar;
    private View confirmPasswordSection;

    // 状态标志
    private boolean isInitializing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 防止截图
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);

        // TODO: 获取BackendService实例 - 需要实现BackendService的具体实现
        BackendService backendService = null; // 这里需要依赖注入或通过其他方式获取

        // 初始化ViewModel - 待BackendService实现后取消注释
        // ViewModelProvider.Factory factory = new LoginViewModelFactory(backendService);
        // viewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        // 临时设置viewModel为null以避免编译错误，实际使用时需要实现BackendService
        viewModel = null;

        initViews();
        setupObservers();
        setupClickListeners();
        setupTextWatchers();
    }

    private void initViews() {
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        loginButton = findViewById(R.id.login_button);
        biometricButton = findViewById(R.id.biometric_button);
        errorText = findViewById(R.id.error_text);
        titleText = findViewById(R.id.title_text);
        progressBar = findViewById(R.id.progress_bar);
        confirmPasswordSection = findViewById(R.id.confirm_password_section);
    }

    private void setupObservers() {
        // 待BackendService和ViewModel实现后，取消注释以下代码
        /*
        // 观察认证状态
        viewModel.isAuthenticated.observe(this, isAuthenticated -> {
            if (isAuthenticated) {
                navigateToMain();
            }
        });

        // 观察错误信息
        viewModel.errorMessage.observe(this, error -> {
            if (error != null) {
                showError(error);
            } else {
                hideError();
            }
        });

        // 观察加载状态
        viewModel.isLoading.observe(this, isLoading -> {
            updateLoadingState(isLoading);
        });

        // 观察初始化状态
        viewModel.isInitialized.observe(this, isInitialized -> {
            updateUiForInitializationState(!isInitialized);
        });

        // 观察生物识别支持
        viewModel.canUseBiometric.observe(this, canUse -> {
            biometricButton.setVisibility(canUse ? View.VISIBLE : View.GONE);
        });
        */
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();

            // TODO: 待ViewModel实现后取消注释
            /*
            if (isInitializing) {
                String confirmPassword = confirmPasswordInput.getText().toString().trim();
                viewModel.initializeWithPassword(password, confirmPassword);
            } else {
                viewModel.loginWithPassword(password);
            }
            */

            // 临时提示功能未实现
            showError("功能待BackendService和ViewModel实现后可用");
        });

        biometricButton.setOnClickListener(v -> {
            // TODO: 待ViewModel实现后取消注释
            // viewModel.loginWithBiometric();

            // 临时提示功能未实现
            showError("生物识别功能待实现");
        });

        // 清除错误
        passwordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // TODO: 待ViewModel实现后取消注释
                // viewModel.clearError();
                hideError();
            }
        });
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLoginButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        passwordInput.addTextChangedListener(textWatcher);
        if (confirmPasswordInput != null) {
            confirmPasswordInput.addTextChangedListener(textWatcher);
        }
    }

    private void updateUiForInitializationState(boolean initializing) {
        isInitializing = initializing;

        if (titleText != null) {
            titleText.setText(initializing ? R.string.set_master_password : R.string.unlock_safevault);
        }

        if (loginButton != null) {
            loginButton.setText(initializing ? R.string.initialize : R.string.unlock);
        }

        // 显示/隐藏确认密码输入框
        if (confirmPasswordSection != null && confirmPasswordLayout != null) {
            confirmPasswordSection.setVisibility(initializing ? View.VISIBLE : View.GONE);
            confirmPasswordLayout.setVisibility(initializing ? View.VISIBLE : View.GONE);
        }

        // 初始化时不显示生物识别按钮
        if (biometricButton != null) {
            // TODO: 待ViewModel实现后恢复完整逻辑
            biometricButton.setVisibility(initializing ? View.GONE : View.GONE);
            /*
            biometricButton.setVisibility(initializing ? View.GONE :
                (viewModel.canUseBiometric.getValue() != null && viewModel.canUseBiometric.getValue() ?
                View.VISIBLE : View.GONE));
            */
        }

        updateLoginButtonState();
    }

    private void updateLoginButtonState() {
        boolean enabled = false;

        if (isInitializing) {
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();
            enabled = !TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmPassword);
        } else {
            enabled = !TextUtils.isEmpty(passwordInput.getText().toString().trim());
        }

        if (loginButton != null) {
            // TODO: 待ViewModel实现后恢复完整逻辑
            // loginButton.setEnabled(enabled && !(viewModel.isLoading.getValue() != null && viewModel.isLoading.getValue()));
            loginButton.setEnabled(enabled);
        }
    }

    private void updateLoadingState(boolean isLoading) {
        if (progressBar != null) {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (passwordInput != null) {
            passwordInput.setEnabled(!isLoading);
        }

        if (confirmPasswordInput != null) {
            confirmPasswordInput.setEnabled(!isLoading);
        }

        if (loginButton != null) {
            loginButton.setEnabled(!isLoading);
        }

        if (biometricButton != null) {
            biometricButton.setEnabled(!isLoading);
        }

        updateLoginButtonState();
    }

    private void showError(String error) {
        if (errorText != null) {
            errorText.setText(error);
            errorText.setVisibility(View.VISIBLE);
        }

        // 震动反馈
        if (passwordLayout != null) {
            passwordLayout.setError(error);
        }
    }

    private void hideError() {
        if (errorText != null) {
            errorText.setVisibility(View.GONE);
        }

        if (passwordLayout != null) {
            passwordLayout.setError(null);
        }

        if (confirmPasswordLayout != null) {
            confirmPasswordLayout.setError(null);
        }
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        // TODO: 待ViewModel实现后恢复完整逻辑

        // 如果正在加载，不允许返回
        if (viewModel.isLoading.getValue() != null && viewModel.isLoading.getValue()) {
            return;
        }


        // 如果是初始化界面，允许退出应用
        if (isInitializing) {
            finish();
        } else {
            // 否则最小化应用
            moveTaskToBack(true);
        }
    }
}