package data.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
public class ForecastData {
    @SerializedName("city")
    private String city;

    @SerializedName("forecast")
    private List<ForecastItem> forecast;

    public static class ForecastItem {
        @SerializedName("datetime")
        private String datetime;

        @SerializedName("temperature_min")
        private int temperatureMin;

        @SerializedName("temperature_max")
        private int temperatureMax;

        @SerializedName("condition")
        private String condition;

        public String getDatetime() { return datetime; }
        public int getTemperatureMin() { return temperatureMin; }
        public int getTemperatureMax() { return temperatureMax; }
        public String getCondition() { return condition; }
    }

    public String getCity() { return city; }
    public List<ForecastItem> getForecast() { return forecast; }
}
