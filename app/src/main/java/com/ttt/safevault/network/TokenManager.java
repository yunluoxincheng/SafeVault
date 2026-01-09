package com.ttt.safevault.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ttt.safevault.dto.response.AuthResponse;
import com.ttt.safevault.network.api.AuthServiceApi;

import io.reactivex.rxjava3.core.Observable;

/**
 * Token管理器
 * 负责Token的存储、获取、刷新
 */
public class TokenManager {
    private static final String TAG = "TokenManager";
    private static final String PREFS_NAME = "token_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_DISPLAY_NAME = "display_name";
    
    private final SharedPreferences prefs;
    private AuthServiceApi authApi;
    private static TokenManager instance;
    
    private TokenManager(Context context) {
        this.prefs = context.getApplicationContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized TokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new TokenManager(context);
        }
        return instance;
    }
    
    public void setAuthApi(AuthServiceApi authApi) {
        this.authApi = authApi;
    }
    
    /**
     * 保存Token
     */
    public void saveTokens(AuthResponse response) {
        if (response == null) {
            return;
        }
        
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, response.getAccessToken())
            .putString(KEY_REFRESH_TOKEN, response.getRefreshToken())
            .putString(KEY_USER_ID, response.getUserId())
            .putString(KEY_DISPLAY_NAME, response.getDisplayName())
            .apply();
        
        Log.d(TAG, "Tokens saved for user: " + response.getDisplayName());
    }
    
    /**
     * 获取访问Token
     */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    /**
     * 获取刷新Token
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    /**
     * 获取用户ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return prefs.getString(KEY_DISPLAY_NAME, null);
    }
    
    /**
     * 刷新Token
     */
    public Observable<AuthResponse> refreshToken() {
        String refreshToken = getRefreshToken();
        if (refreshToken == null || authApi == null) {
            return Observable.error(new IllegalStateException("No refresh token or auth API not set"));
        }
        
        return authApi.refreshToken("Bearer " + refreshToken)
            .doOnNext(this::saveTokens)
            .doOnError(error -> {
                Log.e(TAG, "Failed to refresh token", error);
                clearTokens();
            });
    }
    
    /**
     * 清除Token
     */
    public void clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_DISPLAY_NAME)
            .apply();
        
        Log.d(TAG, "Tokens cleared");
    }
    
    /**
     * 是否已登录
     */
    public boolean isLoggedIn() {
        return getAccessToken() != null;
    }
}
