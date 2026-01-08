package com.ttt.safevault.ui.friend;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.ttt.safevault.R;
import com.ttt.safevault.utils.QRCodeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 用户二维码Activity
 */
public class UserQRCodeActivity extends AppCompatActivity {

    private static final String TAG = "UserQRCodeActivity";

    private TextView textDisplayName;
    private TextView textUserId;
    private ImageView imgQRCode;
    private MaterialButton btnSaveQr;
    private MaterialButton btnShareQr;
    private CircularProgressIndicator progressIndicator;

    private Bitmap qrCodeBitmap;
    private String userId;

    private final ActivityResultLauncher<String> requestStoragePermission =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) {
                saveQRCodeToGallery();
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_qrcode);

        initViews();
        loadUserData();
        setupListeners();
    }

    private void initViews() {
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        textDisplayName = findViewById(R.id.text_display_name);
        textUserId = findViewById(R.id.text_user_id);
        imgQRCode = findViewById(R.id.img_qr_code);
        btnSaveQr = findViewById(R.id.btn_save_qr);
        btnShareQr = findViewById(R.id.btn_share_qr);
        progressIndicator = findViewById(R.id.progress_indicator);
    }

    private void loadUserData() {
        progressIndicator.setVisibility(View.VISIBLE);

        // TODO: 从后端或本地存储获取用户信息
        // 这里使用模拟数据
        userId = "user_" + System.currentTimeMillis();
        String displayName = "当前用户";

        textDisplayName.setText(displayName);
        textUserId.setText(userId);

        // 生成二维码
        generateQRCode();

        progressIndicator.setVisibility(View.GONE);
    }

    private void generateQRCode() {
        try {
            String qrData = "safevault://user/" + userId;
            qrCodeBitmap = QRCodeUtils.generateQRCode(qrData, 512, 512);
            imgQRCode.setImageBitmap(qrCodeBitmap);
        } catch (Exception e) {
            Toast.makeText(this, "生成二维码失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupListeners() {
        // 保存二维码
        btnSaveQr.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 不需要存储权限
                saveQRCodeToGallery();
            } else {
                if (checkStoragePermission()) {
                    saveQRCodeToGallery();
                } else {
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
        });

        // 分享二维码
        btnShareQr.setOnClickListener(v -> shareQRCode());
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, 
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void saveQRCodeToGallery() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "二维码未生成", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用 MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, "SafeVault_QR_" + userId + ".png");
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SafeVault");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        Toast.makeText(this, "二维码已保存到相册", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                // Android 9 及以下
                File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                File safevaultDir = new File(picturesDir, "SafeVault");
                if (!safevaultDir.exists()) {
                    safevaultDir.mkdirs();
                }

                File imageFile = new File(safevaultDir, "SafeVault_QR_" + userId + ".png");
                try (FileOutputStream out = new FileOutputStream(imageFile)) {
                    qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    
                    // 通知系统扫描新文件
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    mediaScanIntent.setData(Uri.fromFile(imageFile));
                    sendBroadcast(mediaScanIntent);
                    
                    Toast.makeText(this, "二维码已保存到相册", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (IOException e) {
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareQRCode() {
        if (qrCodeBitmap == null) {
            Toast.makeText(this, "二维码未生成", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 保存到缓存目录
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "qr_code.png");
            
            try (FileOutputStream out = new FileOutputStream(imageFile)) {
                qrCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }

            // 使用FileProvider获取URI
            Uri contentUri = FileProvider.getUriForFile(this, 
                getPackageName() + ".fileprovider", imageFile);

            // 分享
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "扫描二维码添加我为好友！");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "分享二维码"));
        } catch (IOException e) {
            Toast.makeText(this, "分享失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
