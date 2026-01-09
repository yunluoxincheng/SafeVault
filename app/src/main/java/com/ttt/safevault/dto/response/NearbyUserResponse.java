package com.ttt.safevault.dto.response;

/**
 * 附近用户响应DTO
 */
public class NearbyUserResponse {
    private String userId;
    private String displayName;
    private double distance;     // 距离（米）
    private boolean isOnline;

    public NearbyUserResponse() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }
}
