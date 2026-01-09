package com.ttt.safevault.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.ttt.safevault.dto.response.NearbyUserResponse;
import com.ttt.safevault.model.BackendService;
import com.ttt.safevault.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * 附近用户发现ViewModel
 */
public class NearbyUsersViewModel extends AndroidViewModel {
    private static final String TAG = "NearbyUsersViewModel";
    
    private final BackendService backendService;
    private final RetrofitClient retrofitClient;
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    private final MutableLiveData<List<NearbyUserResponse>> nearbyUsersLiveData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> locationRegisteredLiveData = new MutableLiveData<>(false);
    
    public NearbyUsersViewModel(@NonNull Application application) {
        super(application);
        this.backendService = com.ttt.safevault.ServiceLocator.getInstance().getBackendService();
        this.retrofitClient = RetrofitClient.getInstance(application);
    }
    
    /**
     * 注册位置
     */
    public void registerLocation(double latitude, double longitude, double radius) {
        Disposable disposable = Observable.fromCallable(() -> {
            backendService.registerLocation(latitude, longitude, radius);
            return true;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            success -> {
                locationRegisteredLiveData.setValue(true);
                Log.d(TAG, "Location registered");
            },
            error -> {
                errorLiveData.setValue("位置注册失败: " + error.getMessage());
                Log.e(TAG, "Failed to register location", error);
            }
        );
        
        disposables.add(disposable);
    }
    
    /**
     * 获取附近用户
     */
    public void getNearbyUsers(double latitude, double longitude, double radius) {
        loadingLiveData.setValue(true);
        
        Disposable disposable = Observable.fromCallable(() -> 
            backendService.getNearbyUsers(latitude, longitude, radius)
        )
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            users -> {
                loadingLiveData.setValue(false);
                nearbyUsersLiveData.setValue(users);
                Log.d(TAG, "Found " + users.size() + " nearby users");
            },
            error -> {
                loadingLiveData.setValue(false);
                errorLiveData.setValue("获取附近用户失败: " + error.getMessage());
                Log.e(TAG, "Failed to get nearby users", error);
            }
        );
        
        disposables.add(disposable);
    }
    
    /**
     * 发送心跳
     */
    public void sendHeartbeat() {
        Disposable disposable = Observable.fromCallable(() -> {
            backendService.sendHeartbeat();
            return true;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            success -> Log.d(TAG, "Heartbeat sent"),
            error -> Log.e(TAG, "Failed to send heartbeat", error)
        );
        
        disposables.add(disposable);
    }
    
    // Getters
    public LiveData<List<NearbyUserResponse>> getNearbyUsers() {
        return nearbyUsersLiveData;
    }
    
    public LiveData<Boolean> getLoading() {
        return loadingLiveData;
    }
    
    public LiveData<String> getError() {
        return errorLiveData;
    }
    
    public LiveData<Boolean> getLocationRegistered() {
        return locationRegisteredLiveData;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
