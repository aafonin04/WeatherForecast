package com.example.weatherforecast;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherforecast.data.location.LocationProvider;
import com.example.weatherforecast.ui.viewmodel.LocationViewModel;
import com.example.weatherforecast.ui.viewmodel.WeatherViewModel;

import data.model.CurrentWeather;
import data.model.ForecastData;
import ui.fragment.CurrentWeatherFragment;

public class MainActivity extends AppCompatActivity {
    
    private LocationViewModel locationViewModel;
    private WeatherViewModel weatherViewModel;
    private LocationProvider locationProvider;
    
    // Для запроса разрешений
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean allGranted = permissions.values().stream().allMatch(granted -> granted);
                        
                        if (allGranted) {
                            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
                            locationViewModel.checkPermissions();
                            locationViewModel.fetchCurrentLocation();
                        } else {
                            if (ActivityCompat.shouldShowRequestPermissionRationale(
                                    this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                                showPermissionRationaleDialog();
                            } else {
                                showSettingsDialog();
                            }
                        }
                    });
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Инициализация ViewModels
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        locationProvider = new LocationProvider(this);
        
        // Настройка фрагмента
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CurrentWeatherFragment())
                .commit();
        
        // Подписка на изменения локации
        setupLocationObservers();
        
        // Проверка разрешений
        checkLocationPermissions();
    }
    
    /**
     * Настройка наблюдателей за локацией
     */
    private void setupLocationObservers() {
        // Наблюдаем за текущей локацией
        locationViewModel.getCurrentLocation().observe(this, new Observer<Location>() {
            @Override
            public void onChanged(Location location) {
                if (location != null) {
                    // Загружаем погоду по полученным координатам
                    weatherViewModel.loadCurrentWeather(location.getLatitude(), location.getLongitude());
                    weatherViewModel.loadForecast(location.getLatitude(), location.getLongitude());
                    
                    // Можно показать Toast с координатами
                    Toast.makeText(MainActivity.this,
                            String.format("Location: %.4f, %.4f", 
                                    location.getLatitude(), location.getLongitude()),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Наблюдаем за ошибками локации
        locationViewModel.getLocationError().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String error) {
                if (error != null && !error.isEmpty()) {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                    
                    // Если ошибка связана с разрешениями, используем координаты по умолчанию
                    if (error.contains("permission") || error.contains("GPS")) {
                        useDefaultLocation();
                    }
                }
            }
        });
    }
    
    /**
     * Проверяет разрешения на локацию
     */
    private void checkLocationPermissions() {
        String[] requiredPermissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        
        boolean hasPermissions = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != 
                    PackageManager.PERMISSION_GRANTED) {
                hasPermissions = false;
                break;
            }
        }
        
        if (hasPermissions) {
            // Разрешения уже есть, запрашиваем локацию
            locationViewModel.checkPermissions();
            if (locationProvider.isLocationEnabled()) {
                locationViewModel.fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
                useDefaultLocation();
            }
        } else {
            // Запрашиваем разрешения
            requestPermissionLauncher.launch(requiredPermissions);
        }
    }
    
    /**
     * Использует координаты по умолчанию
     */
    private void useDefaultLocation() {
        // Координаты по умолчанию (например, Москва)
        double defaultLat = 55.7558;
        double defaultLon = 37.6173;
        
        weatherViewModel.loadCurrentWeather(defaultLat, defaultLon);
        weatherViewModel.loadForecast(defaultLat, defaultLon);
        
        Toast.makeText(this, 
                "Using default location (Moscow)", 
                Toast.LENGTH_LONG).show();
    }
    
    /**
     * Показывает диалог с объяснением необходимости разрешений
     */
    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs location permission to show weather for your area. " +
                        "Please grant the permission to continue.")
                .setPositiveButton("OK", (dialog, which) -> {
                    // Запрашиваем снова
                    requestPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    useDefaultLocation();
                })
                .show();
    }
    
    /**
     * Показывает диалог для перехода в настройки
     */
    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("You have permanently denied location permission. " +
                        "Please enable it in app settings.")
                .setPositiveButton("Go to Settings", (dialog, which) -> {
                    // Открываем настройки приложения
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Use Default Location", (dialog, which) -> {
                    useDefaultLocation();
                })
                .show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == 1001) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(
                        this,
                        "Location permission is required for accurate weather",
                        Toast.LENGTH_LONG
                ).show();
                useDefaultLocation();
            } else {
                locationViewModel.fetchCurrentLocation();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // При возвращении в приложение проверяем, возможно, пользователь включил локацию
        if (locationProvider.hasLocationPermission() && locationProvider.isLocationEnabled()) {
            locationViewModel.fetchCurrentLocation();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationProvider != null) {
            locationProvider.shutdown();
        }
    }
}