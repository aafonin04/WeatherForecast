package data.api;

import data.model.ApiResponse;
import data.model.CurrentWeather;
import data.model.ForecastData;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
public interface WeatherApi {
    @GET("weather/current")
    Call<ApiResponse<CurrentWeather>> getCurrentWeather(
            @Query("lat") double lat,
            @Query("lon") double lon
    );

    @GET("weather/forecast")
    Call<ApiResponse<ForecastData>> getForecast(
            @Query("lat") double lat,
            @Query("lon") double lon
    );
}
