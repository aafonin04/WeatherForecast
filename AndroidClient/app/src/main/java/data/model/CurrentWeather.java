package data.model;

import com.google.gson.annotations.SerializedName;
public class CurrentWeather {
    @SerializedName("city")
    private String city;

    @SerializedName("temperature")
    private double temperature;

    @SerializedName("condition")
    private String condition;

    @SerializedName("humidity")
    private int humidity;

    @SerializedName("wind_speed")
    private double windSpeed;

    @SerializedName("wind_direction")
    private String windDirection;

    @SerializedName("timestamp")
    private String timestamp;

    public String getCity() { return city; }
    public double getTemperature() { return temperature; }
    public String getCondition() { return condition; }
    public int getHumidity() { return humidity; }
    public double getWindSpeed() { return windSpeed; }
    public String getWindDirection() { return windDirection; }
    public String getTimestamp() { return timestamp; }

}
