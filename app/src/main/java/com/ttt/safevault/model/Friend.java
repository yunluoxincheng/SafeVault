package com.ttt.safevault.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 好友数据模型
 * 用于表示SafeVault应用内的好友关系
 */
public class Friend implements Parcelable {
    private String friendId;         // 好友用户ID
    private String displayName;      // 好友显示名称
    private String publicKey;        // 好友公钥
    private long addedAt;            // 添加时间
    private boolean isBlocked;       // 是否已屏蔽

    public Friend() {
        this.addedAt = System.currentTimeMillis();
        this.isBlocked = false;
    }

    public Friend(String friendId, String displayName, String publicKey) {
        this();
        this.friendId = friendId;
        this.displayName = displayName;
        this.publicKey = publicKey;
    }

    // Getter 和 Setter 方法
    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
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

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }

    @Override
    public String toString() {
        return "Friend{" +
                "friendId='" + friendId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", addedAt=" + addedAt +
                ", isBlocked=" + isBlocked +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Friend friend = (Friend) o;
        return friendId != null && friendId.equals(friend.friendId);
    }

    @Override
    public int hashCode() {
        return friendId != null ? friendId.hashCode() : 0;
    }

    // Parcelable implementation
    protected Friend(Parcel in) {
        friendId = in.readString();
        displayName = in.readString();
        publicKey = in.readString();
        addedAt = in.readLong();
        isBlocked = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(friendId);
        dest.writeString(displayName);
        dest.writeString(publicKey);
        dest.writeLong(addedAt);
        dest.writeByte((byte) (isBlocked ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Friend> CREATOR = new Creator<Friend>() {
        @Override
        public Friend createFromParcel(Parcel in) {
            return new Friend(in);
        }

        @Override
        public Friend[] newArray(int size) {
            return new Friend[size];
        }
    };
}
