package com.ttt.safevault.ui.share;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.BeepManager;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.google.zxing.BarcodeFormat;
import com.ttt.safevault.R;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 扫描二维码Activity
 * 使用ZXing库实现二维码扫描功能
 */
public class ScanQRCodeActivity extends AppCompatActivity {

    private static final String TAG = "ScanQRCodeActivity";
    private DecoratedBarcodeView barcodeView;
    private BeepManager beepManager;
    private String scanType; // "share" 或 "friend"

    private final ActivityResultLauncher<String> requestCameraPermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                startScanning();
            } else {
                Toast.makeText(this, "需要相机权限才能扫描二维码", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() == null) {
                return;
            }

            // 播放提示音
            beepManager.playBeepSoundAndVibrate();

            // 处理扫描结果
            handleScanResult(result.getText());
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // 可选：绘制可能的结果点
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr_code);

        // 获取扫描类型
        scanType = getIntent().getStringExtra("scan_type");
        if (scanType == null) {
            scanType = "share"; // 默认为分享扫描
        }

        // 初始化扫描视图
        barcodeView = findViewById(R.id.barcode_scanner);
        
        // 配置扫描格式（只扫描二维码）
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callback);

        // 初始化提示音管理器
        beepManager = new BeepManager(this);

        // 设置toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            String title = "friend".equals(scanType) ? "扫描好友二维码" : "扫描分享二维码";
            getSupportActionBar().setTitle(title);
        }

        // 检查权限
        if (checkCameraPermission()) {
            startScanning();
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void startScanning() {
        barcodeView.resume();
    }

    private void handleScanResult(String result) {
        // 暂停扫描
        barcodeView.pause();

        // 根据扫描类型处理结果
        if ("friend".equals(scanType)) {
            // 好友二维码：safevault://user/{userId}
            if (result.startsWith("safevault://user/")) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("qr_data", result);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "无效的好友二维码", Toast.LENGTH_SHORT).show();
                // 继续扫描
                barcodeView.resume();
            }
        } else {
            // 分享二维码：safevault://share/{shareId}
            if (result.startsWith("safevault://share/")) {
                // 启动接收分享Activity
                Intent intent = new Intent(this, ReceiveShareActivity.class);
                intent.setData(android.net.Uri.parse(result));
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "无效的分享二维码", Toast.LENGTH_SHORT).show();
                // 继续扫描
                barcodeView.resume();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkCameraPermission()) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeView.pause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
