package com.example.weatherforecast.error;

public enum ErrorType {
    NETWORK_UNAVAILABLE("No internet connection"),
    NETWORK_TIMEOUT("Network timeout"),
    SERVER_UNAVAILABLE("Server is not available"),
    SERVER_ERROR("Server error"),
    LOCATION_PERMISSION_DENIED("Location permission denied"),
    LOCATION_SERVICES_DISABLED("Location services disabled"),
    LOCATION_UNAVAILABLE("Unable to get location"),
    INVALID_DATA("Invalid data received"),
    UNKNOWN_ERROR("Unknown error occurred"),
    API_KEY_MISSING("API key is missing"),
    JSON_PARSING_ERROR("Failed to parse data"),
    GPS_UNAVAILABLE("GPS is not available"),
    CITY_NOT_FOUND("City not found");

    private final String message;

    ErrorType(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static ErrorType fromThrowable(Throwable throwable) {
        if (throwable instanceof java.net.UnknownHostException) {
            return NETWORK_UNAVAILABLE;
        } else if (throwable instanceof java.net.SocketTimeoutException) {
            return NETWORK_TIMEOUT;
        } else if (throwable instanceof java.io.IOException) {
            return NETWORK_UNAVAILABLE;
        } else if (throwable instanceof com.google.gson.JsonSyntaxException) {
            return JSON_PARSING_ERROR;
        } else {
            return UNKNOWN_ERROR;
        }
    }
}