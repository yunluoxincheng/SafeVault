package com.ttt.safevault.ui.friend;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.ttt.safevault.R;
import com.ttt.safevault.ui.share.ScanQRCodeActivity;
import com.ttt.safevault.viewmodel.FriendViewModel;
import com.ttt.safevault.viewmodel.ViewModelFactory;

/**
 * 添加好友Activity
 */
public class AddFriendActivity extends AppCompatActivity {

    private static final String TAG = "AddFriendActivity";
    private static final int REQUEST_SCAN_QR = 1001;

    private FriendViewModel viewModel;
    private TextInputLayout inputLayoutFriendId;
    private TextInputEditText inputFriendId;
    private MaterialButton btnAddById;
    private MaterialButton btnScanQr;
    private MaterialButton btnShowMyQr;
    private CircularProgressIndicator progressIndicator;

    private final ActivityResultLauncher<String> requestCameraPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                startScanQRCode();
            } else {
                Toast.makeText(this, "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        initViews();
        initViewModel();
        setupObservers();
        setupListeners();
    }

    private void initViews() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        inputLayoutFriendId = findViewById(R.id.input_layout_friend_id);
        inputFriendId = findViewById(R.id.input_friend_id);
        btnAddById = findViewById(R.id.btn_add_by_id);
        btnScanQr = findViewById(R.id.btn_scan_qr);
        btnShowMyQr = findViewById(R.id.btn_show_my_qr);
        progressIndicator = findViewById(R.id.progress_indicator);
    }

    private void initViewModel() {
        ViewModelFactory factory = new ViewModelFactory(
            getApplication(),
            com.ttt.safevault.ServiceLocator.getInstance().getBackendService()
        );
        viewModel = new ViewModelProvider(this, factory).get(FriendViewModel.class);
    }

    private void setupObservers() {
        // 观察加载状态
        viewModel.isLoading.observe(this, isLoading -> {
            progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            btnAddById.setEnabled(!isLoading);
            btnScanQr.setEnabled(!isLoading);
            btnShowMyQr.setEnabled(!isLoading);
        });

        // 观察错误信息
        viewModel.errorMessage.observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                viewModel.clearError();
            }
        });

        // 观察操作成功
        viewModel.operationSuccess.observe(this, success -> {
            if (success) {
                Toast.makeText(this, "好友添加成功", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void setupListeners() {
        // 通过ID添加
        btnAddById.setOnClickListener(v -> {
            String friendId = inputFriendId.getText() != null ? 
                inputFriendId.getText().toString().trim() : "";
            
            if (friendId.isEmpty()) {
                inputLayoutFriendId.setError("请输入好友ID");
                return;
            }
            
            inputLayoutFriendId.setError(null);
            viewModel.addFriend(friendId);
        });

        // 扫描二维码
        btnScanQr.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                startScanQRCode();
            } else {
                requestCameraPermission.launch(Manifest.permission.CAMERA);
            }
        });

        // 显示我的二维码
        btnShowMyQr.setOnClickListener(v -> {
            Intent intent = new Intent(this, UserQRCodeActivity.class);
            startActivity(intent);
        });
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startScanQRCode() {
        Intent intent = new Intent(this, ScanQRCodeActivity.class);
        intent.putExtra("scan_type", "friend");
        startActivityForResult(intent, REQUEST_SCAN_QR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_SCAN_QR && resultCode == RESULT_OK && data != null) {
            String friendData = data.getStringExtra("qr_data");
            if (friendData != null && friendData.startsWith("safevault://user/")) {
                String friendId = friendData.replace("safevault://user/", "");
                viewModel.addFriend(friendId);
            } else {
                Toast.makeText(this, "无效的好友二维码", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
