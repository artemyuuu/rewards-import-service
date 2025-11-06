# Rewards Import Service
Это минимально рабочая версия сервиса Rewards Import Service, реализующая основную бизнес-логику импорта наград сотрудников из CSV/XLSX по REST API.

##  Что умеет
- Принимает **CSV/XLSX** с колонками:
  `employee_id, employee_full_name, reward_id, reward_name, awarded_at (ISO-8601 Z)`
- Валидирует строки и сохраняет в таблицу `reward_assignment`
- Минимальная обработка ошибок (валидация и структура ExceptionHandler — базовая)
- Пропускает строки, если:
  - сотрудник с `employee_id` не найден в `employee`
  - отсутствуют обязательные поля
  - дубль: `(employee_id, reward_id, awarded_at)`
- Возвращает отчёт:
  ```json
  {
    "processed": 10,
    "saved": 8,
    "skipped": 2,
    "errors": [{"row": 7, "message": "Employee 999 not found"}]
  }
Стек
Java 21, Spring Boot 3.5.x (MVC), Spring Web, Spring Data JPA, PostgreSQL (локально через Docker), HikariCP, Gradle 8.x, Apache POI (XLSX), Commons CSV (CSV), JUnit 5

Почему MVC, а не WebFlux?
Загрузка файлов и парсинг — блокирующие операции. Реактивность не даёт выигрыша, а библиотеки парсинга (POI/CSV) синхронные. MVC проще, надёжнее и быстрее в разработке/тестах для этого кейса.
