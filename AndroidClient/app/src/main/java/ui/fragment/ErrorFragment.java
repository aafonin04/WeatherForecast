package com.example.weatherforecast.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.weatherforecast.R;
import com.example.weatherforecast.error.ErrorState;
import com.example.weatherforecast.error.ErrorType;

public class ErrorFragment extends Fragment {
    
    private static final String ARG_ERROR_STATE = "error_state";
    private static final String ARG_RETRY_ACTION = "retry_action";
    
    private ErrorState errorState;
    private Runnable retryAction;
    
    private TextView errorTitle;
    private TextView errorMessage;
    private TextView errorDetails;
    private ImageView errorIcon;
    private Button retryButton;
    private Button settingsButton;
    
    public static ErrorFragment newInstance(@NonNull ErrorState errorState, @Nullable Runnable retryAction) {
        ErrorFragment fragment = new ErrorFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_ERROR_STATE, errorState);
        // Note: We can't pass Runnable in Bundle. We'll handle it via callback.
        fragment.setArguments(args);
        fragment.retryAction = retryAction;
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (getArguments() != null) {
            errorState = (ErrorState) getArguments().getSerializable(ARG_ERROR_STATE);
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_error, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        errorTitle = view.findViewById(R.id.error_title);
        errorMessage = view.findViewById(R.id.error_message);
        errorDetails = view.findViewById(R.id.error_details);
        errorIcon = view.findViewById(R.id.error_icon);
        retryButton = view.findViewById(R.id.retry_button);
        settingsButton = view.findViewById(R.id.settings_button);
        
        setupErrorDisplay();
        setupButtons();
    }
    
    private void setupErrorDisplay() {
        if (errorState == null) return;
        
        // Устанавливаем заголовок и сообщение
        errorTitle.setText(getErrorTitle(errorState.getErrorType()));
        errorMessage.setText(errorState.getMessage());
        
        // Устанавливаем иконку в зависимости от типа ошибки
        int iconResId = getErrorIcon(errorState.getErrorType());
        if (iconResId != 0) {
            errorIcon.setImageResource(iconResId);
        }
        
        // Показываем детали ошибки для разработчиков (в DEBUG режиме)
        if (BuildConfig.DEBUG && errorState.getThrowable() != null) {
            errorDetails.setVisibility(View.VISIBLE);
            errorDetails.setText(getErrorDetails(errorState));
        } else {
            errorDetails.setVisibility(View.GONE);
        }
    }
    
    private void setupButtons() {
        if (errorState == null) return;
        
        // Настраиваем кнопку повтора
        retryButton.setOnClickListener(v -> {
            if (retryAction != null) {
                retryAction.run();
            }
            // Возвращаемся назад
            requireActivity().onBackPressed();
        });
        
        // Показываем/скрываем кнопку повтора
        boolean showRetry = shouldShowRetryButton(errorState.getErrorType());
        retryButton.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        
        // Настраиваем кнопку настроек (для ошибок разрешений)
        settingsButton.setOnClickListener(v -> {
            openAppSettings();
        });
        
        // Показываем кнопку настроек только для ошибок разрешений
        boolean showSettings = errorState.getErrorType() == ErrorType.LOCATION_PERMISSION_DENIED;
        settingsButton.setVisibility(showSettings ? View.VISIBLE : View.GONE);
    }
    
    private String getErrorTitle(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_UNAVAILABLE:
            case NETWORK_TIMEOUT:
                return "No Internet Connection";
            case SERVER_UNAVAILABLE:
            case SERVER_ERROR:
                return "Server Error";
            case LOCATION_PERMISSION_DENIED:
                return "Location Permission Required";
            case LOCATION_SERVICES_DISABLED:
                return "Location Services Disabled";
            case INVALID_DATA:
                return "Data Error";
            default:
                return "Something Went Wrong";
        }
    }
    
    private int getErrorIcon(ErrorType errorType) {
        switch (errorType) {
            case NETWORK_UNAVAILABLE:
            case NETWORK_TIMEOUT:
                return R.drawable.ic_no_wifi;
            case SERVER_UNAVAILABLE:
            case SERVER_ERROR:
                return R.drawable.ic_server_error;
            case LOCATION_PERMISSION_DENIED:
            case LOCATION_SERVICES_DISABLED:
                return R.drawable.ic_location_off;
            case INVALID_DATA:
                return R.drawable.ic_data_error;
            default:
                return R.drawable.ic_error;
        }
    }
    
    private String getErrorDetails(ErrorState errorState) {
        StringBuilder details = new StringBuilder();
        details.append("Error Type: ").append(errorState.getErrorType().name()).append("\n");
        details.append("Time: ").append(new java.util.Date(errorState.getTimestamp())).append("\n");
        
        if (errorState.getThrowable() != null) {
            details.append("Exception: ").append(errorState.getThrowable().getClass().getName()).append("\n");
            details.append("Message: ").append(errorState.getThrowable().getMessage()).append("\n");
            
            StackTraceElement[] stackTrace = errorState.getThrowable().getStackTrace();
            if (stackTrace.length > 0) {
                details.append("Stack Trace: ").append(stackTrace[0].toString());
            }
        }
        
        return details.toString();
    }
    
    private boolean shouldShowRetryButton(ErrorType errorType) {
        return errorType != ErrorType.LOCATION_PERMISSION_DENIED &&
               errorType != ErrorType.API_KEY_MISSING;
    }
    
    private void openAppSettings() {
        android.content.Intent intent = new android.content.Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        );
        intent.setData(android.net.Uri.fromParts("package", requireContext().getPackageName(), null));
        startActivity(intent);
    }
}