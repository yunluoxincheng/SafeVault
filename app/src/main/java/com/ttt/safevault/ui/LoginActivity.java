package com.ttt.safevault.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ttt.safevault.R;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.security.BiometricAuthHelper;
import com.ttt.safevault.utils.AnimationUtils;
import com.ttt.safevault.viewmodel.LoginViewModel;

/**
 * 登录/解锁页面
 * 处理主密码输入和生物识别解锁
 */
public class LoginActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
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
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean fromAutofill = false;  // 是否从自动填充跳转过来
    
    // 生物识别认证助手
    private BiometricAuthHelper biometricAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 防止截图
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        
        // 检查是否从自动填充跳转过来
        if (getIntent() != null) {
            fromAutofill = getIntent().getBooleanExtra("from_autofill", false);
        }

        // 获取BackendService实例
        BackendService backendService = com.ttt.safevault.ServiceLocator.getInstance().getBackendService();

        // 初始化ViewModel
        ViewModelProvider.Factory factory = new com.ttt.safevault.viewmodel.ViewModelFactory(getApplication());
        viewModel = new ViewModelProvider(this, factory).get(LoginViewModel.class);

        initViews();
        setupObservers();
        setupClickListeners();
        setupTextWatchers();
        initBiometricAuth();
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
            if (isInitialized != null) {
                updateUiForInitializationState(!isInitialized);
            }
        });

        // 观察生物识别支持
        viewModel.canUseBiometric.observe(this, canUse -> {
            if (canUse != null && biometricButton != null) {
                biometricButton.setVisibility(canUse ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            String password = passwordInput.getText().toString().trim();

            if (isInitializing) {
                String confirmPassword = confirmPasswordInput.getText().toString().trim();
                viewModel.initializeWithPassword(password, confirmPassword);
            } else {
                viewModel.loginWithPassword(password);
            }
        });

        biometricButton.setOnClickListener(v -> {
            performBiometricAuthentication();
        });

        // 清除错误
        passwordInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                viewModel.clearError();
                hideError();
            }
        });

        // 密码显示/隐藏按钮
        if (passwordLayout != null) {
            passwordLayout.setEndIconOnClickListener(v -> {
                togglePasswordVisibility();
            });
        }

        // 确认密码显示/隐藏按钮
        if (confirmPasswordLayout != null) {
            confirmPasswordLayout.setEndIconOnClickListener(v -> {
                toggleConfirmPasswordVisibility();
            });
        }
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
            Boolean canUseBiometric = viewModel.canUseBiometric.getValue();
            biometricButton.setVisibility(initializing ? View.GONE :
                (canUseBiometric != null && canUseBiometric ? View.VISIBLE : View.GONE));
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
            Boolean isLoading = viewModel.isLoading.getValue();
            loginButton.setEnabled(enabled && (isLoading == null || !isLoading));
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
        if (fromAutofill) {
            // 从自动填充跳转过来，返回结果
            setResult(RESULT_OK);
            finish();
        } else {
            // 正常登录，跳转到主界面
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }

    private void togglePasswordVisibility() {
        if (passwordInput == null || passwordLayout == null) return;

        isPasswordVisible = !isPasswordVisible;

        // 切换密码输入类型
        if (isPasswordVisible) {
            // 显示密码
            passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordLayout.setEndIconDrawable(R.drawable.ic_visibility);
            passwordLayout.setEndIconContentDescription(getString(R.string.hide_password));
        } else {
            // 隐藏密码
            passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordLayout.setEndIconDrawable(R.drawable.ic_visibility_off);
            passwordLayout.setEndIconContentDescription(getString(R.string.show_password));
        }

        // 将光标移到末尾
        passwordInput.setSelection(passwordInput.getText().length());

        // 添加动画反馈（getEndIconView 不是公开 API，这里简化处理）
    }

    private void toggleConfirmPasswordVisibility() {
        if (confirmPasswordInput == null || confirmPasswordLayout == null) return;

        isConfirmPasswordVisible = !isConfirmPasswordVisible;

        // 切换密码输入类型
        if (isConfirmPasswordVisible) {
            // 显示密码
            confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            confirmPasswordLayout.setEndIconDrawable(R.drawable.ic_visibility);
            confirmPasswordLayout.setEndIconContentDescription(getString(R.string.hide_password));
        } else {
            // 隐藏密码
            confirmPasswordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            confirmPasswordLayout.setEndIconDrawable(R.drawable.ic_visibility_off);
            confirmPasswordLayout.setEndIconContentDescription(getString(R.string.show_password));
        }

        // 将光标移到末尾
        confirmPasswordInput.setSelection(confirmPasswordInput.getText().length());

        // 添加动画反馈（getEndIconView 不是公开 API，这里简化处理）
    }

    private void performBiometricAuthentication() {
        if (biometricAuthHelper == null) {
            showError("生物识别认证未初始化");
            return;
        }

        biometricAuthHelper.authenticate(new BiometricAuthHelper.BiometricAuthCallback() {
            @Override
            public void onSuccess() {
                // 生物识别认证成功，现在调用后端服务解锁
                runOnUiThread(() -> {
                    // 直接调用后端服务进行解锁
                    BackendService backendService = com.ttt.safevault.ServiceLocator.getInstance().getBackendService();
                    try {
                        boolean unlocked = backendService.unlockWithBiometric();
                        if (unlocked) {
                            // 生物识别解锁成功，导航到主界面
                            navigateToMain();
                        } else {
                            showError("生物识别解锁失败，请使用主密码解锁");
                        }
                    } catch (Exception e) {
                        showError("解锁时发生错误: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                showError(error);
            }

            @Override
            public void onCancel() {
                // 用户取消认证，不做任何操作或显示提示
                // 可选：显示一条消息告知用户认证已取消
            }
        });
    }

    private void initBiometricAuth() {
        biometricAuthHelper = new BiometricAuthHelper(this);
        
        // 检查并请求生物识别权限
        checkAndRequestBiometricPermission();
    }
    
    private void checkAndRequestBiometricPermission() {
        // 检查设备是否支持生物识别，并更新按钮可见性
        boolean biometricSupported = BiometricAuthHelper.isBiometricSupported(this);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ 需要 BIOMETRIC 权限
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.USE_BIOMETRIC)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // 权限未授予，请求权限
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.USE_BIOMETRIC},
                        PERMISSION_REQUEST_CODE);
            }
        }
        
        if (biometricButton != null) {
            biometricButton.setVisibility(biometricSupported ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 权限已处理，生物识别权限检查完成
            if (biometricButton != null) {
                boolean biometricSupported = BiometricAuthHelper.isBiometricSupported(this);
                biometricButton.setVisibility(biometricSupported ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    @SuppressLint("MissingSuperCall")
    public void onBackPressed() {
        // 如果正在加载，不允许返回
        Boolean isLoading = viewModel.isLoading.getValue();
        if (isLoading != null && isLoading) {
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