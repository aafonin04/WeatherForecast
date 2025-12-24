package com.example.weatherforecast;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
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
import com.example.weatherforecast.data.repository.WeatherRepository;
import com.example.weatherforecast.error.ErrorHandler;
import com.example.weatherforecast.error.ErrorState;
import com.example.weatherforecast.error.ErrorType;
import com.example.weatherforecast.ui.fragment.ErrorFragment;
import com.example.weatherforecast.ui.viewmodel.WeatherViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private LocationViewModel locationViewModel;
    private WeatherViewModel weatherViewModel;
    private LocationProvider locationProvider;
    private NavController navController;
    private BottomNavigationView bottomNavigationView;
    private ErrorHandler errorHandler;

    // Для запроса разрешений
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissions -> {
                        boolean allGranted = permissions.values().stream().allMatch(granted -> granted);

                        if (allGranted) {
                            onLocationPermissionGranted();
                        } else {
                            handlePermissionDenied();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Инициализация ErrorHandler
        errorHandler = new ErrorHandler(this);
        
        // Инициализация репозитория
        WeatherRepository.initialize(getApplicationContext());
        
        // Инициализация ViewModels
        locationViewModel = new ViewModelProvider(this).get(LocationViewModel.class);
        weatherViewModel = new ViewModelProvider(this, 
                new ViewModelProvider.AndroidViewModelFactory(getApplication()))
                .get(WeatherViewModel.class);
        locationProvider = new LocationProvider(this);

        // Настройка BottomNavigationView
        setupBottomNavigation();

        // Подписка на изменения
        setupObservers();

        // Проверка разрешений
        checkLocationPermissions();
    }

    /**
     * Настройка BottomNavigationView
     */
    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Находим NavController из NavHostFragment
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);

        // Настраиваем BottomNavigationView с NavController
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
                // Обработка ошибки навигации
                handleNavigationError(e);
                return false;
            }
        });

        // Обработка повторного нажатия на текущий пункт
        bottomNavigationView.setOnItemReselectedListener(item -> {
            // При повторном нажатии на активный пункт - скроллим к началу
            // Можно добавить логику для конкретных фрагментов
        });
    }

    /**
     * Настройка всех наблюдателей
     */
    private void setupObservers() {
        setupLocationObservers();
        setupWeatherObservers();
        setupErrorObservers();
    }

    /**
     * Настройка наблюдателей за локацией
     */
    private void setupLocationObservers() {
        // Наблюдаем за текущей локацией
        locationViewModel.getCurrentLocation().observe(this, location -> {
            if (location != null) {
                // Загружаем погоду по полученным координатам
                weatherViewModel.loadCurrentWeather(location.getLatitude(), location.getLongitude());
                weatherViewModel.loadForecast(location.getLatitude(), location.getLongitude());

                // Логирование успешного получения локации
                Toast.makeText(MainActivity.this,
                        String.format("Location updated: %.4f, %.4f",
                                location.getLatitude(), location.getLongitude()),
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Наблюдаем за ошибками локации
        locationViewModel.getLocationError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                // Преобразуем строковую ошибку в ErrorState
                ErrorState errorState;
                if (error.contains("permission")) {
                    errorState = errorHandler.handleError(
                            ErrorType.LOCATION_PERMISSION_DENIED,
                            error,
                            true
                    );
                } else if (error.contains("GPS")) {
                    errorState = errorHandler.handleError(
                            ErrorType.LOCATION_SERVICES_DISABLED,
                            error,
                            false
                    );
                } else {
                    errorState = errorHandler.handleError(
                            ErrorType.LOCATION_UNAVAILABLE,
                            error,
                            false
                    );
                }
                
                // Показываем ошибку
                handleError(errorState);
                
                // Если ошибка связана с разрешениями, используем координаты по умолчанию
                if (error.contains("permission") || error.contains("GPS")) {
                    useDefaultLocation();
                }
            }
        });
    }

    /**
     * Настройка наблюдателей за погодой
     */
    private void setupWeatherObservers() {
        // Наблюдаем за состоянием загрузки
        weatherViewModel.getIsLoading().observe(this, isLoading -> {
            // Здесь можно показать/скрыть индикатор загрузки
            if (isLoading != null && isLoading) {
                // Показать ProgressBar или Skeleton Screen
            } else {
                // Скрыть индикатор загрузки
            }
        });
    }

    /**
     * Настройка наблюдателей за ошибками
     */
    private void setupErrorObservers() {
        // Наблюдаем за структурированными ошибками из WeatherViewModel
        weatherViewModel.getErrorState().observe(this, errorState -> {
            if (errorState != null) {
                handleError(errorState);
            }
        });

        // Наблюдаем за простыми ошибками из WeatherViewModel (для совместимости)
        weatherViewModel.getError().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                // Преобразуем строковую ошибку в ErrorState
                ErrorState errorState = errorHandler.handleError(
                        ErrorType.UNKNOWN_ERROR,
                        errorMessage,
                        false
                );
                handleError(errorState);
            }
        });
    }

    /**
     * Обработка ошибок
     */
    private void handleError(@NonNull ErrorState errorState) {
        // Проверяем, нужно ли показывать экран ошибки
        if (errorHandler.shouldShowErrorScreen(errorState)) {
            showErrorFragment(errorState);
        } else {
            showErrorSnackbar(errorState);
        }
    }

    /**
     * Показать экран ошибки
     */
    private void showErrorFragment(@NonNull ErrorState errorState) {
        ErrorFragment errorFragment = ErrorFragment.newInstance(errorState, this::retryWeatherRequest);
        
        // Скрываем BottomNavigation при показе экрана ошибки
        bottomNavigationView.setVisibility(View.GONE);
        
        // Заменяем текущий фрагмент на экран ошибки
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, errorFragment)
                .addToBackStack("error")
                .commit();
    }

    /**
     * Показать Snackbar с ошибкой
     */
    private void showErrorSnackbar(@NonNull ErrorState errorState) {
        View rootView = findViewById(android.R.id.content);
        if (rootView == null) return;
        
        if (errorHandler.isRetryPossible(errorState)) {
            // Snackbar с возможностью повтора
            errorHandler.showRetrySnackbar(rootView, errorState, this::retryWeatherRequest);
        } else {
            // Простой Snackbar без действия
            errorHandler.showErrorSnackbar(rootView, errorState);
        }
    }

    /**
     * Повторный запрос погоды
     */
    private void retryWeatherRequest() {
        // Проверяем доступность сети перед повторным запросом
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    ErrorType.NETWORK_UNAVAILABLE,
                    "No internet connection for retry",
                    true
            );
            handleError(networkError);
            return;
        }
        
        // Очищаем предыдущие ошибки
        weatherViewModel.clearError();
        
        // Пробуем использовать последнюю известную локацию
        if (locationViewModel.getCurrentLocation().getValue() != null) {
            Location location = locationViewModel.getCurrentLocation().getValue();
            weatherViewModel.loadCurrentWeather(location.getLatitude(), location.getLongitude());
            weatherViewModel.loadForecast(location.getLatitude(), location.getLongitude());
        } else {
            // Если локации нет, используем дефолтную или запрашиваем снова
            useDefaultLocation();
        }
    }

    /**
     * Обработка ошибки навигации
     */
    private void handleNavigationError(Exception e) {
        ErrorState errorState = errorHandler.handleError(
                ErrorType.UNKNOWN_ERROR,
                "Navigation error: " + e.getMessage(),
                false
        );
        showErrorSnackbar(errorState);
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
                // Обработка отключенных сервисов локации
                ErrorState error = errorHandler.handleError(
                        ErrorType.LOCATION_SERVICES_DISABLED,
                        "Please enable location services",
                        false
                );
                handleError(error);
                useDefaultLocation();
            }
        } else {
            // Запрашиваем разрешения
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
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        // Проверяем, находимся ли мы на экране ошибки
        if (getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment) instanceof ErrorFragment) {
            // Возвращаем BottomNavigation
            bottomNavigationView.setVisibility(View.VISIBLE);
        }
        
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
        // При возвращении в приложение проверяем, возможно, пользователь включил локацию
        if (locationProvider.hasLocationPermission() && locationProvider.isLocationEnabled()) {
            locationViewModel.fetchCurrentLocation();
        }
        
        // Проверяем доступность сети при возвращении в приложение
        if (!errorHandler.isNetworkAvailable()) {
            ErrorState networkError = errorHandler.handleError(
                    ErrorType.NETWORK_UNAVAILABLE,
                    "No internet connection",
                    false
            );
            handleError(networkError);
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