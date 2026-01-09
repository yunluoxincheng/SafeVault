package com.ttt.safevault.network.api;

import com.ttt.safevault.dto.request.RegisterLocationRequest;
import com.ttt.safevault.dto.response.NearbyUserResponse;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * 附近发现服务API接口
 */
public interface DiscoveryServiceApi {
    
    @POST("v1/discovery/register")
    Observable<Void> registerLocation(@Body RegisterLocationRequest request);
    
    @GET("v1/discovery/nearby")
    Observable<List<NearbyUserResponse>> getNearbyUsers(
        @Query("lat") double lat,
        @Query("lng") double lng,
        @Query("radius") double radius
    );
    
    @POST("v1/discovery/heartbeat")
    Observable<Void> sendHeartbeat();
}
