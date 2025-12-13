import json
from pathlib import Path
import logging

# Настройка логирования для модуля
logger = logging.getLogger(__name__)

BASE_DIR = Path(__file__).parent
DATA_DIR = BASE_DIR / "data" / "weather"


def get_city_by_coords(lat, lon):
    """Получение города по координатам с логированием"""
    try:
        with open(BASE_DIR / "location_mapping.json", "r", encoding="utf-8") as f:
            mapping = json.load(f)
        key = f"{lat},{lon}"
        city = mapping.get(key)

        if not city:
            logger.warning(f"No city found for coordinates: {lat}, {lon}")

        return city

    except FileNotFoundError:
        logger.error(f"Mapping file not found: {BASE_DIR / 'location_mapping.json'}")
        return None
    except json.JSONDecodeError as e:
        logger.error(f"Error parsing mapping file: {e}")
        return None
    except Exception as e:
        logger.error(f"Unexpected error in get_city_by_coords: {e}")
        return None


def get_current_weather(city_file_name):
    """Получение текущей погоды с логированием"""
    try:
        path = DATA_DIR / "current" / f"{city_file_name}.json"

        if not path.exists():
            logger.error(f"Weather file not found: {path}")
            raise FileNotFoundError(f"Weather data not found for {city_file_name}")

        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        logger.info(f"Weather data loaded for {city_file_name}")
        return data

    except json.JSONDecodeError as e:
        logger.error(f"Error parsing weather file {city_file_name}.json: {e}")
        raise
    except Exception as e:
        logger.error(f"Unexpected error in get_current_weather: {e}")
        raise


def get_forecast_weather(city_file_name):
    """Получение прогноза погоды с логированием"""
    try:
        path = DATA_DIR / "forecast" / f"{city_file_name}.json"

        if not path.exists():
            logger.error(f"Forecast file not found: {path}")
            raise FileNotFoundError(f"Forecast data not found for {city_file_name}")

        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        logger.info(f"Forecast data loaded for {city_file_name}")
        return data

    except json.JSONDecodeError as e:
        logger.error(f"Error parsing forecast file {city_file_name}.json: {e}")
        raise
    except Exception as e:
        logger.error(f"Unexpected error in get_forecast_weather: {e}")
        raise