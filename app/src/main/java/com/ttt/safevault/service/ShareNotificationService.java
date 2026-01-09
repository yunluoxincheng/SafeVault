package com.ttt.safevault.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.gson.Gson;
import com.ttt.safevault.R;
import com.ttt.safevault.dto.ShareNotificationMessage;
import com.ttt.safevault.network.TokenManager;
import com.ttt.safevault.network.WebSocketManager;
import com.ttt.safevault.ui.share.ReceiveShareActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 分享通知服务
 * 维护 WebSocket 连接并处理实时分享通知
 */
public class ShareNotificationService extends Service {

    private static final String TAG = "ShareNotificationService";
    private static final String CHANNEL_ID = "share_notification_channel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SERVICE_NOTIFICATION_ID = 2001;

    private WebSocketManager webSocketManager;
    private TokenManager tokenManager;
    private Gson gson;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        tokenManager = TokenManager.getInstance(this);
        gson = new Gson();
        webSocketManager = new WebSocketManager();

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service start command");

        if (!tokenManager.isLoggedIn()) {
            Log.d(TAG, "User not logged in, stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (isRunning.compareAndSet(false, true)) {
            // 启动前台服务
            startForeground(SERVICE_NOTIFICATION_ID, createForegroundNotification());

            // 连接 WebSocket
            connectWebSocket();
        }

        return START_STICKY;
    }

    private void connectWebSocket() {
        String token = tokenManager.getAccessToken();
        if (token == null || token.isEmpty()) {
            Log.e(TAG, "No access token available");
            stopSelf();
            return;
        }

        webSocketManager.connect(token, new WebSocketManager.WebSocketEventListener() {
            @Override
            public void onShareNotification(ShareNotificationMessage notification) {
                handleShareNotification(notification);
            }

            @Override
            public void onOnlineUserUpdate(com.ttt.safevault.dto.OnlineUserMessage message) {
                Log.d(TAG, "Online user update: " + message);
            }

            @Override
            public void onConnectionOpened() {
                Log.d(TAG, "WebSocket connection opened");
            }

            @Override
            public void onConnectionClosed() {
                Log.d(TAG, "WebSocket connection closed");
                // 自动重连
                if (isRunning.get()) {
                    connectWebSocket();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "WebSocket error: " + error);
            }
        });
    }

    private void handleShareNotification(ShareNotificationMessage notification) {
        Log.d(TAG, "Received share notification: " + notification.getShareId());

        // 显示系统通知
        showShareNotification(notification);
    }

    private void showShareNotification(ShareNotificationMessage notification) {
        NotificationManager notificationManager =
            (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // 创建点击通知的 Intent
        Intent intent = new Intent(this, ReceiveShareActivity.class);
        intent.putExtra("SHARE_ID", notification.getShareId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 构建通知
        String fromUser = notification.getFromDisplayName() != null ?
            notification.getFromDisplayName() : notification.getFromUserId();
        String contentText = fromUser + " 与您分享了一个密码";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_share)
            .setContentTitle("收到新的密码分享")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        // 显示通知
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private Notification createForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SafeVault")
            .setContentText("正在接收分享通知...")
            .setSmallIcon(R.drawable.ic_share)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "分享通知",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("接收新的密码分享通知");
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        isRunning.set(false);

        if (webSocketManager != null) {
            webSocketManager.disconnect();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
