package com.ttt.safevault.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 用户配置文件数据模型
 * 用于表示SafeVault应用内的用户身份信息
 */
public class UserProfile implements Parcelable {
    private String userId;           // 唯一用户ID
    private String displayName;      // 显示名称
    private String publicKey;        // 公钥（用于加密）
    private long createdAt;          // 创建时间

    public UserProfile() {
        this.createdAt = System.currentTimeMillis();
    }

    public UserProfile(String userId, String displayName, String publicKey) {
        this();
        this.userId = userId;
        this.displayName = displayName;
        this.publicKey = publicKey;
    }

    // Getter 和 Setter 方法
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

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserProfile{" +
                "userId='" + userId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return userId != null && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }

    // Parcelable implementation
    protected UserProfile(Parcel in) {
        userId = in.readString();
        displayName = in.readString();
        publicKey = in.readString();
        createdAt = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(displayName);
        dest.writeString(publicKey);
        dest.writeLong(createdAt);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<UserProfile> CREATOR = new Creator<UserProfile>() {
        @Override
        public UserProfile createFromParcel(Parcel in) {
            return new UserProfile(in);
        }

        @Override
        public UserProfile[] newArray(int size) {
            return new UserProfile[size];
        }
    };
}
