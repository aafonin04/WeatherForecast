package com.example.weatherforecast.error;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class ErrorHandler {
    private static final String TAG = "ErrorHandler";
    private final Context context;
    private final LogManager logManager;

    public ErrorHandler(Context context) {
        this.context = context.getApplicationContext();
        this.logManager = new LogManager(context);
    }

    /**
     * Проверяет доступность сети
     */
    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = 
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                       capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                       capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
            }
        } else {
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
        return false;
    }

    /**
     * Обрабатывает ошибку и возвращает ErrorState
     */
    @NonNull
    public ErrorState handleError(@NonNull Throwable throwable, boolean isCritical) {
        ErrorType errorType = ErrorType.fromThrowable(throwable);
        String detailedMessage = getDetailedErrorMessage(errorType, throwable);
        
        ErrorState errorState = new ErrorState(errorType, detailedMessage, throwable, isCritical);
        
        // Логируем ошибку
        logError(errorState);
        
        return errorState;
    }

    /**
     * Обрабатывает ошибку по типу
     */
    @NonNull
    public ErrorState handleError(@NonNull ErrorType errorType, boolean isCritical) {
        return handleError(errorType, null, isCritical);
    }

    /**
     * Обрабатывает ошибку с кастомным сообщением
     */
    @NonNull
    public ErrorState handleError(@NonNull ErrorType errorType, @Nullable String message, boolean isCritical) {
        ErrorState errorState = new ErrorState(errorType, message, null, isCritical);
        
        // Логируем ошибку
        logError(errorState);
        
        return errorState;
    }

    /**
     * Показывает Toast с ошибкой
     */
    public void showErrorToast(@NonNull ErrorState errorState) {
        if (context == null) return;
        
        String message = errorState.getMessage();
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Показывает Snackbar с ошибкой
     */
    public void showErrorSnackbar(@NonNull android.view.View view, @NonNull ErrorState errorState) {
        showErrorSnackbar(view, errorState, null);
    }

    /**
     * Показывает Snackbar с ошибкой и действием
     */
    public void showErrorSnackbar(@NonNull android.view.View view, 
                                  @NonNull ErrorState errorState, 
                                  @Nullable String actionText,
                                  @Nullable android.view.View.OnClickListener action) {
        
        Snackbar snackbar = Snackbar.make(view, errorState.getMessage(), Snackbar.LENGTH_LONG);
        
        if (actionText != null && action != null) {
            snackbar.setAction(actionText, action);
        }
        
        // Цвет в зависимости от критичности
        if (errorState.isCritical()) {
            snackbar.setBackgroundTint(context.getResources().getColor(android.R.color.holo_red_dark));
        } else {
            snackbar.setBackgroundTint(context.getResources().getColor(android.R.color.darker_gray));
        }
        
        snackbar.show();
    }

    /**
     * Показывает Snackbar с действием повтора
     */
    public void showRetrySnackbar(@NonNull android.view.View view, 
                                  @NonNull ErrorState errorState,
                                  @NonNull Runnable retryAction) {
        
        showErrorSnackbar(view, errorState, "Retry", v -> retryAction.run());
    }

    /**
     * Получает подробное сообщение об ошибке
     */
    @NonNull
    private String getDetailedErrorMessage(@NonNull ErrorType errorType, @Nullable Throwable throwable) {
        String baseMessage = errorType.getMessage();
        
        if (throwable == null) {
            return baseMessage;
        }
        
        // Добавляем дополнительную информацию для некоторых типов ошибок
        switch (errorType) {
            case NETWORK_UNAVAILABLE:
                return baseMessage + ". Please check your internet connection.";
            case NETWORK_TIMEOUT:
                return baseMessage + ". The request took too long.";
            case SERVER_UNAVAILABLE:
                return baseMessage + ". Please try again later.";
            case LOCATION_PERMISSION_DENIED:
                return baseMessage + ". Go to settings to enable permissions.";
            case LOCATION_SERVICES_DISABLED:
                return baseMessage + ". Please enable GPS.";
            case INVALID_DATA:
                if (throwable.getMessage() != null) {
                    return baseMessage + ": " + throwable.getMessage();
                }
                break;
        }
        
        return baseMessage;
    }

    /**
     * Логирует ошибку
     */
    private void logError(@NonNull ErrorState errorState) {
        Log.e(TAG, "Error occurred: " + errorState);
        
        // Логируем в файл
        logManager.logError(errorState);
        
        // Отправляем логи на сервер, если это не ошибка сети
        if (errorState.getErrorType() != ErrorType.NETWORK_UNAVAILABLE && 
            errorState.getErrorType() != ErrorType.NETWORK_TIMEOUT) {
            
            logManager.sendErrorToServer(errorState);
        }
    }

    /**
     * Проверяет, является ли ошибка критической для показа отдельного экрана
     */
    public boolean shouldShowErrorScreen(@NonNull ErrorState errorState) {
        return errorState.isCritical() || 
               errorState.getErrorType() == ErrorType.SERVER_UNAVAILABLE ||
               errorState.getErrorType() == ErrorType.NETWORK_UNAVAILABLE;
    }

    /**
     * Проверяет, можно ли повторить действие после ошибки
     */
    public boolean isRetryPossible(@NonNull ErrorState errorState) {
        return errorState.getErrorType() != ErrorType.LOCATION_PERMISSION_DENIED &&
               errorState.getErrorType() != ErrorType.API_KEY_MISSING;
    }
}