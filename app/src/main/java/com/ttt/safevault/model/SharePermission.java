package com.ttt.safevault.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 分享权限数据模型
 */
public class SharePermission implements Parcelable {
    private boolean canView;         // 是否可查看
    private boolean canSave;         // 是否可保存到本地
    private boolean isRevocable;     // 是否可撤销

    public SharePermission() {
        this.canView = true;         // 默认可查看
        this.canSave = true;         // 默认可保存
        this.isRevocable = true;     // 默认可撤销
    }

    public SharePermission(boolean canView, boolean canSave, boolean isRevocable) {
        this.canView = canView;
        this.canSave = canSave;
        this.isRevocable = isRevocable;
    }

    // Getter 和 Setter 方法
    public boolean isCanView() {
        return canView;
    }

    public void setCanView(boolean canView) {
        this.canView = canView;
    }

    public boolean isCanSave() {
        return canSave;
    }

    public void setCanSave(boolean canSave) {
        this.canSave = canSave;
    }

    public boolean isRevocable() {
        return isRevocable;
    }

    public void setRevocable(boolean revocable) {
        isRevocable = revocable;
    }

    @Override
    public String toString() {
        return "SharePermission{" +
                "canView=" + canView +
                ", canSave=" + canSave +
                ", isRevocable=" + isRevocable +
                '}';
    }

    // Parcelable implementation
    protected SharePermission(Parcel in) {
        canView = in.readByte() != 0;
        canSave = in.readByte() != 0;
        isRevocable = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (canView ? 1 : 0));
        dest.writeByte((byte) (canSave ? 1 : 0));
        dest.writeByte((byte) (isRevocable ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<SharePermission> CREATOR = new Creator<SharePermission>() {
        @Override
        public SharePermission createFromParcel(Parcel in) {
            return new SharePermission(in);
        }

        @Override
        public SharePermission[] newArray(int size) {
            return new SharePermission[size];
        }
    };
}
