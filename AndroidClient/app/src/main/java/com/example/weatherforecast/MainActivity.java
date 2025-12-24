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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.weatherforecast.data.location.LocationProvider;
import com.example.weatherforecast.data.location.LocationViewModel;
import com.example.weatherforecast.databinding.ActivityMainBinding;
import com.example.weatherforecast.ui.viewmodel.WeatherViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private LocationViewModel locationViewModel;
    private WeatherViewModel weatherViewModel;
    private LocationProvider locationProvider;
    private ActivityMainBinding binding;
    private NavController navController;

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean allGranted = permissions.values().stream()
                                .allMatch(granted -> granted);

                        if (allGranted) {
                            onLocationPermissionGranted();
                        } else {
                            handlePermissionDenied();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeViewModels();
        setupNavigation();
        setupLocationObservers();

        if (savedInstanceState == null) {
            checkLocationPermissions();
        }
    }

    private void initializeViewModels() {
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        weatherViewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        locationProvider = new LocationProvider(this);
    }

    private void setupNavigation() {
        // Находим NavController из FragmentContainerView
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // Настраиваем BottomNavigationView
        BottomNavigationView bottomNavigationView = binding.bottomNavigation;
        NavigationUI.setupWithNavController(bottomNavigationView, navController);

        // Настраиваем обработчик выбора пунктов меню
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            // Если пытаемся перейти на текущий фрагмент, ничего не делаем
            if (navController.getCurrentDestination() != null &&
                    navController.getCurrentDestination().getId() == itemId) {
                return true;
            }

            // Навигация к выбранному фрагменту
            try {
                navController.navigate(itemId);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });

        // Обработка повторного нажатия на текущий пункт
        bottomNavigationView.setOnItemReselectedListener(item -> {
            // При повторном нажатии на активный пункт - скроллим к началу
            int currentFragmentId = navController.getCurrentDestination().getId();

            // Можно добавить логику скролла к началу для каждого фрагмента
            if (currentFragmentId == R.id.currentWeatherFragment) {
                // Скролл к началу в CurrentWeatherFragment
            } else if (currentFragmentId == R.id.forecastFragment) {
                // Скролл к началу в ForecastFragment
            }
        });
    }

    private void setupLocationObservers() {
        locationViewModel.getCurrentLocation().observe(this, location -> {
            if (location != null) {
                weatherViewModel.loadCurrentWeather(location.getLatitude(), location.getLongitude());
                weatherViewModel.loadForecast(location.getLatitude(), location.getLongitude());

                Toast.makeText(this,
                        String.format("Location updated: %.4f, %.4f",
                                location.getLatitude(), location.getLongitude()),
                        Toast.LENGTH_SHORT).show();
            }
        });

        locationViewModel.getLocationError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();

                if (error.contains("permission") || error.contains("GPS")) {
                    useDefaultLocation();
                }
            }
        });
    }

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
            locationViewModel.checkPermissions();
            if (locationProvider.isLocationEnabled()) {
                locationViewModel.fetchCurrentLocation();
            } else {
                Toast.makeText(this, "Please enable location services", Toast.LENGTH_LONG).show();
                useDefaultLocation();
            }
        } else {
            requestPermissionLauncher.launch(requiredPermissions);
        }
    }

    private void onLocationPermissionGranted() {
        Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
        locationViewModel.checkPermissions();
        locationViewModel.fetchCurrentLocation();
    }

    private void handlePermissionDenied() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            showPermissionRationaleDialog();
        } else {
            showSettingsDialog();
        }
    }

    private void useDefaultLocation() {
        double defaultLat = 55.7558;
        double defaultLon = 37.6173;

        weatherViewModel.loadCurrentWeather(defaultLat, defaultLon);
        weatherViewModel.loadForecast(defaultLat, defaultLon);

        Toast.makeText(this, "Using default location (Moscow)", Toast.LENGTH_LONG).show();
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs location permission to show weather for your area.")
                .setPositiveButton("OK", (dialog, which) -> {
                    requestPermissionLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    });
                })
                .setNegativeButton("Cancel", (dialog, which) -> useDefaultLocation())
                .show();
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Please enable location permission in app settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                })
                .setNegativeButton("Use Default", (dialog, which) -> useDefaultLocation())
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        // Если мы не на главном экране, обрабатываем кнопку Back
        if (navController.getCurrentDestination() != null &&
                navController.getCurrentDestination().getId() != R.id.currentWeatherFragment) {

            // Попытка вернуться назад в стеке навигации
            if (!navController.popBackStack()) {
                super.onBackPressed();
            }
        } else {
            // На главном экране - двойное нажатие для выхода
            handleBackPressExit();
        }
    }

    private long backPressedTime = 0;

    private void handleBackPressExit() {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            super.onBackPressed();
        } else {
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
            backPressedTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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