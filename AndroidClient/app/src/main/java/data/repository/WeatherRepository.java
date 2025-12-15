package data.repository;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.LiveData;

import java.util.concurrent.TimeUnit;

import data.api.WeatherApi;
import data.model.CurrentWeather;
import data.model.ForecastData;
import data.model.ApiResponse;

import okhttp3.OkHttpClient;

import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class WeatherRepository {
    private WeatherApi weatherApi;
    private MutableLiveData<CurrentWeather> currentWeatherData = new MutableLiveData<>();
    private MutableLiveData<ForecastData> forecastData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public WeatherRepository() {
        // Логирование запросов (для debug)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttp с таймаутами
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        // Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")  // Для эмулятора;
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();

        weatherApi = retrofit.create(WeatherApi.class);
    }

    public LiveData<CurrentWeather> getCurrentWeather(double lat, double lon) {
        weatherApi.getCurrentWeather(lat, lon).enqueue(new Callback<ApiResponse<CurrentWeather>>() {
            @Override
            public void onResponse(Call<ApiResponse<CurrentWeather>> call, Response<ApiResponse<CurrentWeather>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<CurrentWeather> apiResp = response.body();
                    if (apiResp.isSuccess()) {
                        currentWeatherData.setValue(apiResp.getData());
                    } else {
                        errorMessage.setValue(apiResp.getMessage());
                    }
                } else {
                    errorMessage.setValue("HTTP ошибка: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<CurrentWeather>> call, Throwable t) {
                errorMessage.setValue("Сетевая ошибка: " + t.getMessage());
            }
        });
        return currentWeatherData;
    }

    public LiveData<ForecastData> getForecast(double lat, double lon) {
        weatherApi.getForecast(lat, lon).enqueue(new Callback<ApiResponse<ForecastData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ForecastData>> call, Response<ApiResponse<ForecastData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ForecastData> apiResp = response.body();
                    if (apiResp.isSuccess()) {
                        forecastData.setValue(apiResp.getData());
                    } else {
                        errorMessage.setValue(apiResp.getMessage());
                    }
                } else {
                    errorMessage.setValue("HTTP ошибка: " + response.code() + " - " + response.message());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ForecastData>> call, Throwable t) {
                errorMessage.setValue("Сетевая ошибка: " + t.getMessage());
            }
        });
        return forecastData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
}
