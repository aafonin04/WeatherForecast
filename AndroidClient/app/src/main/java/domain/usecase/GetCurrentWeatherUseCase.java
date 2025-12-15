package domain.usecase;

import androidx.lifecycle.LiveData;

import data.model.CurrentWeather;
import data.repository.WeatherRepository;

public class GetCurrentWeatherUseCase {
    private WeatherRepository repository;

    public GetCurrentWeatherUseCase(WeatherRepository repository) {
        this.repository = repository;
    }

    public LiveData<CurrentWeather> execute(double lat, double lon) {
        return repository.getCurrentWeather(lat, lon);
    }

    public LiveData<String> getError() {
        return repository.getErrorMessage();
    }
}
