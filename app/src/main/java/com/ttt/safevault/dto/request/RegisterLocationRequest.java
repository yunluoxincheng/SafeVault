package com.ttt.safevault.dto.request;

/**
 * 注册位置请求DTO
 */
public class RegisterLocationRequest {
    private double latitude;
    private double longitude;
    private double radius;  // 发现范围（米）

    public RegisterLocationRequest() {
    }

    public RegisterLocationRequest(double latitude, double longitude, double radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
