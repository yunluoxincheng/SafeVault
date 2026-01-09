package com.ttt.safevault.network;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.ttt.safevault.dto.OnlineUserMessage;
import com.ttt.safevault.dto.ShareNotificationMessage;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * WebSocket管理器
 * 处理实时分享通知和在线用户更新
 */
public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    
    private WebSocket webSocket;
    private final OkHttpClient client;
    private final Gson gson;
    private WebSocketEventListener eventListener;
    private boolean isConnected = false;
    
    public interface WebSocketEventListener {
        void onShareNotification(ShareNotificationMessage notification);
        void onOnlineUserUpdate(OnlineUserMessage message);
        void onConnectionOpened();
        void onConnectionClosed();
        void onError(String error);
    }
    
    public WebSocketManager() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }
    
    /**
     * 连接WebSocket
     * @param token 认证Token
     * @param listener 事件监听器
     */
    public void connect(String token, WebSocketEventListener listener) {
        if (isConnected) {
            Log.w(TAG, "WebSocket already connected");
            return;
        }
        
        this.eventListener = listener;
        
        Request request = new Request.Builder()
            .url(ApiConstants.WS_URL + "?token=" + token)
            .build();
        
        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
                isConnected = true;
                Log.d(TAG, "WebSocket connected");
                if (eventListener != null) {
                    eventListener.onConnectionOpened();
                }
            }
            
            @Override
            public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
                Log.d(TAG, "WebSocket message received: " + text);
                handleMessage(text);
            }
            
            @Override
            public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                Log.d(TAG, "WebSocket closing: " + reason);
                webSocket.close(1000, null);
            }
            
            @Override
            public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
                isConnected = false;
                Log.d(TAG, "WebSocket closed: " + reason);
                if (eventListener != null) {
                    eventListener.onConnectionClosed();
                }
            }
            
            @Override
            public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
                isConnected = false;
                Log.e(TAG, "WebSocket error", t);
                if (eventListener != null) {
                    eventListener.onError(t.getMessage());
                }
            }
        });
    }
    
    /**
     * 断开WebSocket连接
     */
    public void disconnect() {
        if (webSocket != null) {
            webSocket.close(1000, "Client disconnect");
            webSocket = null;
            isConnected = false;
        }
    }
    
    /**
     * 发送心跳
     */
    public void sendHeartbeat() {
        if (webSocket != null && isConnected) {
            webSocket.send("{\"type\":\"heartbeat\"}");
        }
    }
    
    /**
     * 处理收到的消息
     */
    private void handleMessage(String message) {
        try {
            // 简单解析消息类型
            if (message.contains("\"type\":\"SHARE_NOTIFICATION\"") || 
                message.contains("\"type\":\"NEW_SHARE\"") ||
                message.contains("\"type\":\"SHARE_REVOKED\"")) {
                ShareNotificationMessage notification = gson.fromJson(message, ShareNotificationMessage.class);
                if (eventListener != null) {
                    eventListener.onShareNotification(notification);
                }
            } else if (message.contains("\"type\":\"ONLINE_USER\"")) {
                OnlineUserMessage userMessage = gson.fromJson(message, OnlineUserMessage.class);
                if (eventListener != null) {
                    eventListener.onOnlineUserUpdate(userMessage);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse WebSocket message", e);
        }
    }
    
    public boolean isConnected() {
        return isConnected;
    }
}
