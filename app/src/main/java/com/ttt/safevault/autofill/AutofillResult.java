package com.ttt.safevault.autofill;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.autofill.AutofillId;

/**
 * 自动填充结果
 * 包含填充字段信息和选择的凭据
 */
public class AutofillResult implements Parcelable {

    private AutofillId usernameId;
    private AutofillId passwordId;
    private AutofillId urlId;
    private String packageName;
    private String webDomain;

    public AutofillResult() {
    }

    public AutofillResult(AutofillId usernameId, AutofillId passwordId,
                          AutofillId urlId, String packageName, String webDomain) {
        this.usernameId = usernameId;
        this.passwordId = passwordId;
        this.urlId = urlId;
        this.packageName = packageName;
        this.webDomain = webDomain;
    }

    protected AutofillResult(Parcel in) {
        usernameId = in.readParcelable(AutofillId.class.getClassLoader());
        passwordId = in.readParcelable(AutofillId.class.getClassLoader());
        urlId = in.readParcelable(AutofillId.class.getClassLoader());
        packageName = in.readString();
        webDomain = in.readString();
    }

    public static final Creator<AutofillResult> CREATOR = new Creator<AutofillResult>() {
        @Override
        public AutofillResult createFromParcel(Parcel in) {
            return new AutofillResult(in);
        }

        @Override
        public AutofillResult[] newArray(int size) {
            return new AutofillResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(usernameId, flags);
        dest.writeParcelable(passwordId, flags);
        dest.writeParcelable(urlId, flags);
        dest.writeString(packageName);
        dest.writeString(webDomain);
    }

    // Getters and Setters
    public AutofillId getUsernameId() {
        return usernameId;
    }

    public void setUsernameId(AutofillId usernameId) {
        this.usernameId = usernameId;
    }

    public AutofillId getPasswordId() {
        return passwordId;
    }

    public void setPasswordId(AutofillId passwordId) {
        this.passwordId = passwordId;
    }

    public AutofillId getUrlId() {
        return urlId;
    }

    public void setUrlId(AutofillId urlId) {
        this.urlId = urlId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getWebDomain() {
        return webDomain;
    }

    public void setWebDomain(String webDomain) {
        this.webDomain = webDomain;
    }

    public boolean hasUsername() {
        return usernameId != null;
    }

    public boolean hasPassword() {
        return passwordId != null;
    }

    public boolean hasUrl() {
        return urlId != null;
    }

    public boolean isValid() {
        return hasUsername() && hasPassword();
    }
}