package com.ttt.safevault.network.api;

import com.ttt.safevault.dto.request.LoginByUsernameRequest;
import com.ttt.safevault.dto.request.LoginRequest;
import com.ttt.safevault.dto.request.RegisterRequest;
import com.ttt.safevault.dto.response.AuthResponse;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * 认证服务API接口
 */
public interface AuthServiceApi {

    @POST("v1/auth/register")
    Observable<AuthResponse> register(@Body RegisterRequest request);

    @POST("v1/auth/login")
    Observable<AuthResponse> login(@Body LoginRequest request);

    @POST("v1/auth/login/by-username")
    Observable<AuthResponse> loginByUsername(@Body LoginByUsernameRequest request);

    @POST("v1/auth/refresh")
    Observable<AuthResponse> refreshToken(@Header("Authorization") String refreshToken);
}
