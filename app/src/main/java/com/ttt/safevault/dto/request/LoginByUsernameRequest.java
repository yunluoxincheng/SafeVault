package com.ttt.safevault.dto.request;

/**
 * 通过用户名登录请求
 */
public class LoginByUsernameRequest {
    private String username;
    private String deviceId;
    private String signature;

    public LoginByUsernameRequest() {
    }

    public LoginByUsernameRequest(String username, String deviceId, String signature) {
        this.username = username;
        this.deviceId = deviceId;
        this.signature = signature;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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
