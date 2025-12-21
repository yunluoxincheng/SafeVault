package com.ttt.safevault.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
import com.ttt.safevault.viewmodel.PasswordDetailViewModel;

/**
 * 密码详情Fragment
 * 展示密码条目的详细信息
 */
public class PasswordDetailFragment extends Fragment {

    private PasswordDetailViewModel viewModel;
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
    private ImageView passwordVisibilityIcon;
    private View loadingOverlay;
    private LinearProgressIndicator progressIndicator;
    private MaterialCardView usernameCard;
    private MaterialCardView passwordCard;
    private MaterialCardView urlCard;
    private MaterialButton editButton;
    private MaterialButton deleteButton;
    private MaterialButton shareButton;
    private View copyUsernameStatus;
    private View copyPasswordStatus;
    private View copyUrlStatus;
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
        return inflater.inflate(R.layout.fragment_password_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupToolbar();
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
        passwordVisibilityIcon = view.findViewById(R.id.password_visibility_icon);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        usernameCard = view.findViewById(R.id.username_card);
        passwordCard = view.findViewById(R.id.password_card);
        urlCard = view.findViewById(R.id.url_card);
        editButton = view.findViewById(R.id.btn_edit);
        deleteButton = view.findViewById(R.id.btn_delete);
        shareButton = view.findViewById(R.id.btn_share);
        copyUsernameStatus = view.findViewById(R.id.copy_username_status);
        copyPasswordStatus = view.findViewById(R.id.copy_password_status);
        copyUrlStatus = view.findViewById(R.id.copy_url_status);
    }

    private void initViewModel() {
        // TODO: 通过ViewModelFactory创建ViewModel
        // PasswordDetailViewModelFactory factory = new PasswordDetailViewModelFactory(backendService);
        // viewModel = new ViewModelProvider(requireActivity(), factory).get(PasswordDetailViewModel.class);

        if (passwordId >= 0) {
            viewModel.loadPasswordItem(passwordId);
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                Navigation.findNavController(v).navigateUp();
            });

            toolbar.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_edit) {
                    navigateToEdit();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupClickListeners() {
        // 密码显示/隐藏
        passwordVisibilityIcon.setOnClickListener(v -> {
            viewModel.togglePasswordVisibility();
        });

        // 复制用户名
        if (usernameCard != null) {
            usernameCard.setOnClickListener(v -> {
                viewModel.copyUsername();
            });
        }

        // 复制密码
        if (passwordCard != null) {
            passwordCard.setOnClickListener(v -> {
                viewModel.copyPassword();
            });
        }

        // 复制URL
        if (urlCard != null) {
            urlCard.setOnClickListener(v -> {
                viewModel.copyUrl();
            });
        }

        // 编辑按钮
        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                navigateToEdit();
            });
        }

        // 删除按钮
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                confirmDelete();
            });
        }

        // 分享按钮
        if (shareButton != null) {
            shareButton.setOnClickListener(v -> {
                sharePassword();
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

        // 观察密码显示状态
        viewModel.isPasswordVisible.observe(getViewLifecycleOwner(), isVisible -> {
            updatePasswordVisibility(isVisible != null && isVisible);
        });

        // 观察复制状态
        viewModel.copiedField.observe(getViewLifecycleOwner(), field -> {
            updateCopyStatus(field);
        });

        // 观察删除状态
        viewModel.isDeleted.observe(getViewLifecycleOwner(), isDeleted -> {
            if (isDeleted != null && isDeleted) {
                Toast.makeText(requireContext(), "密码已删除", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).navigateUp();
            }
        });
    }

    private void updatePasswordItem(PasswordItem item) {
        if (item == null) return;

        // 标题
        titleText.setText(item.getTitle());

        // 用户名
        if (!TextUtils.isEmpty(item.getUsername())) {
            usernameText.setText(item.getUsername());
            usernameLayout.setVisibility(View.VISIBLE);
        } else {
            usernameLayout.setVisibility(View.GONE);
        }

        // 密码（总是显示，但内容可能隐藏）
        passwordText.setText(item.getPassword());

        // URL
        if (!TextUtils.isEmpty(item.getUrl())) {
            urlText.setText(item.getUrl());
            urlLayout.setVisibility(View.VISIBLE);
        } else {
            urlLayout.setVisibility(View.GONE);
        }

        // 备注
        if (!TextUtils.isEmpty(item.getNotes())) {
            notesText.setText(item.getNotes());
            notesLayout.setVisibility(View.VISIBLE);
        } else {
            notesLayout.setVisibility(View.GONE);
        }

        // 更新Toolbar标题
        if (toolbar != null) {
            toolbar.setTitle(item.getDisplayName());
        }
    }

    private void updateLoadingState(boolean isLoading) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        if (progressIndicator != null) {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }

        // 禁用/启用按钮
        if (editButton != null) {
            editButton.setEnabled(!isLoading);
        }
        if (deleteButton != null) {
            deleteButton.setEnabled(!isLoading);
        }
        if (shareButton != null) {
            shareButton.setEnabled(!isLoading);
        }
    }

    private void updatePasswordVisibility(boolean isVisible) {
        if (passwordVisibilityIcon != null) {
            passwordVisibilityIcon.setImageResource(
                    isVisible ? R.drawable.ic_visibility_off : R.drawable.ic_visibility
            );
        }

        if (passwordText != null) {
            passwordText.setInputType(isVisible ?
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD :
                    android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            );
        }
    }

    private void updateCopyStatus(Integer field) {
        // 重置所有状态
        if (copyUsernameStatus != null) {
            copyUsernameStatus.setVisibility(View.GONE);
        }
        if (copyPasswordStatus != null) {
            copyPasswordStatus.setVisibility(View.GONE);
        }
        if (copyUrlStatus != null) {
            copyUrlStatus.setVisibility(View.GONE);
        }

        // 显示对应状态
        if (field != null) {
            switch (field) {
                case 0: // 用户名
                    if (copyUsernameStatus != null) {
                        copyUsernameStatus.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(requireContext(), "用户名已复制", Toast.LENGTH_SHORT).show();
                    break;
                case 1: // 密码
                    if (copyPasswordStatus != null) {
                        copyPasswordStatus.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(requireContext(), "密码已复制", Toast.LENGTH_SHORT).show();
                    break;
                case 2: // URL
                    if (copyUrlStatus != null) {
                        copyUrlStatus.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(requireContext(), "网址已复制", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void navigateToEdit() {
        Bundle bundle = new Bundle();
        bundle.putInt("passwordId", passwordId);

        Navigation.findNavController(requireView())
                .navigate(R.id.action_passwordDetailFragment_to_editPasswordFragment, bundle);
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除密码")
                .setMessage("确定要删除这个密码吗？此操作无法撤销。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.deletePassword();
                })
                .show();
    }

    private void sharePassword() {
        String shareText = viewModel.getShareText();
        if (shareText != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "分享密码"));
        }
    }

    private void showError(String error) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 刷新数据
        if (viewModel != null) {
            viewModel.refresh();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 暂停时隐藏密码
        if (viewModel != null) {
            viewModel.hidePassword();
        }
    }
}