package domain.usecase;

import androidx.lifecycle.LiveData;

import data.model.ForecastData;
import data.repository.WeatherRepository;

public class GetForecastUseCase {
    private WeatherRepository repository;

    public GetForecastUseCase(WeatherRepository repository) {
        this.repository = repository;
    }

    public LiveData<ForecastData> execute(double lat, double lon) {
        return repository.getForecast(lat, lon);
    }

    public LiveData<String> getError() {
        return repository.getErrorMessage();
    }
}
