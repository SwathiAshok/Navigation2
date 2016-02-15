package com.example.swathi.navigation.API;

import com.example.swathi.navigation.utils.model.APS;
import retrofit.Callback;
import retrofit.http.GET;
import retrofit.http.Path;

public interface MyAPI {

        @GET("/users/<id>")
        public void getxyByBSSID(@Path("id") String id, Callback<APS> cb);
}
