package com.example.weatherforecast.error;

import android.content.Context;
import android.util.Log;

import com.example.weatherforecast.data.network.ApiService;
import com.example.weatherforecast.data.network.RetrofitClient;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LogManager {
    private static final String TAG = "LogManager";
    private static final String LOG_FILE_NAME = "weather_errors.log";
    private static final SimpleDateFormat DATE_FORMAT = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    private final Context context;
    private final ExecutorService executorService;
    private final Gson gson;
    private final ApiService apiService;

    public LogManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.gson = new Gson();
        this.apiService = RetrofitClient.getApiService();
    }

    /**
     * Логирует ошибку в файл
     */
    public void logError(@NonNull ErrorState errorState) {
        executorService.execute(() -> {
            File logFile = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
            
            try (FileWriter writer = new FileWriter(logFile, true)) {
                String timestamp = DATE_FORMAT.format(new Date(errorState.getTimestamp()));
                String logEntry = String.format(Locale.getDefault(),
                        "[%s] %s: %s%n",
                        timestamp,
                        errorState.getErrorType().name(),
                        errorState.getMessage());
                
                if (errorState.getThrowable() != null) {
                    logEntry += "Exception: " + Log.getStackTraceString(errorState.getThrowable()) + "\n";
                }
                
                writer.write(logEntry);
                writer.write("---\n");
                writer.flush();
                
                Log.d(TAG, "Error logged to file: " + errorState.getErrorType());
            } catch (IOException e) {
                Log.e(TAG, "Failed to write error log", e);
            }
        });
    }

    /**
     * Отправляет ошибку на сервер
     */
    public void sendErrorToServer(@NonNull ErrorState errorState) {
        executorService.execute(() -> {
            try {
                // Создаем объект для отправки
                ErrorLog errorLog = new ErrorLog(
                        errorState.getErrorType().name(),
                        errorState.getMessage(),
                        errorState.getTimestamp(),
                        getDeviceInfo()
                );

                String json = gson.toJson(errorLog);
                RequestBody requestBody = RequestBody.create(
                        MediaType.parse("application/json"), 
                        json
                );

                // Отправляем на сервер
                apiService.sendErrorLog(requestBody).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "Error log sent to server successfully");
                        } else {
                            Log.e(TAG, "Failed to send error log: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                        Log.e(TAG, "Failed to send error log", t);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to prepare error log for sending", e);
            }
        });
    }

    /**
     * Получает информацию об устройстве
     */
    private String getDeviceInfo() {
        return String.format(Locale.getDefault(),
                "Device: %s, Android: %s, SDK: %d",
                Build.MODEL,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT);
    }

    /**
     * Класс для отправки логов на сервер
     */
    private static class ErrorLog {
        private final String errorType;
        private final String message;
        private final long timestamp;
        private final String deviceInfo;

        ErrorLog(String errorType, String message, long timestamp, String deviceInfo) {
            this.errorType = errorType;
            this.message = message;
            this.timestamp = timestamp;
            this.deviceInfo = deviceInfo;
        }
    }

    /**
     * Очищает старые логи
     */
    public void cleanupOldLogs(long maxAgeMillis) {
        executorService.execute(() -> {
            File logFile = new File(context.getExternalFilesDir(null), LOG_FILE_NAME);
            if (logFile.exists()) {
                long fileAge = System.currentTimeMillis() - logFile.lastModified();
                if (fileAge > maxAgeMillis) {
                    if (logFile.delete()) {
                        Log.d(TAG, "Old log file deleted");
                    }
                }
            }
        });
    }
}