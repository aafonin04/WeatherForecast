package com.example.weatherforecast.ui.viewmodel;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherforecast.data.location.LocationProvider;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationViewModel extends AndroidViewModel {
    private static final String TAG = "LocationViewModel";
    
    private final LocationProvider locationProvider;
    private final ExecutorService executorService;
    
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<String> locationError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<Boolean> hasPermission = new MutableLiveData<>();
    
    public LocationViewModel(@NonNull Application application) {
        super(application);
        this.locationProvider = new LocationProvider(application);
        this.executorService = Executors.newSingleThreadExecutor();
        this.isLoading.setValue(false);
        checkPermissions();
    }
    
    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }
    
    public LiveData<String> getLocationError() {
        return locationError;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<Boolean> getHasPermission() {
        return hasPermission;
    }
    
    /**
     * Проверяет наличие разрешений
     */
    public void checkPermissions() {
        boolean hasPerm = locationProvider.hasLocationPermission();
        hasPermission.setValue(hasPerm);
        
        if (hasPerm) {
            boolean locationEnabled = locationProvider.isLocationEnabled();
            if (!locationEnabled) {
                locationError.setValue("Please enable location services");
            }
        } else {
            locationError.setValue("Location permission required");
        }
    }
    
    /**
     * Получает текущую локацию
     */
    public void fetchCurrentLocation() {
        isLoading.setValue(true);
        locationError.setValue(null);
        
        executorService.execute(() -> {
            // Проверяем разрешения в фоновом потоке
            if (!locationProvider.hasLocationPermission()) {
                locationError.postValue("Location permission required");
                isLoading.postValue(false);
                return;
            }
            
            // Проверяем, включены ли службы локации
            if (!locationProvider.isLocationEnabled()) {
                locationError.postValue("Please enable GPS");
                isLoading.postValue(false);
                return;
            }
            
            // Получаем локацию с fallback
            locationProvider.getLocationWithFallback(new LocationProvider.LocationCallbackListener() {
                @Override
                public void onLocationResult(Location location) {
                    currentLocation.postValue(location);
                    locationError.postValue(null);
                    isLoading.postValue(false);
                    Log.d(TAG, "Location obtained: " + location.getLatitude() + ", " + location.getLongitude());
                }
                
                @Override
                public void onLocationError(String error) {
                    locationError.postValue(error);
                    isLoading.postValue(false);
                    Log.e(TAG, "Location error: " + error);
                }
            });
        });
    }
    
    /**
     * Начинает отслеживание изменений локации
     */
    public void startLocationTracking() {
        if (!locationProvider.hasLocationPermission()) {
            locationError.setValue("Location permission required");
            return;
        }
        
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    currentLocation.postValue(location);
                    Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
                }
            }
        };
        
        locationProvider.startLocationUpdates(locationCallback);
    }
    
    /**
     * Сбрасывает ошибку
     */
    public void clearError() {
        locationError.setValue(null);
    }
    
    /**
     * Обновляет локацию вручную (для тестирования)
     */
    public void updateLocation(Location location) {
        currentLocation.setValue(location);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        locationProvider.shutdown();
        executorService.shutdown();
    }
}