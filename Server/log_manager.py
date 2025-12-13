import json
import os
import time
from datetime import datetime
from pathlib import Path
import logging
from logging.handlers import RotatingFileHandler


class LogManager:
    """Менеджер для сбора и записи логов работы сервера"""

    def __init__(self, log_dir="logs", max_log_size=10 * 1024 * 1024, backup_count=5):
        """
        Инициализация менеджера логов

        Args:
            log_dir (str): Директория для хранения логов
            max_log_size (int): Максимальный размер файла лога в байтах (по умолчанию 10MB)
            backup_count (int): Количество резервных копий (по умолчанию 5)
        """
        self.base_dir = Path(__file__).parent
        self.log_dir = self.base_dir / log_dir

        # Создаем директорию для логов, если ее нет
        self.log_dir.mkdir(exist_ok=True)

        # Инициализируем статистику
        self.stats_file = self.log_dir / "usage_stats.json"
        self._init_stats()

        # Настраиваем систему логирования
        self._setup_logging(max_log_size, backup_count)

        # Логируем запуск системы
        self.log_request("SYSTEM", "LogManager initialized", {"status": "ready"})

    def _init_stats(self):
        """Инициализация файла статистики"""
        if not self.stats_file.exists():
            default_stats = {
                "total_requests": 0,
                "endpoints": {},
                "status_codes": {},
                "errors": 0,
                "start_time": datetime.utcnow().isoformat(),
                "last_update": datetime.utcnow().isoformat()
            }
            self._save_stats(default_stats)

    def _setup_logging(self, max_log_size, backup_count):
        """Настройка системы логирования с ротацией"""

        # Форматтер для логов
        formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )

        # Хендлер для общих логов (запросы)
        self.request_logger = logging.getLogger('request_logger')
        self.request_logger.setLevel(logging.INFO)

        request_handler = RotatingFileHandler(
            filename=self.log_dir / "requests.log",
            maxBytes=max_log_size,
            backupCount=backup_count,
            encoding='utf-8'
        )
        request_handler.setFormatter(formatter)
        self.request_logger.addHandler(request_handler)

        # Хендлер для ошибок
        self.error_logger = logging.getLogger('error_logger')
        self.error_logger.setLevel(logging.ERROR)

        error_handler = RotatingFileHandler(
            filename=self.log_dir / "errors.log",
            maxBytes=max_log_size,
            backupCount=backup_count,
            encoding='utf-8'
        )
        error_handler.setFormatter(formatter)
        self.error_logger.addHandler(error_handler)

        # Хендлер для логов клиента
        self.client_logger = logging.getLogger('client_logger')
        self.client_logger.setLevel(logging.INFO)

        client_handler = RotatingFileHandler(
            filename=self.log_dir / "client.log",
            maxBytes=max_log_size,
            backupCount=backup_count,
            encoding='utf-8'
        )
        client_handler.setFormatter(formatter)
        self.client_logger.addHandler(client_handler)

    def log_request(self, endpoint, client_ip, params=None, status_code=200, response_time=None):
        """
        Логирование информации о запросе

        Args:
            endpoint (str): Путь эндпоинта
            client_ip (str): IP адрес клиента
            params (dict): Параметры запроса
            status_code (int): HTTP статус код ответа
            response_time (float): Время обработки запроса в секундах
        """
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "type": "request",
            "endpoint": endpoint,
            "client_ip": client_ip,
            "params": params or {},
            "status_code": status_code,
            "response_time_ms": round(response_time * 1000, 2) if response_time else None
        }

        # Записываем в лог файл
        self.request_logger.info(json.dumps(log_entry, ensure_ascii=False))

        # Обновляем статистику
        self._update_request_stats(endpoint, status_code)

        # Выводим в консоль для отладки (опционально)
        print(f"[REQUEST] {endpoint} - {status_code} - {client_ip}")

    def log_error(self, error_type, message, traceback=None, endpoint=None, client_ip=None):
        """
        Логирование ошибок

        Args:
            error_type (str): Тип ошибки (например, "ValidationError", "ServerError")
            message (str): Сообщение об ошибке
            traceback (str): Стек вызовов
            endpoint (str): Эндпоинт где произошла ошибка
            client_ip (str): IP адрес клиента
        """
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "type": "error",
            "error_type": error_type,
            "message": message,
            "endpoint": endpoint,
            "client_ip": client_ip,
            "traceback": traceback
        }

        # Записываем в лог файл
        self.error_logger.error(json.dumps(log_entry, ensure_ascii=False))

        # Обновляем статистику
        self._update_error_stats()

        # Выводим в консоль для отладки
        print(f"[ERROR] {error_type}: {message}")

    def log_client(self, log_data, client_ip):
        """
        Логирование информации от клиента

        Args:
            log_data (dict): Данные лога от клиента
            client_ip (str): IP адрес клиента
        """
        log_entry = {
            "timestamp": datetime.utcnow().isoformat(),
            "type": "client",
            "client_ip": client_ip,
            "data": log_data
        }

        # Записываем в лог файл
        self.client_logger.info(json.dumps(log_entry, ensure_ascii=False))

        print(f"[CLIENT LOG] From {client_ip}: {log_data.get('event', 'unknown')}")

    def _update_request_stats(self, endpoint, status_code):
        """Обновление статистики запросов"""
        try:
            with open(self.stats_file, 'r', encoding='utf-8') as f:
                stats = json.load(f)
        except:
            stats = self._init_stats()

        # Обновляем общее количество запросов
        stats["total_requests"] = stats.get("total_requests", 0) + 1

        # Обновляем статистику по эндпоинтам
        if endpoint not in stats["endpoints"]:
            stats["endpoints"][endpoint] = 0
        stats["endpoints"][endpoint] += 1

        # Обновляем статистику по статус кодам
        status_str = str(status_code)
        if status_str not in stats["status_codes"]:
            stats["status_codes"][status_str] = 0
        stats["status_codes"][status_str] += 1

        # Обновляем время последнего изменения
        stats["last_update"] = datetime.utcnow().isoformat()

        self._save_stats(stats)

    def _update_error_stats(self):
        """Обновление статистики ошибок"""
        try:
            with open(self.stats_file, 'r', encoding='utf-8') as f:
                stats = json.load(f)
        except:
            stats = self._init_stats()

        stats["errors"] = stats.get("errors", 0) + 1
        stats["last_update"] = datetime.utcnow().isoformat()

        self._save_stats(stats)

    def _save_stats(self, stats):
        """Сохранение статистики в файл"""
        try:
            with open(self.stats_file, 'w', encoding='utf-8') as f:
                json.dump(stats, f, indent=2, ensure_ascii=False)
        except Exception as e:
            print(f"Ошибка при сохранении статистики: {e}")

    def get_stats(self):
        """Получение текущей статистики"""
        try:
            with open(self.stats_file, 'r', encoding='utf-8') as f:
                return json.load(f)
        except:
            return self._init_stats()

    def cleanup_old_logs(self, days_to_keep=30):
        """
        Очистка старых лог-файлов

        Args:
            days_to_keep (int): Количество дней для хранения логов
        """
        cutoff_time = time.time() - (days_to_keep * 24 * 60 * 60)

        for log_file in self.log_dir.glob("*.log*"):
            # Проверяем время последнего изменения файла
            if log_file.stat().st_mtime < cutoff_time:
                try:
                    log_file.unlink()
                    print(f"Удален старый лог-файл: {log_file.name}")
                except Exception as e:
                    print(f"Ошибка при удалении {log_file}: {e}")


# Создаем глобальный экземпляр LogManager
log_manager = LogManager()