package com.ttt.safevault.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ttt.safevault.R;
import com.ttt.safevault.adapter.PasswordListAdapter;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.model.PasswordItem;
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
    private TextView emptyText;
    private View loadingLayout;
    private PasswordListAdapter adapter;
    private BackendService backendService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: 获取BackendService实例
        backendService = null; // 通过依赖注入获取
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
        emptyText = view.findViewById(R.id.empty_text);
        loadingLayout = view.findViewById(R.id.loading_layout);
    }

    private void initViewModel() {
        // TODO: 通过ViewModelFactory创建ViewModel - 需要BackendService实现
        // PasswordListViewModelFactory factory = new PasswordListViewModelFactory(backendService);
        // viewModel = new ViewModelProvider(requireActivity(), factory).get(PasswordListViewModel.class);

        // 临时设置viewModel为null以避免编译错误，实际使用时需要实现BackendService
        viewModel = null;
    }

    private void setupRecyclerView() {
        adapter = new PasswordListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        // 添加分割线装饰
        // recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
    }

    private void setupObservers() {
        // TODO: 待ViewModel实现后取消注释以下代码
        /*
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
        */
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // TODO: 待ViewModel实现后取消注释
            // viewModel.refresh();

            // 临时隐藏刷新状态
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void updateEmptyState(List<PasswordItem> items) {
        boolean isEmpty = items == null || items.isEmpty();

        if (emptyText != null) {
            emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (isEmpty) {
                // TODO: 待ViewModel实现后恢复完整逻辑
                /*
                if (viewModel.isSearching.getValue() != null && viewModel.isSearching.getValue()) {
                    emptyText.setText(R.string.no_search_results);
                } else {
                    emptyText.setText(R.string.no_passwords);
                }
                */
                emptyText.setText(R.string.no_passwords);
            }
        }

        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void updateEmptyState(boolean isEmpty) {
        if (emptyText != null) {
            emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

            if (isEmpty) {
                // TODO: 待ViewModel实现后恢复完整逻辑
                /*
                if (viewModel.isSearching.getValue() != null && viewModel.isSearching.getValue()) {
                    emptyText.setText(R.string.no_search_results);
                } else {
                    emptyText.setText(R.string.no_passwords);
                }
                */
                emptyText.setText(R.string.no_passwords);
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
        // TODO: 待ViewModel实现后取消注释
        // viewModel.copyPassword(item.getId());

        // 临时提示功能未实现
        showError("复制功能待BackendService和ViewModel实现后可用");
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
                    // TODO: 待ViewModel实现后取消注释
                    // viewModel.deletePasswordItem(item.getId());

                    // 临时提示功能未实现
                    showError("删除功能待BackendService和ViewModel实现后可用");
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO: 待ViewModel实现后取消注释
        // 每次返回时刷新数据
        // if (viewModel != null) {
        //     viewModel.refresh();
        // }
    }
}