package com.ttt.safevault.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ttt.safevault.R;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.model.PasswordStrength;
import com.ttt.safevault.viewmodel.EditPasswordViewModel;

/**
 * 编辑密码Fragment
 * 用于创建和编辑密码条目
 */
public class EditPasswordFragment extends Fragment {

    private EditPasswordViewModel viewModel;
    private MaterialToolbar toolbar;
    private TextInputLayout titleLayout;
    private TextInputEditText titleText;
    private TextInputLayout usernameLayout;
    private TextInputEditText usernameText;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordText;
    private TextInputLayout urlLayout;
    private TextInputEditText urlText;
    private TextInputLayout notesLayout;
    private TextInputEditText notesText;
    private MaterialButton generatePasswordButton;
    private MaterialCardView passwordStrengthCard;
    private ProgressBar passwordStrengthBar;
    private TextView passwordStrengthText;
    private View loadingOverlay;
    private LinearProgressIndicator progressIndicator;
    private MaterialButton saveButton;
    private BackendService backendService;
    private int passwordId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取传递的密码ID
        if (getArguments() != null) {
            passwordId = getArguments().getInt("passwordId", -1);
        }

        // TODO: 获取BackendService实例
        backendService = null; // 通过依赖注入获取
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupToolbar();
        setupTextWatchers();
        setupClickListeners();
        setupObservers();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        titleLayout = view.findViewById(R.id.title_layout);
        titleText = view.findViewById(R.id.title_text);
        usernameLayout = view.findViewById(R.id.username_layout);
        usernameText = view.findViewById(R.id.username_text);
        passwordLayout = view.findViewById(R.id.password_layout);
        passwordText = view.findViewById(R.id.password_text);
        urlLayout = view.findViewById(R.id.url_layout);
        urlText = view.findViewById(R.id.url_text);
        notesLayout = view.findViewById(R.id.notes_layout);
        notesText = view.findViewById(R.id.notes_text);
        generatePasswordButton = view.findViewById(R.id.btn_generate_password);
        passwordStrengthCard = view.findViewById(R.id.password_strength_card);
        passwordStrengthBar = view.findViewById(R.id.password_strength_bar);
        passwordStrengthText = view.findViewById(R.id.password_strength_text);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        saveButton = view.findViewById(R.id.btn_save);
    }

    private void initViewModel() {
        // TODO: 通过ViewModelFactory创建ViewModel
        // EditPasswordViewModelFactory factory = new EditPasswordViewModelFactory(backendService);
        // viewModel = new ViewModelProvider(requireActivity(), factory).get(EditPasswordViewModel.class);

        viewModel.loadPasswordItem(passwordId);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                handleBackNavigation();
            });

            // 设置标题
            boolean isNew = viewModel.isNewPassword.getValue() != null && viewModel.isNewPassword.getValue();
            toolbar.setTitle(isNew ? "新建密码" : "编辑密码");
        }
    }

    private void setupTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.markChanges();
                updatePasswordStrength();
                updateSaveButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        if (titleText != null) titleText.addTextChangedListener(textWatcher);
        if (usernameText != null) usernameText.addTextChangedListener(textWatcher);
        if (passwordText != null) passwordText.addTextChangedListener(textWatcher);
        if (urlText != null) urlText.addTextChangedListener(textWatcher);
        if (notesText != null) notesText.addTextChangedListener(textWatcher);
    }

    private void setupClickListeners() {
        // 生成密码按钮
        if (generatePasswordButton != null) {
            generatePasswordButton.setOnClickListener(v -> {
                showPasswordGeneratorDialog();
            });
        }

        // 保存按钮
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                savePassword();
            });
        }

        // 密码输入框的显示/隐藏按钮
        if (passwordLayout != null) {
            passwordLayout.setEndIconOnClickListener(v -> {
                // 切换密码显示/隐藏
                // 这里可以添加密码可见性切换逻辑
            });
        }
    }

    private void setupObservers() {
        // 观察密码条目
        viewModel.passwordItem.observe(getViewLifecycleOwner(), this::updatePasswordItem);

        // 观察加载状态
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            updateLoadingState(isLoading != null && isLoading);
        });

        // 观察错误信息
        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
                viewModel.clearError();
            }
        });

        // 观察保存状态
        viewModel.isSaved.observe(getViewLifecycleOwner(), isSaved -> {
            if (isSaved != null && isSaved) {
                Toast.makeText(requireContext(), "密码已保存", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });

        // 观察生成的密码
        viewModel.generatedPassword.observe(getViewLifecycleOwner(), password -> {
            if (password != null && passwordText != null) {
                passwordText.setText(password);
                updatePasswordStrength();
                Toast.makeText(requireContext(), "密码已生成", Toast.LENGTH_SHORT).show();
                viewModel.clearGeneratedPassword();
            }
        });

        // 观察是否为新密码
        viewModel.isNewPassword.observe(getViewLifecycleOwner(), isNew -> {
            if (toolbar != null) {
                toolbar.setTitle(isNew != null && isNew ? "新建密码" : "编辑密码");
            }
        });
    }

    private void updatePasswordItem(PasswordItem item) {
        if (item == null) return;

        if (titleText != null) titleText.setText(item.getTitle());
        if (usernameText != null) usernameText.setText(item.getUsername());
        if (passwordText != null) passwordText.setText(item.getPassword());
        if (urlText != null) urlText.setText(item.getUrl());
        if (notesText != null) notesText.setText(item.getNotes());

        updatePasswordStrength();
    }

    private void updateLoadingState(boolean isLoading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (progressIndicator != null) {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        // 禁用/启用输入框和按钮
        boolean enabled = !isLoading;
        if (titleText != null) titleText.setEnabled(enabled);
        if (usernameText != null) usernameText.setEnabled(enabled);
        if (passwordText != null) passwordText.setEnabled(enabled);
        if (urlText != null) urlText.setEnabled(enabled);
        if (notesText != null) notesText.setEnabled(enabled);
        if (generatePasswordButton != null) generatePasswordButton.setEnabled(enabled);
        if (saveButton != null) saveButton.setEnabled(enabled);
    }

    private void updatePasswordStrength() {
        if (passwordText == null || passwordStrengthBar == null || passwordStrengthText == null) {
            return;
        }

        String password = passwordText.getText().toString();
        if (password.isEmpty()) {
            passwordStrengthCard.setVisibility(View.GONE);
            return;
        }

        passwordStrengthCard.setVisibility(View.VISIBLE);
        var strength = viewModel.checkPasswordStrength(password);
        var description = viewModel.getPasswordStrengthDescription(strength);

        passwordStrengthBar.setProgress((strength.score() + 1) * 33); // 0-100
        passwordStrengthText.setText(description);

        // 设置颜色
        var colorRes = switch (strength.level()) {
            case WEAK -> R.color.strength_weak;
            case MEDIUM -> R.color.strength_medium;
            case STRONG -> R.color.strength_strong;
        };

        passwordStrengthBar.setProgressTintList(getResources().getColorStateList(colorRes));
        passwordStrengthText.setTextColor(getResources().getColor(colorRes));
    }

    private void updateSaveButtonState() {
        if (saveButton == null) return;

        boolean hasTitle = titleText != null && !titleText.getText().toString().trim().isEmpty();
        boolean hasUsername = usernameText != null && !usernameText.getText().toString().trim().isEmpty();
        boolean hasPassword = passwordText != null && !passwordText.getText().toString().trim().isEmpty();

        saveButton.setEnabled(hasTitle && hasUsername && hasPassword);
    }

    private void savePassword() {
        String title = titleText != null ? titleText.getText().toString().trim() : "";
        String username = usernameText != null ? usernameText.getText().toString().trim() : "";
        String password = passwordText != null ? passwordText.getText().toString().trim() : "";
        String url = urlText != null ? urlText.getText().toString().trim() : "";
        String notes = notesText != null ? notesText.getText().toString().trim() : "";

        viewModel.savePassword(title, username, password, url, notes);
    }

    private void showPasswordGeneratorDialog() {
        GeneratePasswordDialog dialog = new GeneratePasswordDialog();
        dialog.setOnPasswordGeneratedListener((password, length, uppercase, lowercase, numbers, symbols) -> {
            // 设置生成的密码
            if (passwordText != null) {
                passwordText.setText(password);
                updatePasswordStrength();
            }
            // 保存生成参数
            viewModel.setPasswordGenerationParams(length, uppercase, lowercase, numbers, symbols);
        });
        dialog.show(getChildFragmentManager(), "PasswordGenerator");
    }

    private void handleBackNavigation() {
        if (viewModel.hasUnsavedChanges()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("未保存的更改")
                    .setMessage("您有未保存的更改，确定要退出吗？")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("退出", (dialog, which) -> {
                        Navigation.findNavController(requireView()).navigateUp();
                    })
                    .show();
        } else {
            Navigation.findNavController(requireView()).navigateUp();
        }
    }

    private void showError(String error) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
    }
}