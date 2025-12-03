import json
from pathlib import Path

BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data" / "weather"

def get_city_by_coords(lat, lon):
    with open(BASE_DIR / "location_mapping.json", "r", encoding="utf-8") as f:
        mapping = json.load(f)
    key = f"{lat},{lon}"
    return mapping.get(key)

def get_current_weather(city_file_name):
    path = DATA_DIR / "current" / f"{city_file_name}.json"
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def get_forecast_weather(city_file_name):
    path = DATA_DIR / "forecast" / f"{city_file_name}.json"
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)
