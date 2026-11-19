# Соответствие требованиям КР3

| Требование | Реализация | Проверка |
| --- | --- | --- |
| Тема КР1, прежняя БД как основа | Таблицы applicants, examiners, exam_applications; копирование данных, Flyway V1 | Сравнение 6/10/5 и исходных полей, миграции на копии |
| Пакеты backend | controller, service, repository, domain, dto, security, filter, error, config; db/migration | Структура web-backend |
| Spring Data JPA и validate | JPA-сущности, JpaRepository, Specification; ddl-auto=validate | Загрузка Spring Context на PostgreSQL |
| JWT и refresh | AuthService, JwtService, RefreshSession, ротация и отзыв | FullApiIT, браузерный вход и reload |
| Роли и безопасность | VIEWER, OPERATOR, ADMIN, BCrypt, смена временного пароля, блокировки | FullApiIT, BusinessRulesTest, браузер |
| Основная сущность | ExamApplication: UUID publicId, внешний код, кандидат, дисциплина, статус, балл, готовность, документы, теги, риск, даты, версия, архив | CRUD и фильтры API |
| CRUD и архив | /api/applications; version при изменениях; удаление только администратором | FullApiIT и браузер |
| Поиск и сравнения | Criteria Specification, LT/LTE/EQ/GTE/GT; все обязательные фильтры | API и браузер |
| Сортировка и страницы | PageableFactory, разрешенные поля, 1–100 записей | API и браузер |
| Справочник | /api/disciplines, создание/изменение/активность только ADMIN | API и браузер |
| Заметки | /api/applications/{id}/notes; автор, время, закрепление, версии | API и браузер |
| Массовые действия | /api/applications/bulk; архив/восстановление/статус/добавление/удаление тега | API и браузер |
| CSV | /api/applications/import/preview, /import/apply, /export; CREATE_ONLY/UPSERT | API и браузер |
| Аналитика | /api/analytics/summary; все количества и три группировки | API и браузер |
| Пользователи | /api/admin/users; страницы, создание, изменение, временный пароль и отключение | API и браузер |
| Аудит | AuditEvent, correlation ID, поиск, сортировка, пагинация, ADMIN | API и браузер |
| Валидация и ошибки | Bean Validation, отдельные DTO, единый ApiError, HTTP 400/401/403/404/409 | FullApiIT |
| PostgreSQL и Flyway | V1 исходная схема, V2 безопасность и workflow, V3 примеры только в пустой базе | Копия КР1 и чистая тестовая база |
| Docker обязателен | web-backend/docker-compose.yml, два сервиса, healthchecks, localhost, volume | Реальный запуск Docker |
| Swagger | /swagger-ui.html и /v3/api-docs, доступ ADMIN | API и браузер |
| Web frontend | web/index.html, api.js, app.js, styles.css; HTML/CSS/JS без сборщика | Браузерные сценарии и адаптивный экран |
| Отдельный JavaFX | common, desktop и прежний backend сохранены; порты 8081/8082 | Сборка Maven, работающая КР2 |

React необязателен по тексту задания: использовано альтернативное решение на стандартных модулях JavaScript. Архив преподавателя изучен и использован как основа структуры безопасности; доменные названия заменены понятиями экзаменационного центра. Его `.env`, готовые секреты, Android-клиент и скомпилированные файлы в сдаваемую КР3 не включены.
