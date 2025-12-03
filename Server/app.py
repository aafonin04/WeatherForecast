from flask import Flask, jsonify, request
from weather_provider import (
    get_city_by_coords,
    get_current_weather,
    get_forecast_weather
)
from datetime import datetime

app = Flask(__name__)

@app.route('/status', methods=['GET'])
def status():
    return jsonify({
        "status": "ok",
        "timestamp": datetime.utcnow().isoformat()
    })

@app.route("/weather/current")
def weather_current():
    lat = request.args.get("lat")
    lon = request.args.get("lon")

    city = get_city_by_coords(lat, lon)
    if not city:
        return jsonify({"error": "Unknown coordinates"}), 404

    data = get_current_weather(city)
    return jsonify(data)

@app.route("/weather/forecast")
def weather_forecast():
    lat = request.args.get("lat")
    lon = request.args.get("lon")

    city = get_city_by_coords(lat, lon)
    if not city:
        return jsonify({"error": "Unknown coordinates"}), 404

    data = get_forecast_weather(city)
    return jsonify(data)

if __name__ == "__main__":
    print("Starting Flask server...")
    app.run(host="0.0.0.0", port=5000, debug=True)
