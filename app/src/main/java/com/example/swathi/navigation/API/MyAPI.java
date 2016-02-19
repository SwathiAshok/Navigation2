package com.example.swathi.navigation.API;

import com.example.swathi.navigation.utils.model.APS;
import com.example.swathi.navigation.utils.model.CAL;

import retrofit.Call;
import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface MyAPI {

        @GET("AP/{BSSID}")
        Call<APS> getxyByBSSID(@Path("BSSID") String BSSID);

        @POST("CAL/")
        Call<CAL> getres(@Body String string);
}
