package data.model;

import com.google.gson.annotations.SerializedName;
public class ApiResponse<T> {
    @SerializedName("data")
    private T data;

    @SerializedName("status")
    private String status;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("message")
    private String message;

    public T getData() { return data; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public String getMessage() { return message; }

    public boolean isSuccess() { return "success".equals(status); }

}
