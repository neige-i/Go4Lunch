package com.neige_i.go4lunch.data.firebase;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import java.util.List;

public interface FirebaseRepository {

    @NonNull
    LiveData<String> getSelectedRestaurant();

    void setSelectedRestaurant(@NonNull String placeId);

    void clearSelectedRestaurant();

    @NonNull
    LiveData<List<String>> getFavoriteRestaurants();

    void toggleFavoriteRestaurant(@NonNull String placeId);
}
