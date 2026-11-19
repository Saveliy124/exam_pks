# REST API экзаменационного центра

Базовый адрес: `http://127.0.0.1:8081`; демо — `http://127.0.0.1:8082`. Все данные JSON, даты ISO-8601 `2026-11-01T10:30:00`, статусы — enum-имена на английском. В интерфейсе они переведены на русский. Роли и авторизация пока не реализованы. В Docker HTTP опубликован только на localhost.

| Метод | Путь | Назначение |
| --- | --- | --- |
| GET | /actuator/health | UP при доступной PostgreSQL; 503 при недоступной |
| GET | /api/info | Версия, имя БД и часовой пояс без учётных данных |
| GET | /api/snapshot | Согласованные списки трёх сущностей |
| GET | /api/applicants | Кандидаты |
| GET | /api/applicants/{id} | Один кандидат |
| POST | /api/applicants | Создать кандидата |
| PUT | /api/applicants/{id} | Изменить кандидата |
| DELETE | /api/applicants/{id} | Удалить кандидата без заявок |
| GET | /api/examiners | Экзаменаторы |
| GET | /api/examiners/{id} | Один экзаменатор |
| POST | /api/examiners | Создать экзаменатора |
| PUT | /api/examiners/{id} | Изменить экзаменатора |
| DELETE | /api/examiners/{id} | Удалить экзаменатора |
| GET | /api/applications | Заявки; параметры search, status, from, to, applicantId |
| GET | /api/applications/{id} | Одна заявка |
| POST | /api/applications | Создать заявку NEW |
| PUT | /api/applications/{id} | Изменить детали NEW/APPROVED |
| DELETE | /api/applications/{id} | Удалить заявку |
| PATCH | /api/applications/{id}/status | Согласовать, отменить, завершить с баллом |
| GET | /api/statistics?from=2026-01-01&to=2026-12-31 | Показатели по дате экзамена; обе границы необязательны |

POST возвращает 201, `Location` и `{ "id": 123 }`; PUT/DELETE/PATCH — 204 без тела. GET — 200. Неизвестные поля и неправильные типы во входном JSON отвергаются. Прямые API-запросы проходят те же бизнес-проверки, что и работа из окна.

## Тела запросов

Кандидат: `{ "fullName": "Иван Петров", "email": "ivan@example.org", "phone": "", "status": "ACTIVE" }`. Статусы кандидата ACTIVE/BLOCKED.

Экзаменатор: `{ "fullName": "Анна Иванова", "email": "examiner@example.org", "subject": "Математика", "status": "ACTIVE" }`. Статусы ACTIVE/INACTIVE.

Заявка: `{ "applicantId": 1, "examName": "Физика", "scheduledAt": "2026-12-01T10:30:00" }`. Время должно быть строго в будущем. ID кандидата должен существовать, кандидат должен быть активен. Не передавайте id/status/score/createdAt при создании.

Согласование: `{ "status": "APPROVED", "score": null }`. Отмена: `{ "status": "CANCELLED", "score": null }`. Завершение после времени экзамена: `{ "status": "COMPLETED", "score": 87 }`.

## Ошибки

Формат: `{ "code": "BAD_REQUEST", "field": "email", "message": "Этот email уже используется." }`. `field` может быть пустым, если ошибка относится ко всей операции. 400 — валидация/тип/период, 404 — неизвестный адрес или ID, 405 — неподдерживаемый метод, 409 — конфликт уникальности/связи/устаревшей команды, 503 — недоступная БД, 500 — неожиданная серверная ошибка без stack trace в ответе.

При сетевой ошибке во время сохранения неизвестно, успел ли сервер записать данные. Клиент сообщает об этом: сначала обновите список, затем принимайте решение о повторе. Автоматического повторного POST/PUT/PATCH/DELETE нет.

## Безопасный просмотр

```sh
curl http://127.0.0.1:8081/actuator/health
curl http://127.0.0.1:8081/api/snapshot
curl 'http://127.0.0.1:8081/api/applications?status=APPROVED'
```

Для создания записей через API используйте демо-сервер и собственные новые email. Учебный Student Hub на 8080 использует другой контракт и другую БД.
