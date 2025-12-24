package com.example.weatherforecast.ui.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherforecast.data.model.CurrentWeather;
import com.example.weatherforecast.data.model.ForecastData;
import com.example.weatherforecast.data.repository.WeatherRepository;
import com.example.weatherforecast.domain.usecase.GetCurrentWeatherUseCase;
import com.example.weatherforecast.domain.usecase.GetForecastUseCase;
import com.example.weatherforecast.error.ErrorHandler;
import com.example.weatherforecast.error.ErrorState;

public class WeatherViewModel extends AndroidViewModel {
    private static final String TAG = "WeatherViewModel";
    
    private final GetCurrentWeatherUseCase getCurrentUseCase;
    private final GetForecastUseCase getForecastUseCase;
    private final ErrorHandler errorHandler;
    
    private final MutableLiveData<CurrentWeather> currentWeather = new MutableLiveData<>();
    private final MutableLiveData<ForecastData> forecastData = new MutableLiveData<>();
    private final MutableLiveData<ErrorState> errorState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> simpleError = new MutableLiveData<>();
    
    private double lastLatitude = 0;
    private double lastLongitude = 0;
    
    public WeatherViewModel(@NonNull Application application) {
        super(application);
        
        WeatherRepository repo = WeatherRepository.getInstance(application);
        this.getCurrentUseCase = new GetCurrentWeatherUseCase(repo);
        this.getForecastUseCase = new GetForecastUseCase(repo);
        this.errorHandler = new ErrorHandler(application);
        
        // Инициализируем наблюдателей для Use Cases
        setupUseCaseObservers();
    }
    
    private void setupUseCaseObservers() {
        // Наблюдаем за данными из GetCurrentWeatherUseCase
        getCurrentUseCase.getCurrentWeatherData().observeForever(weather -> {
            if (weather != null) {
                currentWeather.setValue(weather);
                isLoading.setValue(false);
                // Очищаем ошибки при успешном получении данных
                errorState.setValue(null);
                simpleError.setValue(null);
            }
        });
        
        // Наблюдаем за ошибками из GetCurrentWeatherUseCase
        getCurrentUseCase.getError().observeForever(error -> {
            if (error != null) {
                handleUseCaseError(error, false);
            }
        });
        
        // Наблюдаем за данными из GetForecastUseCase
        getForecastUseCase.getForecastData().observeForever(forecast -> {
            if (forecast != null) {
                forecastData.setValue(forecast);
                // Очищаем ошибки при успешном получении данных
                errorState.setValue(null);
                simpleError.setValue(null);
            }
        });
        
        // Наблюдаем за ошибками из GetForecastUseCase
        getForecastUseCase.getError().observeForever(error -> {
            if (error != null) {
                handleUseCaseError(error, false);
            }
        });
    }
    
    public void loadCurrentWeather(double lat, double lon) {
        // Сохраняем последние координаты для возможности повтора
        lastLatitude = lat;
        lastLongitude = lon;
        
        // Проверяем доступность сети перед запросом
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.NETWORK_UNAVAILABLE,
                    true
            );
            errorState.setValue(networkError);
            isLoading.setValue(false);
            return;
        }
        
        isLoading.setValue(true);
        
        // Выполняем запрос через UseCase
        getCurrentUseCase.execute(lat, lon);
    }
    
    public void loadForecast(double lat, double lon) {
        // Сохраняем последние координаты для возможности повтора
        lastLatitude = lat;
        lastLongitude = lon;
        
        // Проверяем доступность сети перед запросом
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.NETWORK_UNAVAILABLE,
                    false // Прогноз не критичен
            );
            errorState.setValue(networkError);
            return;
        }
        
        // Выполняем запрос через UseCase
        getForecastUseCase.execute(lat, lon);
    }
    
    public void loadWeatherForLastLocation() {
        if (lastLatitude != 0 && lastLongitude != 0) {
            loadCurrentWeather(lastLatitude, lastLongitude);
            loadForecast(lastLatitude, lastLongitude);
        }
    }
    
    public void retryLastRequest() {
        if (lastLatitude != 0 && lastLongitude != 0) {
            loadCurrentWeather(lastLatitude, lastLongitude);
            loadForecast(lastLatitude, lastLongitude);
        } else {
            // Если нет сохраненных координат, используем дефолтные
            loadCurrentWeather(55.7558, 37.6173); // Москва
            loadForecast(55.7558, 37.6173);
        }
    }
    
    private void handleUseCaseError(String errorMessage, boolean isCritical) {
        // Преобразуем строковую ошибку в ErrorState
        ErrorState errorStateObj;
        
        if (errorMessage.contains("network") || errorMessage.contains("connection")) {
            errorStateObj = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.NETWORK_UNAVAILABLE,
                    isCritical
            );
        } else if (errorMessage.contains("server") || errorMessage.contains("unavailable")) {
            errorStateObj = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.SERVER_UNAVAILABLE,
                    isCritical
            );
        } else if (errorMessage.contains("timeout")) {
            errorStateObj = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.NETWORK_TIMEOUT,
                    isCritical
            );
        } else if (errorMessage.contains("location") || errorMessage.contains("GPS")) {
            errorStateObj = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.LOCATION_UNAVAILABLE,
                    isCritical
            );
        } else if (errorMessage.contains("invalid") || errorMessage.contains("parse")) {
            errorStateObj = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.INVALID_DATA,
                    isCritical
            );
        } else {
            errorStateObj = errorHandler.handleError(
                    com.example.weatherforecast.error.ErrorType.UNKNOWN_ERROR,
                    errorMessage,
                    isCritical
            );
        }
        
        errorState.setValue(errorStateObj);
        simpleError.setValue(errorMessage);
        isLoading.setValue(false);
        
        Log.e(TAG, "Weather request failed: " + errorMessage);
    }
    
    // Методы для обработки ошибок извне
    public void handleExternalError(ErrorState errorState) {
        this.errorState.setValue(errorState);
        isLoading.setValue(false);
    }
    
    public void clearError() {
        errorState.setValue(null);
        simpleError.setValue(null);
    }
    
    // Getters
    public LiveData<CurrentWeather> getCurrentWeather() {
        return currentWeather;
    }
    
    public LiveData<ForecastData> getForecastData() {
        return forecastData;
    }
    
    public LiveData<ErrorState> getErrorState() {
        return errorState;
    }
    
    public LiveData<String> getError() {
        return simpleError;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public double getLastLatitude() {
        return lastLatitude;
    }
    
    public double getLastLongitude() {
        return lastLongitude;
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Очищаем наблюдателей чтобы избежать утечек памяти
        getCurrentUseCase.getCurrentWeatherData().removeObservers(null);
        getCurrentUseCase.getError().removeObservers(null);
        getForecastUseCase.getForecastData().removeObservers(null);
        getForecastUseCase.getError().removeObservers(null);
    }
}