package com.ttt.safevault.network;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ttt.safevault.network.api.AuthServiceApi;
import com.ttt.safevault.network.api.DiscoveryServiceApi;
import com.ttt.safevault.network.api.ShareServiceApi;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit客户端单例
 */
public class RetrofitClient {
    private static RetrofitClient instance;
    private final Retrofit retrofit;
    private final TokenManager tokenManager;
    
    private AuthServiceApi authServiceApi;
    private ShareServiceApi shareServiceApi;
    private DiscoveryServiceApi discoveryServiceApi;
    
    private RetrofitClient(Context context) {
        tokenManager = TokenManager.getInstance(context);
        
        // 日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // 认证拦截器
        AuthInterceptor authInterceptor = new AuthInterceptor(tokenManager);
        
        // 构建OkHttpClient
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build();
        
        // 构建Gson
        Gson gson = new GsonBuilder()
            .setLenient()
            .create();
        
        // 构建Retrofit
        retrofit = new Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build();
        
        // 创建API服务
        authServiceApi = retrofit.create(AuthServiceApi.class);
        shareServiceApi = retrofit.create(ShareServiceApi.class);
        discoveryServiceApi = retrofit.create(DiscoveryServiceApi.class);
        
        // 设置TokenManager的authApi
        tokenManager.setAuthApi(authServiceApi);
    }
    
    public static synchronized RetrofitClient getInstance(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context.getApplicationContext());
        }
        return instance;
    }
    
    public AuthServiceApi getAuthServiceApi() {
        return authServiceApi;
    }
    
    public ShareServiceApi getShareServiceApi() {
        return shareServiceApi;
    }
    
    public DiscoveryServiceApi getDiscoveryServiceApi() {
        return discoveryServiceApi;
    }
    
    public TokenManager getTokenManager() {
        return tokenManager;
    }
}
