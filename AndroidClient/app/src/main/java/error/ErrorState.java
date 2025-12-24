package com.example.weatherforecast.error;

import androidx.annotation.NonNull;

public class ErrorState {
    private final ErrorType errorType;
    private final String message;
    private final Throwable throwable;
    private final boolean isCritical;
    private final long timestamp;

    public ErrorState(@NonNull ErrorType errorType) {
        this(errorType, errorType.getMessage(), null, false);
    }

    public ErrorState(@NonNull ErrorType errorType, String message) {
        this(errorType, message, null, false);
    }

    public ErrorState(@NonNull ErrorType errorType, String message, Throwable throwable, boolean isCritical) {
        this.errorType = errorType;
        this.message = message != null ? message : errorType.getMessage();
        this.throwable = throwable;
        this.isCritical = isCritical;
        this.timestamp = System.currentTimeMillis();
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public boolean isCritical() {
        return isCritical;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "ErrorState{" +
                "errorType=" + errorType +
                ", message='" + message + '\'' +
                ", isCritical=" + isCritical +
                ", timestamp=" + timestamp +
                '}';
    }
}