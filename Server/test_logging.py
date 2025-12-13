import requests
import json
import time

BASE_URL = "http://localhost:5000"


def test_logging_system():
    print("🧪 Тестирование системы логирования")
    print("=" * 50)

    # 1. Отправляем обычные запросы для генерации логов
    print("\n1. Генерация логов запросов:")
    requests.get(f"{BASE_URL}/status")
    requests.get(f"{BASE_URL}/weather/current?lat=55.7558&lon=37.6173")
    requests.get(f"{BASE_URL}/weather/current?lat=95&lon=200")  # Ошибка

    # 2. Отправляем логи от клиента
    print("\n2. Отправка логов от клиента:")
    client_log = {
        "event": "app_started",
        "level": "info",
        "message": "Пользователь запустил приложение",
        "user_id": "user123",
        "device": "Android 12",
        "app_version": "1.0.0",
        "metadata": {
            "screen": "MainActivity",
            "action": "start"
        }
    }

    response = requests.post(
        f"{BASE_URL}/api/logs",
        json=client_log,
        headers={"Content-Type": "application/json"}
    )

    print(f"Статус: {response.status_code}")
    print(f"Ответ: {response.json()}")

    # 3. Получаем статистику
    print("\n3. Получение статистики:")
    stats_response = requests.get(f"{BASE_URL}/api/logs/stats")
    if stats_response.status_code == 200:
        stats = stats_response.json()
        print(f"Всего запросов: {stats['data']['total_requests']}")
        print(f"Ошибок: {stats['data']['total_errors']}")
        print(f"Успешных: {stats['data']['success_rate']}%")

    # 4. Проверяем ошибку сервера (неправильный запрос к логам)
    print("\n4. Проверка обработки ошибок:")
    error_response = requests.post(f"{BASE_URL}/api/logs", data="not json")
    print(f"Статус ошибки: {error_response.status_code}")

    print("\n✅ Тестирование завершено!")


if __name__ == "__main__":
    test_logging_system()