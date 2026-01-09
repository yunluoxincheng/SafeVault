package com.ttt.safevault.network.api;

import com.ttt.safevault.dto.request.CreateShareRequest;
import com.ttt.safevault.dto.response.ReceivedShareResponse;
import com.ttt.safevault.dto.response.ShareResponse;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * 分享服务API接口
 */
public interface ShareServiceApi {
    
    @POST("v1/shares")
    Observable<ShareResponse> createShare(@Body CreateShareRequest request);
    
    @GET("v1/shares/{shareId}")
    Observable<ReceivedShareResponse> receiveShare(@Path("shareId") String shareId);
    
    @POST("v1/shares/{shareId}/revoke")
    Observable<Void> revokeShare(@Path("shareId") String shareId);
    
    @POST("v1/shares/{shareId}/save")
    Observable<Void> saveSharedPassword(@Path("shareId") String shareId);
    
    @GET("v1/shares/created")
    Observable<List<ReceivedShareResponse>> getMyShares();
    
    @GET("v1/shares/received")
    Observable<List<ReceivedShareResponse>> getReceivedShares();
}
