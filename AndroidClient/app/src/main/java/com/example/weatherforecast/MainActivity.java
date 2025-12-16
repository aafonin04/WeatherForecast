package com.example.weatherforecast;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import data.model.CurrentWeather;
import data.model.ForecastData;
import ui.viewmodel.WeatherViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setContentView(R.layout.activity_main);
        WeatherViewModel viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        double lat = 43.5853;
        double lon = 39.7231;
        viewModel.loadCurrentWeather(lat, lon);
        viewModel.loadForecast(lat, lon);

        viewModel.getCurrentData().observe(this, current -> {
            if (current != null) {
                Log.d("WeatherApp", "Получены current данные: city=" + current.getCity() + ", temp=" + current.getTemperature());
                Toast.makeText(this, "Current: " + current.getTemperature() + "°C, " + current.getCondition(), Toast.LENGTH_LONG).show();
            }
        });

        // Наблюдай за данными current
        viewModel.getCurrentData().observe(this, (CurrentWeather current) -> {
            if (current != null) {
                Log.d("WeatherApp", "Получены current данные: city=" + current.getCity() + ", temp=" + current.getTemperature());
                Toast.makeText(this, "Current: " + current.getTemperature() + "°C, " + current.getCondition(), Toast.LENGTH_LONG).show();
            }
        });

    // Наблюдай за данными forecast
        viewModel.getForecastData().observe(this, (ForecastData forecast) -> {
            if (forecast != null && forecast.getForecast() != null) {
                Log.d("WeatherApp", "Получен forecast: дней=" + forecast.getForecast().size());
                String firstDay = forecast.getForecast().get(0).getDatetime() + ": " + forecast.getForecast().get(0).getCondition();
                Toast.makeText(this, "Forecast: " + firstDay, Toast.LENGTH_LONG).show();
            }
        });

    // Наблюдай за ошибками (здесь тип String, обычно инференс работает, но для consistency)
        viewModel.getError().observe(this, (String error) -> {
            if (error != null) {
                Log.e("WeatherApp", "Ошибка: " + error);
                Toast.makeText(this, "Ошибка: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }
}