from flask import Flask, jsonify, request
from weather_provider import (
    get_city_by_coords,
    get_current_weather,
    get_forecast_weather
)
from datetime import datetime

app = Flask(__name__)

# Добавлена проверка корректности географических координат
def validate_coordinates(lat, lon):
    try:
        lat_f=float(lat)
        lon_f=float(lon)
        return -90 <= lat_f <= 90 and -180 <= lon_f <= 180
    except (ValueError, TypeError):
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
    #Изменено на стандартное формирование
    return create_response(
        data={"server": "running", "version": "1.0"},
        message="Server is operational"
    )

@app.route("/weather/current")
def weather_current():
    lat = request.args.get("lat")
    lon = request.args.get("lon")

    # Валидация входных параметров
    # Проверка наличия параметров
    if not lat or not lon:
        return create_response(
            status="error",
            message="Missing lat or lon parameters",
            http_status=400  # HTTP 400 Bad Request
        )

    # Проверка корректности диапазонов координат
    if not validate_coordinates(lat, lon):
        return create_response(
            status="error",
            message="Invalid coordinates range. Use: -90≤lat≤90, -180≤lon≤180",
            http_status=400  # HTTP 400 Bad Request
        )

    # Поиск города по координатам
    city = get_city_by_coords(lat, lon)
    if not city:
        # ИЗМЕНЕНО: Использует единый формат для ошибок
        return create_response(
            status="error",
            message="Weather data not available for these coordinates",
            http_status=404  # HTTP 404 Not Found
        )

    # Получение данных о погоде
    try:
        weather_data = get_current_weather(city)
        return create_response(
            data=weather_data,
            message="Current weather retrieved successfully"
        )
    except Exception as e:
        # Обработка внутренних ошибок
        return create_response(
            status="error",
            message=f"Server error: {str(e)}",
            http_status=500  # HTTP 500 Internal Server Error
        )

@app.route("/weather/forecast")
def weather_forecast():
    lat = request.args.get("lat")
    lon = request.args.get("lon")

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
    try:
        forecast_data = get_forecast_weather(city)
        # Добавлено сообщение о 5-дневном прогнозе
        return create_response(
            data=forecast_data,
            message="5-day forecast retrieved successfully"
        )
    except Exception as e:
        return create_response(
            status="error",
            message=f"Server error: {str(e)}",
            http_status=500
        )

if __name__ == "__main__":
    print("Starting Flask server...")
    app.run(host="0.0.0.0", port=5000, debug=True)
