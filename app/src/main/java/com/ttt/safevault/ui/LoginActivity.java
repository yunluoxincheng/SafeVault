package com.ttt.safevault.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.core.splashscreen.SplashScreen;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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
import com.ttt.safevault.viewmodel.AuthViewModel;
import com.ttt.safevault.viewmodel.LoginViewModel;

/**
 * 登录/解锁页面
 * 处理主密码输入、生物识别解锁和云端账号登录
 */
public class LoginActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1001;
    private LoginViewModel viewModel;
    private AuthViewModel authViewModel;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private TextInputEditText usernameInput;
    private TextInputEditText displayNameInput;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputLayout usernameLayout;
    private TextInputLayout displayNameLayout;
    private Button loginButton;
    private Button biometricButton;
    private Button cloudLoginButton;
    private TextView errorText;
    private TextView titleText;
    private TextView subtitleText;
    private TextView switchModeText;
    private TextView cloudLoginHint;
    private ProgressBar progressBar;
    private View confirmPasswordSection;
    private View cloudLoginSection;

    // 状态标志
    private boolean isInitializing = false;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;
    private boolean fromAutofill = false;  // 是否从自动填充跳转过来
    private boolean isCloudLoginMode = false;  // 是否为云端登录模式
    private boolean isRegisterMode = false;  // 是否为注册模式
    
    // 生物识别认证助手
    private BiometricAuthHelper biometricAuthHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 安装启动画面 (兼容API 29+)
        SplashScreen.installSplashScreen(this);

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
        authViewModel = new ViewModelProvider(this, factory).get(AuthViewModel.class);

        initViews();
        setupObservers();
        setupClickListeners();
        setupTextWatchers();
        initBiometricAuth();
    }

    private void initViews() {
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        usernameInput = findViewById(R.id.username_input);
        displayNameInput = findViewById(R.id.display_name_input);
        passwordLayout = findViewById(R.id.password_layout);
        confirmPasswordLayout = findViewById(R.id.confirm_password_layout);
        usernameLayout = findViewById(R.id.username_layout);
        displayNameLayout = findViewById(R.id.display_name_layout);
        loginButton = findViewById(R.id.login_button);
        biometricButton = findViewById(R.id.biometric_button);
        cloudLoginButton = findViewById(R.id.cloud_login_button);
        errorText = findViewById(R.id.error_text);
        titleText = findViewById(R.id.title_text);
        subtitleText = findViewById(R.id.subtitle_text);
        switchModeText = findViewById(R.id.switch_mode_text);
        cloudLoginHint = findViewById(R.id.cloud_login_hint);
        progressBar = findViewById(R.id.progress_bar);
        confirmPasswordSection = findViewById(R.id.confirm_password_section);
        cloudLoginSection = findViewById(R.id.cloud_login_section);
    }

    private void setupObservers() {
        // 观察本地认证状态
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

        // 观察云端认证状态
        authViewModel.getAuthResponse().observe(this, authResponse -> {
            if (authResponse != null) {
                Toast.makeText(this, isRegisterMode ? "注册成功" : "登录成功", Toast.LENGTH_SHORT).show();
                // 云端登录成功后，也需要完成本地解锁
                if (!isInitializing) {
                    navigateToMain();
                }
            }
        });

        // 观察云端认证错误
        authViewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                showError(error);
            }
        });

        // 观察云端加载状态
        authViewModel.getLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                updateLoadingState(isLoading);
            }
        });
    }

    private void setupClickListeners() {
        loginButton.setOnClickListener(v -> {
            if (isCloudLoginMode) {
                handleCloudLogin();
            } else {
                String password = passwordInput.getText().toString().trim();
                if (isInitializing) {
                    String confirmPassword = confirmPasswordInput.getText().toString().trim();
                    viewModel.initializeWithPassword(password, confirmPassword);
                } else {
                    viewModel.loginWithPassword(password);
                }
            }
        });

        biometricButton.setOnClickListener(v -> {
            performBiometricAuthentication();
        });

        // 云端登录按钮
        if (cloudLoginButton != null) {
            cloudLoginButton.setOnClickListener(v -> toggleCloudLoginMode());
        }

        // 切换注册/登录模式
        if (switchModeText != null) {
            switchModeText.setOnClickListener(v -> toggleRegisterMode());
        }

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
        // 添加云端登录相关输入框的监听
        if (usernameInput != null) {
            usernameInput.addTextChangedListener(textWatcher);
        }
        if (displayNameInput != null) {
            displayNameInput.addTextChangedListener(textWatcher);
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

        if (isCloudLoginMode) {
            // 云端模式
            if (isRegisterMode) {
                // 云端注册：需要用户名和显示名称
                String username = usernameInput != null ? usernameInput.getText().toString().trim() : "";
                String displayName = displayNameInput != null ? displayNameInput.getText().toString().trim() : "";
                enabled = !TextUtils.isEmpty(username) && !TextUtils.isEmpty(displayName);
            } else {
                // 云端登录：总是启用（使用已保存的userId）
                enabled = true;
            }
        } else {
            // 本地模式
            if (isInitializing) {
                // 本地初始化：需要密码和确认密码
                String password = passwordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();
                enabled = !TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmPassword);
            } else {
                // 本地解锁：需要主密码
                enabled = !TextUtils.isEmpty(passwordInput.getText().toString().trim());
            }
        }

        if (loginButton != null) {
            Boolean localLoading = viewModel.isLoading.getValue();
            Boolean cloudLoading = authViewModel.getLoading().getValue();
            boolean isLoading = (localLoading != null && localLoading) || (cloudLoading != null && cloudLoading);
            loginButton.setEnabled(enabled && !isLoading);
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

    /**
     * 切换云端登录模式
     */
    private void toggleCloudLoginMode() {
        isCloudLoginMode = !isCloudLoginMode;
        updateUiForCloudLoginMode();
    }

    /**
     * 切换注册/登录模式
     */
    private void toggleRegisterMode() {
        isRegisterMode = !isRegisterMode;
        updateUiForRegisterMode();
    }

    /**
     * 更新UI以显示云端登录模式
     */
    private void updateUiForCloudLoginMode() {
        if (cloudLoginSection == null) return;

        if (isCloudLoginMode) {
            // 显示云端登录界面
            cloudLoginSection.setVisibility(View.VISIBLE);
            confirmPasswordSection.setVisibility(View.GONE);
            biometricButton.setVisibility(View.GONE);

            // 根据注册/登录模式显示/隐藏用户名和密码输入框
            // 在云端登录模式下，passwordLayout 不在 cloudLoginSection 内，需要单独处理
            if (isRegisterMode) {
                // 注册模式：显示用户名、显示名称和密码输入框
                if (usernameLayout != null) {
                    usernameLayout.setVisibility(View.VISIBLE);
                }
                if (displayNameLayout != null) {
                    displayNameLayout.setVisibility(View.VISIBLE);
                }
                if (passwordLayout != null) {
                    passwordLayout.setVisibility(View.VISIBLE);
                }
                // 隐藏提示文本
                if (cloudLoginHint != null) {
                    cloudLoginHint.setVisibility(View.GONE);
                }
            } else {
                // 登录模式：显示用户名输入框（支持多账号切换）
                // 隐藏显示名称和密码输入框
                if (cloudLoginHint != null) {
                    cloudLoginHint.setVisibility(View.GONE);
                }
                if (usernameLayout != null) {
                    usernameLayout.setVisibility(View.VISIBLE);
                }
                if (displayNameLayout != null) {
                    displayNameLayout.setVisibility(View.GONE);
                }
                if (passwordLayout != null) {
                    passwordLayout.setVisibility(View.GONE);
                }
            }

            titleText.setText(isRegisterMode ? R.string.cloud_register : R.string.cloud_login);
            // 更新副标题
            if (subtitleText != null) {
                if (isRegisterMode) {
                    subtitleText.setText("创建云端账号");
                } else {
                    subtitleText.setText("输入用户名登录");
                }
            }
            loginButton.setText(isRegisterMode ? R.string.register : R.string.login);
            cloudLoginButton.setText(R.string.local_unlock);
            if (switchModeText != null) {
                switchModeText.setVisibility(View.VISIBLE);
                switchModeText.setText(isRegisterMode ? R.string.switch_to_login : R.string.switch_to_register);
            }

            // 更新按钮状态
            updateLoginButtonState();
        } else {
            // 显示本地解锁界面
            cloudLoginSection.setVisibility(View.GONE);
            if (usernameLayout != null) usernameLayout.setVisibility(View.GONE);
            if (displayNameLayout != null) displayNameLayout.setVisibility(View.GONE);
            // 确保 passwordLayout 可见（本地模式总是需要密码输入）
            if (passwordLayout != null) {
                passwordLayout.setVisibility(View.VISIBLE);
            }
            updateUiForInitializationState(isInitializing);
            cloudLoginButton.setText(R.string.cloud_login);
            if (switchModeText != null) {
                switchModeText.setVisibility(View.GONE);
            }
            // 恢复副标题
            if (subtitleText != null) {
                subtitleText.setText(R.string.enter_master_password);
            }
            // 注意：不要重置 isRegisterMode，这样当用户再次切换回云端模式时能恢复之前的状态
        }
    }

    /**
     * 更新UI以显示注册/登录模式
     */
    private void updateUiForRegisterMode() {
        if (!isCloudLoginMode) return;

        if (isRegisterMode) {
            // 注册模式 - 需要用户名、显示名称和密码
            titleText.setText(R.string.cloud_register);
            // 更新副标题
            if (subtitleText != null) {
                subtitleText.setText("创建云端账号");
            }
            loginButton.setText(R.string.register);
            if (usernameLayout != null) {
                usernameLayout.setVisibility(View.VISIBLE);
            }
            if (displayNameLayout != null) {
                displayNameLayout.setVisibility(View.VISIBLE);
            }
            if (passwordLayout != null) {
                passwordLayout.setVisibility(View.VISIBLE);
            }
            // 隐藏提示文本
            if (cloudLoginHint != null) {
                cloudLoginHint.setVisibility(View.GONE);
            }
            if (switchModeText != null) {
                switchModeText.setText(R.string.switch_to_login);
            }
        } else {
            // 登录模式 - 需要用户名输入框（支持多账号切换）
            titleText.setText(R.string.cloud_login);
            // 更新副标题
            if (subtitleText != null) {
                subtitleText.setText("输入用户名登录");
            }
            loginButton.setText(R.string.login);
            if (usernameLayout != null) {
                usernameLayout.setVisibility(View.VISIBLE);
            }
            if (passwordLayout != null) {
                passwordLayout.setVisibility(View.GONE);
            }
            if (displayNameLayout != null) {
                displayNameLayout.setVisibility(View.GONE);
            }
            // 隐藏提示文本
            if (cloudLoginHint != null) {
                cloudLoginHint.setVisibility(View.GONE);
            }
            if (switchModeText != null) {
                switchModeText.setText(R.string.switch_to_register);
            }
        }

        // 更新按钮状态
        updateLoginButtonState();
    }

    /**
     * 处理云端登录/注册
     */
    private void handleCloudLogin() {
        // 获取用户名
        String username = usernameInput != null ? usernameInput.getText().toString().trim() : "";

        if (username.isEmpty()) {
            showError("用户名不能为空");
            return;
        }

        if (isRegisterMode) {
            // 注册模式 - 需要显示名称
            String displayName = displayNameInput != null ? displayNameInput.getText().toString().trim() : "";
            if (displayName.isEmpty()) {
                showError(getString(R.string.display_name_required));
                return;
            }
            authViewModel.register(username, displayName);
        } else {
            // 登录模式 - 使用用户名登录
            authViewModel.loginWithUsername(username);
        }
    }
}