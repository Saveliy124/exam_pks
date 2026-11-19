# Проверки КР3

Дата: 19 ноября 2026 года.

JUnit: 76 проверок, ошибок и падений 0. Браузер: 11 сквозных сценариев, падений 0. Необработанных JavaScript-ошибок: 0.

Состав JUnit: 44 проверки общей библиотеки КР2, 16 чистых проверок КР3 и 16 интеграционных сценариев на PostgreSQL. API проверяет реальную цепочку фильтров безопасности, контроллеров, сервисов и JPA, включая одновременные изменения. Браузер работает через настоящий HTTP с backend в Docker.

- Login, mandatory password replacement, cookie security and refresh after reload: PASS
- User creation with viewer and operator roles: PASS
- Candidate, examiner and discipline create and edit: PASS
- Application create, update, combined filters and CSV download: PASS
- Pinned notes create, escaped HTML, edit and delete: PASS
- Bulk tagging, archive, restore and permanent deletion: PASS
- CSV preview, invalid rows, import, UPSERT and sample download: PASS
- Pagination, sorting, network failure and recovery: PASS
- Audit search, analytics, Swagger and responsive layout: PASS
- Viewer restrictions and operator permissions in real browser: PASS
- User reset and disable; profile password change: PASS

Проверены перенос полей КР1, миграции на существующей и пустой базе, health, обычный перезапуск с сохранением данных. Отчеты: qa/test-summary.json, qa/browser-results.json и qa/data-migration.json. Скриншоты 01–07 показывают тестовые данные.

Среда проверки: macOS ARM, JDK 21, Maven 3.9, PostgreSQL 17 в Docker, Chromium из Яндекс Браузера. Это функциональная проверка всех реализованных экранов и основных отрицательных сценариев, а не гарантия отсутствия любых возможных дефектов. Промышленная нагрузка, внешнее размещение с HTTPS, Windows и Android-клиент в этот прогон не входят.
