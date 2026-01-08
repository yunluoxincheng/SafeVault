package com.ttt.safevault.ui.share;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.ttt.safevault.R;

/**
 * 分享历史Fragment
 * 包含两个Tab：我的分享和接收的分享
 */
public class ShareHistoryFragment extends Fragment {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private MaterialToolbar toolbar;

    private final ActivityResultLauncher<String> requestCameraPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                startScanQRCode();
            } else {
                Toast.makeText(requireContext(), "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT).show();
            }
        });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_share_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupMenu();
        setupViewPager();
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        tabLayout = view.findViewById(R.id.tab_layout);
        viewPager = view.findViewById(R.id.view_pager);

        // 设置工具栏
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
    }

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.share_history_menu, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_scan_qr) {
                    onScanQRCodeClick();
                    return true;
                } else if (menuItem.getItemId() == R.id.action_bluetooth_receive) {
                    onBluetoothReceiveClick();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void onScanQRCodeClick() {
        // 检查相机权限
        if (checkCameraPermission()) {
            startScanQRCode();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startScanQRCode() {
        Intent intent = new Intent(requireContext(), ScanQRCodeActivity.class);
        intent.putExtra("scan_type", "share");
        startActivity(intent);
    }
    
    private void onBluetoothReceiveClick() {
        // 直接跳转到蓝牙接收界面
        Intent intent = new Intent(requireContext(), BluetoothReceiveActivity.class);
        startActivity(intent);
    }

    private void setupViewPager() {
        SharePagerAdapter adapter = new SharePagerAdapter(this);
        viewPager.setAdapter(adapter);

        // 连接TabLayout和ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                tab.setText(position == 0 ? "我的分享" : "接收的分享");
            }
        ).attach();
    }

    /**
     * ViewPager2适配器
     */
    private static class SharePagerAdapter extends FragmentStateAdapter {

        public SharePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            // position 0: 我的分享, position 1: 接收的分享
            return ShareListFragment.newInstance(position == 0);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}
