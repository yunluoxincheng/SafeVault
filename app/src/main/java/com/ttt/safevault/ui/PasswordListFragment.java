package com.ttt.safevault.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ttt.safevault.R;
import com.ttt.safevault.adapter.PasswordListAdapter;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
import com.ttt.safevault.utils.AnimationUtils;
import com.ttt.safevault.viewmodel.PasswordListViewModel;

import java.util.List;

/**
 * 密码列表Fragment
 * 展示所有密码条目
 */
public class PasswordListFragment extends Fragment implements PasswordListAdapter.OnItemClickListener {

    private PasswordListViewModel viewModel;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private View emptyLayout;
    private TextView emptyText;
    private TextView emptySubtext;
    private Button emptyAddButton;
    private View loadingLayout;
    private PasswordListAdapter adapter;
    private BackendService backendService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取BackendService实例
        backendService = com.ttt.safevault.ServiceLocator.getInstance().getBackendService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_password_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupRecyclerView();
        setupObservers();
        setupSwipeRefresh();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        emptyLayout = view.findViewById(R.id.empty_layout);
        emptyText = view.findViewById(R.id.empty_text);
        emptySubtext = view.findViewById(R.id.empty_subtext);
        emptyAddButton = view.findViewById(R.id.empty_add_button);
        loadingLayout = view.findViewById(R.id.loading_layout);

        // 设置空状态按钮点击事件
        if (emptyAddButton != null) {
            emptyAddButton.setOnClickListener(v -> {
                AnimationUtils.buttonPressFeedback(v);
                navigateToAddPassword();
            });
        }
    }

    private void navigateToAddPassword() {
        // 导航到添加密码页面（passwordId = -1 表示新增）
        Bundle bundle = new Bundle();
        bundle.putInt("passwordId", -1);

        Navigation.findNavController(requireView())
                .navigate(R.id.action_passwordListFragment_to_editPasswordFragment, bundle);
    }

    private void initViewModel() {
        // 通过ViewModelFactory创建ViewModel
        ViewModelProvider.Factory factory = new com.ttt.safevault.viewmodel.ViewModelFactory(requireActivity().getApplication());
        viewModel = new ViewModelProvider(requireActivity(), factory).get(PasswordListViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new PasswordListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // 添加分割线装饰
        // recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    private void setupObservers() {
        // 观察密码列表
        viewModel.passwordItems.observe(getViewLifecycleOwner(), items -> {
            adapter.submitList(items);
            updateEmptyState(items);
        });

        // 观察加载状态
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            swipeRefreshLayout.setRefreshing(isLoading);
            if (loadingLayout != null) {
                loadingLayout.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // 观察错误信息
        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                showError(error);
                viewModel.clearError();
            }
        });

        // 观察空状态
        viewModel.isEmpty.observe(getViewLifecycleOwner(), isEmpty -> {
            if (isEmpty != null) {
                updateEmptyState(isEmpty);
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            viewModel.refresh();
        });
    }

    private void updateEmptyState(List<PasswordItem> items) {
        boolean isEmpty = items == null || items.isEmpty();

        if (emptyLayout != null) {
            emptyLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (isEmpty && emptyText != null) {
                Boolean isSearching = viewModel.isSearching.getValue();
                if (isSearching != null && isSearching) {
                    emptyText.setText(R.string.no_search_results);
                } else {
                    emptyText.setText(R.string.no_passwords);
                }

                // 添加淡入动画
                if (emptyLayout.getVisibility() == View.VISIBLE) {
                    AnimationUtils.fadeIn(emptyLayout, 300);
                }
            }
        }

        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (emptyLayout != null) {
            emptyLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (isEmpty && emptyText != null) {
                Boolean isSearching = viewModel.isSearching.getValue();
                if (isSearching != null && isSearching) {
                    emptyText.setText(R.string.no_search_results);
                } else {
                    emptyText.setText(R.string.no_passwords);
                }

                // 添加淡入动画
                if (emptyLayout.getVisibility() == View.VISIBLE) {
                    AnimationUtils.fadeIn(emptyLayout, 300);
                }
            }
        }

        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showError(String error) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("错误")
                .setMessage(error)
                .setPositiveButton("确定", null)
                .show();
    }

    // PasswordListAdapter.OnItemClickListener 实现

    @Override
    public void onItemClick(PasswordItem item) {
        // 导航到密码详情页面
        Bundle bundle = new Bundle();
        bundle.putInt("passwordId", item.getId());

        Navigation.findNavController(requireView())
                .navigate(R.id.action_passwordListFragment_to_passwordDetailFragment, bundle);
    }

    @Override
    public void onItemCopyClick(PasswordItem item) {
        viewModel.copyPassword(item.getId());
    }

    @Override
    public void onItemEditClick(PasswordItem item) {
        // 导航到编辑页面
        Bundle bundle = new Bundle();
        bundle.putInt("passwordId", item.getId());

        Navigation.findNavController(requireView())
                .navigate(R.id.action_passwordListFragment_to_editPasswordFragment, bundle);
    }

    @Override
    public void onItemDeleteClick(PasswordItem item) {
        // 确认删除对话框
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除密码")
                .setMessage("确定要删除 \"" + item.getDisplayName() + "\" 吗？此操作无法撤销。")
                .setNegativeButton("取消", null)
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.deletePasswordItem(item.getId());
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次返回时刷新数据
        viewModel.refresh();
    }
}