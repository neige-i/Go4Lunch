package com.neige_i.go4lunch.view.list;

import android.graphics.Typeface;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.neige_i.go4lunch.R;
import com.neige_i.go4lunch.data.google_places.model.DetailsResponse;
import com.neige_i.go4lunch.domain.GetRestaurantDetailsListUseCase;
import com.neige_i.go4lunch.view.util.Util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListViewModel extends ViewModel {

    @NonNull
    private static final DateTimeFormatter hourFormat = DateTimeFormatter.ofPattern("HHmm");

    @NonNull
    private final GetRestaurantDetailsListUseCase getRestaurantDetailsListUseCase;

    public ListViewModel(@NonNull GetRestaurantDetailsListUseCase getRestaurantDetailsListUseCase) {
        this.getRestaurantDetailsListUseCase = getRestaurantDetailsListUseCase;
    }

    public LiveData<List<RestaurantViewState>> getViewState() {
        // 1. FETCH the list model
        return Transformations.map(getRestaurantDetailsListUseCase.getDetailsList(), listModel -> {
            // The view state to return
            final List<RestaurantViewState> viewStates = new ArrayList<>();

            if (listModel != null) {

                // 2. ITERATE through the list of details responses
                for (DetailsResponse response : listModel.getDetailsResponses()) {

                    final DetailsResponse.Result result = response.getResult();
                    if (result != null && result.getBusinessStatus() != null && result.getBusinessStatus().equals("OPERATIONAL")) {

                        // 3. GET the distance between the restaurant and the current location
                        final DetailsResponse.Location restaurantLocation = result.getGeometry().getLocation();
                        final float[] distances = new float[3];
                        Location.distanceBetween(
                            listModel.getCurrentLocation().getLatitude(),
                            listModel.getCurrentLocation().getLongitude(),
                            restaurantLocation.getLat(),
                            restaurantLocation.getLng(),
                            distances
                        );

                        // GET opening hours
                        final PlaceHourWrapper placeHourWrapper = getPlaceHour(result.getOpeningHours());

                        // MAPPING
                        viewStates.add(new RestaurantViewState(
                            result.getPlaceId(),
                            result.getName(),
                            distances[0],
                            Util.getFormattedDistance(distances[0]),
                            Util.getShortAddress(result.getFormattedAddress()),
                            placeHourWrapper.getFontStyle(),//Typeface.BOLD_ITALIC,
                            placeHourWrapper.getFontColor(),//R.color.lime,
                            placeHourWrapper.getHours(),
                            true,
                            2,
                            Util.getRating(result.getRating()),
                            Util.getPhotoUrl(result.getPhotos())
                        ));

                        // SORT the restaurant list by distance
                        Collections.sort(
                            viewStates,
                            (viewState1, viewState2) -> (int) (viewState1.getDistance() - viewState2.getDistance())
                        );
                    }
                }
            }

            return viewStates;
        });
    }

    private PlaceHourWrapper getPlaceHour(@Nullable DetailsResponse.OpeningHours openingHours) {
        final PlaceHourWrapper unknownHours = new PlaceHourWrapper("Unknown hours", R.color.gray_dark, Typeface.NORMAL);

        if (openingHours == null)
            return unknownHours;

        final List<DetailsResponse.Period> periodList = openingHours.getPeriods();
        if (periodList == null)
            return unknownHours;

        // 1. Check if the place is never closed
        if (periodList.stream().allMatch(period -> period.getClose() == null))
            return new PlaceHourWrapper("Open 24/7", R.color.lime_dark, Typeface.ITALIC);

        // Day values: from 1 (Monday) to 7 (Sunday) for java.time and from 0 (Sunday) to 6 (Saturday) for Places API
        final DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        final int currentDay = dayOfWeek == DayOfWeek.SUNDAY ? 0 : dayOfWeek.getValue();

        // 2. Find the first opened period for today
        DetailsResponse.Period firstTodayOpenPeriod = null;
        for (DetailsResponse.Period period : periodList) {
            if (period.getOpen() != null && period.getOpen().getDay() != null && period.getOpen().getDay() == currentDay) {
                firstTodayOpenPeriod = period;
                break;
            }
        }

        // 3. Check if the place is opened today
        if (firstTodayOpenPeriod == null)
            return new PlaceHourWrapper("Closed today", android.R.color.holo_red_dark, Typeface.BOLD);

        PlaceHourWrapper placeHourWrapper = null;
        for (int i = periodList.indexOf(firstTodayOpenPeriod); i < periodList.size(); i++) {

            final DetailsResponse.Open todayOpen = periodList.get(i).getOpen();
            if (todayOpen == null) {
                break; // Should never happen because at this state we know the place is opened today
            } else if (todayOpen.getDay() != null && todayOpen.getDay() != currentDay) {
                break; // Break the loop if iterate over a period with an opened day different from today
            }

            final LocalTime currentTime = LocalTime.now();

            // 4. Check if the place isn't opened yet
            final LocalTime openTime = LocalTime.parse(todayOpen.getTime(), hourFormat);
            if (currentTime.isBefore(openTime)) {
                // TODO: handle when next open is another day
                placeHourWrapper = new PlaceHourWrapper("Closed until " + openTime, android.R.color.holo_red_dark, Typeface.BOLD);
                break;
            }

            if (firstTodayOpenPeriod.getClose() == null) {
                break;
            }

            final LocalTime closeTime = LocalTime.parse(firstTodayOpenPeriod.getClose().getTime(), hourFormat);

            final int closedNextDay;
            if (firstTodayOpenPeriod.getClose().getDay() != null && firstTodayOpenPeriod.getClose().getDay() != currentDay)
                closedNextDay = 24; // 24 hours a day
            else
                closedNextDay = 0;

            if (currentTime.isBefore(closeTime) || closedNextDay != 0) {
                if (Duration.between(currentTime, closeTime).toHours() + closedNextDay < 1) {
                    placeHourWrapper = new PlaceHourWrapper("Closing soon", android.R.color.holo_red_dark, Typeface.BOLD);
                } else {
                    placeHourWrapper = new PlaceHourWrapper("Open until " + closeTime, R.color.lime_dark, Typeface.ITALIC);
                }
                break;
            }
        }

        return placeHourWrapper == null ? unknownHours : placeHourWrapper;
    }
}
