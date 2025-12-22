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
import ui.fragment.CurrentWeatherFragment;
import ui.viewmodel.WeatherViewModel;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setContentView(R.layout.activity_main);
        WeatherViewModel viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new CurrentWeatherFragment())
                .commit();

        double lat = 43.5853;
        double lon = 39.7231;
        viewModel.loadCurrentWeather(lat, lon);
        viewModel.loadForecast(lat, lon);
    }
}