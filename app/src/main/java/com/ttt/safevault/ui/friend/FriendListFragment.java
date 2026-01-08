package com.ttt.safevault.ui.friend;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ttt.safevault.R;
import com.ttt.safevault.adapter.FriendAdapter;
import com.ttt.safevault.model.Friend;
import com.ttt.safevault.viewmodel.FriendViewModel;
import com.ttt.safevault.viewmodel.ViewModelFactory;

/**
 * 好友列表Fragment
 */
public class FriendListFragment extends Fragment {

    private FriendViewModel viewModel;
    private FriendAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        initViewModel();
        setupRecyclerView();
        setupObservers();

        // 加载好友列表
        viewModel.loadFriendList();
    }

    private void initViews(View view) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh);
        emptyView = view.findViewById(R.id.empty_view);
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_friend);

        // 下拉刷新
        swipeRefresh.setOnRefreshListener(() -> viewModel.loadFriendList());

        // 添加好友按钮
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddFriendActivity.class);
            startActivity(intent);
        });
    }

    private void initViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
            requireActivity().getApplication(),
            com.ttt.safevault.ServiceLocator.getInstance().getBackendService()
        );
        viewModel = new ViewModelProvider(this, factory).get(FriendViewModel.class);
    }

    private void setupRecyclerView() {
        adapter = new FriendAdapter();
        adapter.setOnFriendActionListener(this::confirmRemoveFriend);

        RecyclerView recyclerView = requireView().findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
    }

    private void setupObservers() {
        // 观察好友列表
        viewModel.friendList.observe(getViewLifecycleOwner(), friends -> {
            adapter.setFriendList(friends);
            updateEmptyView(friends == null || friends.isEmpty());
        });

        // 观察加载状态
        viewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            swipeRefresh.setRefreshing(isLoading != null && isLoading);
        });

        // 观察错误信息
        viewModel.errorMessage.observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // 观察操作成功
        viewModel.operationSuccess.observe(getViewLifecycleOwner(), success -> {
            if (success) {
                viewModel.loadFriendList(); // 重新加载列表
            }
        });
    }

    private void updateEmptyView(boolean isEmpty) {
        emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void confirmRemoveFriend(Friend friend) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除好友")
            .setMessage("确定要删除好友 \"" + friend.getDisplayName() + "\" 吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除", (dialog, which) -> {
                viewModel.removeFriend(friend.getFriendId());
                Toast.makeText(requireContext(), "好友已删除", Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 刷新列表
        viewModel.loadFriendList();
    }
}
