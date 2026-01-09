package com.ttt.safevault.network;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 认证拦截器
 * 自动添加Authorization头，处理401错误
 */
public class AuthInterceptor implements Interceptor {
    private static final String TAG = "AuthInterceptor";
    private final TokenManager tokenManager;
    
    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }
    
    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request original = chain.request();
        
        // 如果是刷新Token的请求,直接放行
        if (original.url().encodedPath().contains("/auth/refresh")) {
            return chain.proceed(original);
        }
        
        // 添加Authorization头
        String token = tokenManager.getAccessToken();
        Request.Builder requestBuilder = original.newBuilder();
        
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }
        
        Request request = requestBuilder.build();
        Response response = chain.proceed(request);
        
        // 处理401错误 - Token过期
        if (response.code() == 401 && token != null) {
            Log.d(TAG, "Token expired, attempting to refresh");
            response.close();
            
            // 尝试刷新Token (同步方式)
            try {
                String newToken = refreshTokenSync();
                if (newToken != null) {
                    // 使用新Token重试请求
                    Request newRequest = original.newBuilder()
                        .header("Authorization", "Bearer " + newToken)
                        .build();
                    return chain.proceed(newRequest);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to refresh token", e);
            }
        }
        
        return response;
    }
    
    /**
     * 同步刷新Token
     */
    private String refreshTokenSync() {
        try {
            // 这里需要同步执行刷新操作
            // 实际实现中可能需要使用CountDownLatch或其他同步机制
            return tokenManager.getAccessToken();
        } catch (Exception e) {
            Log.e(TAG, "Sync token refresh failed", e);
            return null;
        }
    }
}
