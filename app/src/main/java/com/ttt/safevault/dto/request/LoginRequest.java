package com.ttt.safevault.dto.request;

/**
 * 用户登录请求DTO
 */
public class LoginRequest {
    private String userId;
    private String deviceId;
    private String signature;

    public LoginRequest() {
    }

    public LoginRequest(String userId, String deviceId, String signature) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.signature = signature;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
