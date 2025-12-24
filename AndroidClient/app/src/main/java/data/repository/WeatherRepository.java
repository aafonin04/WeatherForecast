package com.example.weatherforecast.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherforecast.data.api.WeatherApi;
import com.example.weatherforecast.data.model.ApiResponse;
import com.example.weatherforecast.data.model.CurrentWeather;
import com.example.weatherforecast.data.model.ForecastData;
import com.example.weatherforecast.error.ErrorHandler;
import com.example.weatherforecast.error.ErrorState;
import com.example.weatherforecast.error.ErrorType;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherRepository {
    private static final String TAG = "WeatherRepository";
    private static WeatherRepository instance;
    
    private final WeatherApi weatherApi;
    private final ErrorHandler errorHandler;
    
    private final MutableLiveData<CurrentWeather> currentWeatherData = new MutableLiveData<>();
    private final MutableLiveData<ForecastData> forecastData = new MutableLiveData<>();
    private final MutableLiveData<ErrorState> errorState = new MutableLiveData<>();
    private final MutableLiveData<String> simpleErrorMessage = new MutableLiveData<>();
    
    private final Map<String, CurrentWeather> currentCache = new HashMap<>();
    private final Map<String, ForecastData> forecastCache = new HashMap<>();
    
    // Интерфейсы для коллбэков
    public interface WeatherCallback<T> {
        void onSuccess(T data);
        void onError(ErrorState errorState);
    }
    
    public interface SimpleWeatherCallback<T> {
        void onSuccess(T data);
        void onError(String errorMessage);
    }
    
    private WeatherRepository(android.content.Context context) {
        this.errorHandler = new ErrorHandler(context);
        
        // Логирование запросов (для debug)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // OkHttp с улучшенными таймаутами и перехватчиками
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    // Добавляем заголовки или логику повторных попыток
                    try {
                        return chain.proceed(chain.request());
                    } catch (SocketTimeoutException e) {
                        android.util.Log.e(TAG, "Socket timeout", e);
                        throw e;
                    } catch (UnknownHostException e) {
                        android.util.Log.e(TAG, "No internet connection", e);
                        throw e;
                    }
                })
                .build();
        
        // Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:5000/")  // Для эмулятора
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
        
        weatherApi = retrofit.create(WeatherApi.class);
    }
    
    // Singleton pattern
    public static synchronized WeatherRepository getInstance(android.content.Context context) {
        if (instance == null) {
            instance = new WeatherRepository(context.getApplicationContext());
        }
        return instance;
    }
    
    // Статический метод для совместимости со старым кодом
    public static WeatherRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("WeatherRepository must be initialized with context first");
        }
        return instance;
    }
    
    // Метод для инициализации (должен быть вызван в Application или MainActivity)
    public static void initialize(android.content.Context context) {
        if (instance == null) {
            instance = new WeatherRepository(context.getApplicationContext());
        }
    }
    
    /**
     * Получение текущей погоды с использованием LiveData (старый интерфейс)
     */
    public LiveData<CurrentWeather> getCurrentWeather(double lat, double lon) {
        // Проверяем кэш
        String cacheKey = lat + "," + lon;
        CurrentWeather cached = currentCache.get(cacheKey);
        if (cached != null) {
            currentWeatherData.setValue(cached);
            return currentWeatherData;
        }
        
        // Проверяем доступность сети
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    ErrorType.NETWORK_UNAVAILABLE, 
                    "Please check your internet connection",
                    true
            );
            errorState.setValue(networkError);
            simpleErrorMessage.setValue(networkError.getMessage());
            return currentWeatherData;
        }
        
        weatherApi.getCurrentWeather(lat, lon).enqueue(new Callback<ApiResponse<CurrentWeather>>() {
            @Override
            public void onResponse(Call<ApiResponse<CurrentWeather>> call, 
                                  Response<ApiResponse<CurrentWeather>> response) {
                
                android.util.Log.d(TAG, "Запрос current: " + call.request().url());
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<CurrentWeather> apiResp = response.body();
                    if (apiResp.isSuccess()) {
                        CurrentWeather weather = apiResp.getData();
                        // Кэшируем результат
                        currentCache.put(cacheKey, weather);
                        currentWeatherData.setValue(weather);
                        // Очищаем ошибки
                        errorState.setValue(null);
                        simpleErrorMessage.setValue(null);
                    } else {
                        handleApiError(apiResp.getMessage(), "current_weather", false);
                    }
                } else {
                    handleHttpError(response.code(), response.message(), "current_weather");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<CurrentWeather>> call, Throwable t) {
                android.util.Log.e(TAG, "Сетевая ошибка: " + t.getMessage(), t);
                handleNetworkError(t, "current_weather");
            }
        });
        
        return currentWeatherData;
    }
    
    /**
     * Получение прогноза с использованием LiveData (старый интерфейс)
     */
    public LiveData<ForecastData> getForecast(double lat, double lon) {
        // Проверяем кэш
        String cacheKey = lat + "," + lon;
        ForecastData cached = forecastCache.get(cacheKey);
        if (cached != null) {
            forecastData.setValue(cached);
            return forecastData;
        }
        
        // Проверяем доступность сети
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    ErrorType.NETWORK_UNAVAILABLE,
                    "Please check your internet connection",
                    false // Прогноз не критичен
            );
            errorState.setValue(networkError);
            simpleErrorMessage.setValue(networkError.getMessage());
            return forecastData;
        }
        
        weatherApi.getForecast(lat, lon).enqueue(new Callback<ApiResponse<ForecastData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ForecastData>> call, 
                                  Response<ApiResponse<ForecastData>> response) {
                
                android.util.Log.d(TAG, "Запрос forecast: " + call.request().url());
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ForecastData> apiResp = response.body();
                    if (apiResp.isSuccess()) {
                        ForecastData forecast = apiResp.getData();
                        // Кэшируем результат
                        forecastCache.put(cacheKey, forecast);
                        forecastData.setValue(forecast);
                        // Очищаем ошибки
                        errorState.setValue(null);
                        simpleErrorMessage.setValue(null);
                    } else {
                        handleApiError(apiResp.getMessage(), "forecast", false);
                    }
                } else {
                    handleHttpError(response.code(), response.message(), "forecast");
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<ForecastData>> call, Throwable t) {
                android.util.Log.e(TAG, "Сетевая ошибка: " + t.getMessage(), t);
                handleNetworkError(t, "forecast");
            }
        });
        
        return forecastData;
    }
    
    /**
     * НОВЫЙ: Получение текущей погоды через коллбэк с обработкой ошибок
     */
    public void getCurrentWeather(double lat, double lon, WeatherCallback<CurrentWeather> callback) {
        // Проверяем кэш
        String cacheKey = lat + "," + lon;
        CurrentWeather cached = currentCache.get(cacheKey);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }
        
        // Проверяем доступность сети
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    ErrorType.NETWORK_UNAVAILABLE,
                    "Please check your internet connection",
                    true
            );
            callback.onError(networkError);
            return;
        }
        
        weatherApi.getCurrentWeather(lat, lon).enqueue(new Callback<ApiResponse<CurrentWeather>>() {
            @Override
            public void onResponse(Call<ApiResponse<CurrentWeather>> call, 
                                  Response<ApiResponse<CurrentWeather>> response) {
                
                android.util.Log.d(TAG, "Запрос current: " + call.request().url());
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<CurrentWeather> apiResp = response.body();
                    if (apiResp.isSuccess()) {
                        CurrentWeather weather = apiResp.getData();
                        // Кэшируем результат
                        currentCache.put(cacheKey, weather);
                        callback.onSuccess(weather);
                    } else {
                        ErrorState error = handleApiErrorForCallback(
                                apiResp.getMessage(), "current_weather", false);
                        callback.onError(error);
                    }
                } else {
                    ErrorState error = handleHttpErrorForCallback(
                            response.code(), response.message(), "current_weather");
                    callback.onError(error);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<CurrentWeather>> call, Throwable t) {
                android.util.Log.e(TAG, "Сетевая ошибка: " + t.getMessage(), t);
                ErrorState error = handleNetworkErrorForCallback(t, "current_weather");
                callback.onError(error);
            }
        });
    }
    
    /**
     * НОВЫЙ: Получение прогноза через коллбэк с обработкой ошибок
     */
    public void getForecast(double lat, double lon, WeatherCallback<ForecastData> callback) {
        // Проверяем кэш
        String cacheKey = lat + "," + lon;
        ForecastData cached = forecastCache.get(cacheKey);
        if (cached != null) {
            callback.onSuccess(cached);
            return;
        }
        
        // Проверяем доступность сети
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    ErrorType.NETWORK_UNAVAILABLE,
                    "Please check your internet connection",
                    false
            );
            callback.onError(networkError);
            return;
        }
        
        weatherApi.getForecast(lat, lon).enqueue(new Callback<ApiResponse<ForecastData>>() {
            @Override
            public void onResponse(Call<ApiResponse<ForecastData>> call, 
                                  Response<ApiResponse<ForecastData>> response) {
                
                android.util.Log.d(TAG, "Запрос forecast: " + call.request().url());
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<ForecastData> apiResp = response.body();
                    if (apiResp.isSuccess()) {
                        ForecastData forecast = apiResp.getData();
                        // Кэшируем результат
                        forecastCache.put(cacheKey, forecast);
                        callback.onSuccess(forecast);
                    } else {
                        ErrorState error = handleApiErrorForCallback(
                                apiResp.getMessage(), "forecast", false);
                        callback.onError(error);
                    }
                } else {
                    ErrorState error = handleHttpErrorForCallback(
                            response.code(), response.message(), "forecast");
                    callback.onError(error);
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<ForecastData>> call, Throwable t) {
                android.util.Log.e(TAG, "Сетевая ошибка: " + t.getMessage(), t);
                ErrorState error = handleNetworkErrorForCallback(t, "forecast");
                callback.onError(error);
            }
        });
    }
    
    /**
     * НОВЫЙ: Совместимость со старым SimpleWeatherCallback
     */
    public void getCurrentWeather(double lat, double lon, SimpleWeatherCallback<CurrentWeather> callback) {
        getCurrentWeather(lat, lon, new WeatherCallback<CurrentWeather>() {
            @Override
            public void onSuccess(CurrentWeather data) {
                callback.onSuccess(data);
            }
            
            @Override
            public void onError(ErrorState errorState) {
                callback.onError(errorState.getMessage());
            }
        });
    }
    
    /**
     * НОВЫЙ: Совместимость со старым SimpleWeatherCallback
     */
    public void getForecast(double lat, double lon, SimpleWeatherCallback<ForecastData> callback) {
        getForecast(lat, lon, new WeatherCallback<ForecastData>() {
            @Override
            public void onSuccess(ForecastData data) {
                callback.onSuccess(data);
            }
            
            @Override
            public void onError(ErrorState errorState) {
                callback.onError(errorState.getMessage());
            }
        });
    }
    
    /**
     * Очистка кэша
     */
    public void clearCache() {
        currentCache.clear();
        forecastCache.clear();
        android.util.Log.d(TAG, "Cache cleared");
    }
    
    /**
     * Очистка кэша для конкретной локации
     */
    public void clearCache(double lat, double lon) {
        String cacheKey = lat + "," + lon;
        currentCache.remove(cacheKey);
        forecastCache.remove(cacheKey);
        android.util.Log.d(TAG, "Cache cleared for location: " + cacheKey);
    }
    
    // Методы обработки ошибок для LiveData
    private void handleApiError(String message, String endpoint, boolean isCritical) {
        ErrorState error = errorHandler.handleError(
                ErrorType.SERVER_ERROR,
                "API Error (" + endpoint + "): " + message,
                isCritical
        );
        errorState.setValue(error);
        simpleErrorMessage.setValue(error.getMessage());
    }
    
    private void handleHttpError(int code, String message, String endpoint) {
        ErrorType errorType = (code >= 500) ? ErrorType.SERVER_ERROR : ErrorType.INVALID_DATA;
        ErrorState error = errorHandler.handleError(
                errorType,
                "HTTP Error " + code + " (" + endpoint + "): " + message,
                code >= 500 // Серверные ошибки считаем критическими
        );
        errorState.setValue(error);
        simpleErrorMessage.setValue(error.getMessage());
    }
    
    private void handleNetworkError(Throwable t, String endpoint) {
        ErrorState error = errorHandler.handleError(t, false);
        errorState.setValue(error);
        simpleErrorMessage.setValue(error.getMessage());
    }
    
    // Методы обработки ошибок для коллбэков
    private ErrorState handleApiErrorForCallback(String message, String endpoint, boolean isCritical) {
        return errorHandler.handleError(
                ErrorType.SERVER_ERROR,
                "API Error (" + endpoint + "): " + message,
                isCritical
        );
    }
    
    private ErrorState handleHttpErrorForCallback(int code, String message, String endpoint) {
        ErrorType errorType = (code >= 500) ? ErrorType.SERVER_ERROR : ErrorType.INVALID_DATA;
        return errorHandler.handleError(
                errorType,
                "HTTP Error " + code + " (" + endpoint + "): " + message,
                code >= 500
        );
    }
    
    private ErrorState handleNetworkErrorForCallback(Throwable t, String endpoint) {
        return errorHandler.handleError(t, false);
    }
    
    // Getters для LiveData
    public LiveData<CurrentWeather> getCurrentWeatherData() {
        return currentWeatherData;
    }
    
    public LiveData<ForecastData> getForecastData() {
        return forecastData;
    }
    
    public LiveData<ErrorState> getErrorState() {
        return errorState;
    }
    
    public LiveData<String> getSimpleErrorMessage() {
        return simpleErrorMessage;
    }
}