package ui.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import data.model.CurrentWeather;
import data.model.ForecastData;
import data.repository.WeatherRepository;
import domain.usecase.GetCurrentWeatherUseCase;
import domain.usecase.GetForecastUseCase;

public class WeatherViewModel extends ViewModel {
    private GetCurrentWeatherUseCase getCurrentUseCase;
    private GetForecastUseCase getForecastUseCase;
    private LiveData<CurrentWeather> currentData;
    private LiveData<ForecastData> forecastData;
    private LiveData<String> error;
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public WeatherViewModel() {
        WeatherRepository repo = new WeatherRepository();
        getCurrentUseCase = new GetCurrentWeatherUseCase(repo);
        getForecastUseCase = new GetForecastUseCase(repo);
    }

    public void loadCurrentWeather(double lat, double lon) {
        isLoading.setValue(true);
        currentData = getCurrentUseCase.execute(lat, lon);
        error = getCurrentUseCase.getError();
        isLoading.setValue(false);
    }

    public void loadForecast(double lat, double lon) {
        forecastData = getForecastUseCase.execute(lat, lon);
        error = getForecastUseCase.getError();
    }

    public LiveData<CurrentWeather> getCurrentData() {
        return currentData;
    }

    public LiveData<ForecastData> getForecastData() {
        return forecastData;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

}
