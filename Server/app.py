from flask import Flask, jsonify, request
from weather_provider import (
    get_city_by_coords,
    get_current_weather,
    get_forecast_weather
)
from datetime import datetime
import time
from log_manager import log_manager

app = Flask(__name__)

# Middleware для логирования всех запросов
@app.before_request
def before_request():
    request.start_time = time.time()


@app.after_request
def after_request(response):
    # Получаем время обработки
    response_time = time.time() - request.start_time if hasattr(request, 'start_time') else None

    # Логируем запрос
    log_manager.log_request(
        endpoint=request.path,
        client_ip=request.remote_addr,
        params=dict(request.args),
        status_code=response.status_code,
        response_time=response_time
    )

    return response

# Добавлена проверка корректности географических координат
def validate_coordinates(lat, lon):
    try:
        lat_f=float(lat)
        lon_f=float(lon)
        return -90 <= lat_f <= 90 and -180 <= lon_f <= 180
    except (ValueError, TypeError):
        # Логируем ошибку валидации
        log_manager.log_error(
            error_type="ValidationError",
            message=f"Invalid coordinates: lat={lat}, lon={lon}",
            endpoint=request.path if request else None,
            client_ip=request.remote_addr if request else None
        )
        return False

# Добавлена фабрика стандартизированных ответов API
def create_response(data=None, status="success", message="",http_status=200):

    return jsonify({
        "data": data,
        "status": status,
        "timestamp": datetime.utcnow().isoformat(),
        "message": message
    }), http_status

@app.route('/status', methods=['GET'])
def status():
    return create_response(
        data={
            "server": "running",
            "version": "1.0",
            "log_stats": log_manager.get_stats()  # Добавляем статистику в ответ
        },
        message="Server is operational"
    )

@app.route("/weather/current")
def weather_current():
    try:
        lat = request.args.get("lat")
        lon = request.args.get("lon")

        # Валидация параметров
        if not lat or not lon:
            return create_response(
                status="error",
                message="Missing lat or lon parameters",
                http_status=400
            )

        if not validate_coordinates(lat, lon):
            return create_response(
                status="error",
                message="Invalid coordinates range. Use: -90≤lat≤90, -180≤lon≤180",
                http_status=400
            )

        # Поиск города по координатам
        city = get_city_by_coords(lat, lon)
        if not city:
            return create_response(
                status="error",
                message="Weather data not available for these coordinates",
                http_status=404
            )

        # Получение данных о погоде
        weather_data = get_current_weather(city)
        return create_response(
            data=weather_data,
            message="Current weather retrieved successfully"
        )

    except Exception as e:
        # Логируем внутреннюю ошибку сервера
        import traceback
        log_manager.log_error(
            error_type="ServerError",
            message=f"Error in weather_current: {str(e)}",
            traceback=traceback.format_exc(),
            endpoint="/weather/current",
            client_ip=request.remote_addr
        )

        return create_response(
            status="error",
            message=f"Server error: {str(e)}",
            http_status=500
        )


@app.route("/weather/forecast")
def weather_forecast():
    """Прогноз погоды на 5 дней"""
    try:
        lat = request.args.get("lat")
        lon = request.args.get("lon")

        # Валидация параметров
        if not lat or not lon:
            return create_response(
                status="error",
                message="Missing lat or lon parameters",
                http_status=400
            )

        if not validate_coordinates(lat, lon):
            return create_response(
                status="error",
                message="Invalid coordinates range. Use: -90≤lat≤90, -180≤lon≤180",
                http_status=400
            )

        # Поиск города по координатам
        city = get_city_by_coords(lat, lon)
        if not city:
            return create_response(
                status="error",
                message="Weather data not available for these coordinates",
                http_status=404
            )

        # Получение прогноза
        forecast_data = get_forecast_weather(city)
        return create_response(
            data=forecast_data,
            message="5-day forecast retrieved successfully"
        )

    except Exception as e:
        # Логируем внутреннюю ошибку сервера
        import traceback
        log_manager.log_error(
            error_type="ServerError",
            message=f"Error in weather_forecast: {str(e)}",
            traceback=traceback.format_exc(),
            endpoint="/weather/forecast",
            client_ip=request.remote_addr
        )

        return create_response(
            status="error",
            message=f"Server error: {str(e)}",
            http_status=500
        )


@app.route("/api/logs", methods=['POST'])
def receive_client_logs():
    """
    Эндпоинт для приема логов от клиента

    Ожидает JSON в теле запроса:
    {
        "event": "user_action",
        "level": "info",
        "message": "User opened weather details",
        "metadata": {...}
    }
    """
    try:
        # Проверяем, что запрос содержит JSON
        if not request.is_json:
            return create_response(
                status="error",
                message="Request must be JSON",
                http_status=400
            )

        log_data = request.get_json()

        # Валидация обязательных полей
        if not log_data or not isinstance(log_data, dict):
            return create_response(
                status="error",
                message="Invalid log data format",
                http_status=400
            )

        # Логируем полученные данные
        log_manager.log_client(log_data, request.remote_addr)

        return create_response(
            data={"received": True},
            message="Log received successfully"
        )

    except Exception as e:
        # Логируем ошибку при обработке логов
        import traceback
        log_manager.log_error(
            error_type="LogError",
            message=f"Error processing client log: {str(e)}",
            traceback=traceback.format_exc(),
            endpoint="/api/logs",
            client_ip=request.remote_addr
        )

        return create_response(
            status="error",
            message=f"Error processing log: {str(e)}",
            http_status=500
        )


@app.route("/api/logs/stats", methods=['GET'])
def get_log_statistics():
    """Получение статистики использования API"""
    stats = log_manager.get_stats()

    # Форматируем статистику для ответа
    formatted_stats = {
        "total_requests": stats.get("total_requests", 0),
        "total_errors": stats.get("errors", 0),
        "uptime": stats.get("start_time", ""),
        "last_update": stats.get("last_update", ""),
        "endpoints": stats.get("endpoints", {}),
        "status_codes": stats.get("status_codes", {}),
        "success_rate": round(
            (stats.get("total_requests", 0) - stats.get("errors", 0)) /
            max(stats.get("total_requests", 1), 1) * 100, 2
        )
    }

    return create_response(
        data=formatted_stats,
        message="Statistics retrieved successfully"
    )


@app.route("/api/logs/cleanup", methods=['POST'])
def cleanup_logs():
    """Очистка старых лог-файлов (только для админов)"""
    try:
        # Можно добавить проверку авторизации здесь
        days_to_keep = request.args.get("days", default=30, type=int)

        # Выполняем очистку
        log_manager.cleanup_old_logs(days_to_keep)

        return create_response(
            data={"cleaned": True, "days_kept": days_to_keep},
            message=f"Old logs cleaned up (keeping {days_to_keep} days)"
        )

    except Exception as e:
        log_manager.log_error(
            error_type="CleanupError",
            message=f"Error cleaning logs: {str(e)}",
            endpoint="/api/logs/cleanup",
            client_ip=request.remote_addr
        )

        return create_response(
            status="error",
            message=f"Error cleaning logs: {str(e)}",
            http_status=500
        )


if __name__ == "__main__":
    print("Starting Flask server with logging...")
    print(f"Log directory: {log_manager.log_dir}")
    app.run(host="0.0.0.0", port=5000, debug=True)

