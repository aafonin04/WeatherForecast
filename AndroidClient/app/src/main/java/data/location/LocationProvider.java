package com.example.weatherforecast.data.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocationProvider {
    private static final String TAG = "LocationProvider";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final long LOCATION_UPDATE_INTERVAL = 10000L; // 10 секунд
    private static final long FASTEST_UPDATE_INTERVAL = 5000L;   // 5 секунд
    
    private final Context context;
    private final FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executorService;
    
    private LocationCallback locationCallback;
    
    public LocationProvider(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Проверяет, есть ли необходимые разрешения
     */
    public boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Запрашивает разрешения у пользователя
     */
    public void requestLocationPermission(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }
    
    /**
     * Получает последнюю известную локацию (асинхронно)
     */
    public interface LocationCallbackListener {
        void onLocationResult(Location location);
        void onLocationError(String error);
    }
    
    @SuppressLint("MissingPermission")
    public void getLastKnownLocation(LocationCallbackListener listener) {
        if (!hasLocationPermission()) {
            listener.onLocationError("Location permission not granted");
            return;
        }
        
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(executorService, location -> {
                    if (location != null) {
                        listener.onLocationResult(location);
                    } else {
                        listener.onLocationError("No last known location available");
                    }
                })
                .addOnFailureListener(executorService, e -> {
                    Log.e(TAG, "Error getting last location", e);
                    listener.onLocationError("Failed to get location: " + e.getMessage());
                });
    }
    
    /**
     * Начинает получать обновления локации
     */
    @SuppressLint("MissingPermission")
    public void startLocationUpdates(LocationCallback callback) {
        if (!hasLocationPermission()) {
            Log.e(TAG, "Cannot start location updates: permission denied");
            return;
        }
        
        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                LOCATION_UPDATE_INTERVAL
        )
                .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                .setWaitForAccurateLocation(true)
                .build();
        
        this.locationCallback = callback;
        
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                callback,
                Looper.getMainLooper()
        ).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to request location updates", e);
        });
    }
    
    /**
     * Останавливает получение обновлений локации
     */
    public void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
    }
    
    /**
     * Проверяет, включены ли службы локации
     */
    public boolean isLocationEnabled() {
        try {
            android.location.LocationManager locationManager = 
                    (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Error checking location services", e);
            return false;
        }
    }
    
    /**
     * Получает локацию через сеть (менее точный метод)
     */
    @SuppressLint("MissingPermission")
    public void getNetworkLocation(LocationCallbackListener listener) {
        if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            listener.onLocationError("Coarse location permission not granted");
            return;
        }
        
        Task<Location> locationTask = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                null
        );
        
        locationTask.addOnSuccessListener(executorService, location -> {
            if (location != null) {
                listener.onLocationResult(location);
            } else {
                listener.onLocationError("Failed to get network location");
            }
        }).addOnFailureListener(executorService, e -> {
            Log.e(TAG, "Error getting network location", e);
            listener.onLocationError("Network location error: " + e.getMessage());
        });
    }
    
    /**
     * Получает локацию с автоматическим fallback
     */
    public void getLocationWithFallback(LocationCallbackListener listener) {
        getLastKnownLocation(new LocationCallbackListener() {
            @Override
            public void onLocationResult(Location location) {
                listener.onLocationResult(location);
            }
            
            @Override
            public void onLocationError(String error) {
                Log.w(TAG, "GPS location failed, trying network: " + error);
                getNetworkLocation(listener);
            }
        });
    }
    
    /**
     * Освобождает ресурсы
     */
    public void shutdown() {
        stopLocationUpdates();
        executorService.shutdown();
    }
    
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == 
                PackageManager.PERMISSION_GRANTED;
    }
}