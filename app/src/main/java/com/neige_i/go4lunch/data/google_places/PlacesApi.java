package com.neige_i.go4lunch.data.google_places;

import com.neige_i.go4lunch.data.google_places.model.NearbyResponse;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public interface PlacesApi {

    @GET("json?location=48.856614,2.3522219&radius=2000&type=restaurant&key=AIzaSyDdc24VwRd5iGQjd46ygpOIcVGhiDnD4gs")
    Call<NearbyResponse> getNearbyRestaurants();

    static PlacesApi getInstance() {
        return new Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/api/place/nearbysearch/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PlacesApi.class);
    }
}
