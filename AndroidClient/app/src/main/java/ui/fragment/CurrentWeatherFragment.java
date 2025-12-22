package ui.fragment;

import androidx.fragment.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.weatherforecast.databinding.FragmentCurrentWeatherBinding;

import ui.viewmodel.WeatherViewModel;


public class CurrentWeatherFragment extends Fragment {
    private FragmentCurrentWeatherBinding binding;
    private WeatherViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCurrentWeatherBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(WeatherViewModel.class);

        // Обработка загрузки
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            binding.progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            binding.textConditionIcon.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.textTemperature.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.textCondition.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.textHumidity.setVisibility(isLoading ? View.GONE : View.VISIBLE);
            binding.textWind.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        });

        // Обработка ошибок
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null) {
                binding.textError.setText(error);
                binding.textError.setVisibility(View.VISIBLE);
                // Скрыть данные
                binding.textConditionIcon.setVisibility(View.GONE);
                binding.textTemperature.setVisibility(View.GONE);
                binding.textCondition.setVisibility(View.GONE);
                binding.textHumidity.setVisibility(View.GONE);
                binding.textWind.setVisibility(View.GONE);
            } else {
                binding.textError.setVisibility(View.GONE);
            }
        });

        // Обработка данных
        viewModel.getCurrentData().observe(getViewLifecycleOwner(), current -> {
            if (current != null) {
                binding.textTemperature.setText("Температура: " + current.getTemperature() + "°C");
                binding.textCondition.setText("Условия: " + current.getCondition());
                binding.textHumidity.setText("Влажность: " + current.getHumidity() + "%");
                binding.textWind.setText("Ветер: " + current.getWindSpeed() + " m/s, " + current.getWindDirection());

                // Эмодзи-иконка
                String emoji;
                switch (current.getCondition().toLowerCase()) {
                    case "cloudy":
                        emoji = "☁️";
                        break;
                    case "snow":
                        emoji = "❄️";
                        break;
                    case "sunny":
                        emoji = "☀️";
                        break;
                    default:
                        emoji = "❓";
                        break;
                }
                binding.textConditionIcon.setText(emoji);

                // Показать данные
                binding.textConditionIcon.setVisibility(View.VISIBLE);
                binding.textTemperature.setVisibility(View.VISIBLE);
                binding.textCondition.setVisibility(View.VISIBLE);
                binding.textHumidity.setVisibility(View.VISIBLE);
                binding.textWind.setVisibility(View.VISIBLE);
            } else {
                binding.textTemperature.setText("Данные недоступны");

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
