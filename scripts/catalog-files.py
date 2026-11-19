#!/usr/bin/env python3
"""Build a complete catalog of delivered files, with purpose and explicit relationships."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
ROLES={
'ApiContracts.java':('Определяет JSON-контракты: отдельные входные данные кандидата, экзаменатора, заявки и смены статуса; ID созданной записи, ошибку API и статистику. Поля id и createdAt нельзя подставить через запрос создания.','ApiController принимает DTO; ApiClient и три клиентских адаптера отправляют/читают их; ExamCenterFacade выполняет операции.'),
'Applicant.java':('Объект кандидата с ID, именем, email, телефоном и статусом. Геттеры нужны таблицам и JSON; пустой конструктор и сеттеры позволяют Jackson восстановить объект из ответа.','ApplicantRepository создаёт из SQL; ApplicantService проверяет изменения; ApplicantsController и ApplicantDetailsController отображают.'),
'Examiner.java':('Объект экзаменатора: ID, ФИО, email, предмет и активность. Представляет отдельный справочник; связи назначения на заявку в исходной схеме нет.','ExaminerRepository, ExaminerService, ExaminersController и PersonFormController.'),
'ExamApplication.java':('Заявка с ID кандидата, его отображаемыми именем/email, экзаменом, временем, статусом, необязательным баллом и датой создания. Имя/email в запросе чтения получаются через JOIN.','ExamApplicationRepository читает; ExamApplicationService меняет; ApplicationClient передаёт; ApplicationsController показывает.'),
'ApplicantStatus.java':('Разрешённые состояния кандидата ACTIVE и BLOCKED. Перечисление исключает случайные значения в Java, Jackson и формах.','Applicant, Rules, Filters, ApplicantService; Labels переводит подписи.'),
'ExaminerStatus.java':('Разрешённые состояния экзаменатора ACTIVE и INACTIVE. Используются в форме, поиске и общем счётчике активных сотрудников.','Examiner, ExaminerService, StatisticsService, Labels.'),
'ApplicationStatus.java':('Четыре состояния заявки NEW, APPROVED, COMPLETED, CANCELLED. Они образуют управляемый жизненный цикл.','Rules задаёт переходы; StatusFormController запрашивает действие; SQL хранит имя enum.'),
'CenterSnapshot.java':('Согласованный набор трёх списков. Копирует списки в неизменяемые коллекции, чтобы один результат загрузки не менялся самопроизвольно.','CenterRepository создаёт; API сериализует; AppContext раскладывает в ObservableList; StatisticsService считает показатели.'),
'Rules.java':('Общие чистые проверки: длины и пробелы строк, email, обязательность поля, будущая дата, разрешённые переходы и балл. Не обращается к БД и не зависит от JavaFX.','Формы быстро проверяют ввод; предметные сервисы повторяют правила на сервере; RulesTest проверяет границы.'),
'ValidationException.java':('Ошибка понятного пользователю правила с ключом поля. Позволяет сообщить, что исправить, и подсветить именно это поле.','Rules и сервисы бросают; ApiExceptionHandler превращает в JSON; ApiClient восстанавливает для BaseFormController.'),
'Filters.java':('Функции поиска по нескольким полям и совместных фильтров по статусу, периоду, кандидату, домену и предмету. Проверяет правильность диапазона дат.','FX-контроллеры фильтруют снимок в памяти; ApiController фильтрует GET заявок; StatisticsService отбирает период.'),
'Labels.java':('Русские названия enum-статусов. Отделяет хранимые английские коды от подписей для человека.','Cells, формы, ExportService и таблицы используют подписи.'),
'StatisticsService.java':('Чистый расчёт показателей по снимку и периоду: статусы, средний/минимум/максимум, предстоящие экзамены, доля завершённых и справочники. Clock позволяет контролировать время в тестах.','BackendConfiguration создаёт; ExamCenterFacade вызывает и преобразует Optional в nullable JSON-поля; FiltersStatisticsTest проверяет.'),
'ExportService.java':('Превращает модели в таблицу бизнес-значений с русскими заголовками. Сохраняет порядок переданного списка, не добавляет технические ID.','BaseTableController передаёт отсортированные строки; ExportTable хранит снимок; XlsxExporter пишет файл.'),
'ExportTable.java':('Неизменяемый снимок одной экспортируемой таблицы: имя листа, заголовки и строки значений. Не зависит от JavaFX.','ExportService формирует; XlsxExporter читает в фоновой задаче.'),
'DataExporter.java':('Интерфейс экспорта: одна таблица текущего раздела или три списка для старой консоли. Отделяет формат файла от вызывающего интерфейса.','XlsxExporter реализует; AppContext хранит реализацию; ConsoleUi использует старый трёхлистовый вариант.'),
'XlsxExporter.java':('Создаёт XLSX через Apache POI. Записывает даты и числа с правильными типами, заголовки и автофильтр, сначала сохраняет временный файл и затем заменяет целевой.','DataExporter, ExportTable, ExportTest; GUI выбирает путь через Dialogs.'),
'BackendApplication.java':('Точка входа нашего Spring Boot. Запускает встроенный Tomcat, загружает application.yml, создаёт компоненты Spring и HTTP-маршруты.','BackendConfiguration, ApiController, ApiExceptionHandler; запускается executable JAR из Dockerfile.'),
'BackendConfiguration.java':('Собирает серверные зависимости: Clock, три предметных сервиса, сервис снимка и расчёта статистики. Передаёт репозиториям транзакционную обёртку DataSource.','Spring/Hikari создаёт DataSource; TransactionAwareDataSourceProxy связывает JDBC с @Transactional; ExamCenterFacade получает сервисы.'),
'ExamCenterFacade.java':('Единый серверный слой операций REST. Задаёт транзакции с откатом и для checked-исключений, проверяет наличие ID, делегирует предметные операции и выдаёт статистику.','ApiController вызывает; ApplicantService/ExaminerService/ExamApplicationService сохраняют; CenterService делает снимок.'),
'ApiController.java':('Объявляет HTTP-маршруты для трёх сущностей, смены статуса, снимка, статистики и информации о сервере. Возвращает 201 с ID и Location при создании, 204 после изменения/удаления.','ExamCenterFacade выполняет правила; ApiContracts описывает JSON; ApiExceptionHandler перехватывает ошибки.'),
'ApiExceptionHandler.java':('Единый перевод исключений в HTTP-коды и JSON code/field/message. Обрабатывает ввод, неизвестные ID, конфликты SQL, недоступную БД и неожиданные ошибки.','Ошибки сервисов и JDBC; ApiClient читает JSON; технический журнал сервера сохраняет диагностику.'),
'NotFoundException.java':('Обозначает запрос к отсутствующей записи. Отличает отсутствие объекта от неверно заполненной формы.','ExamCenterFacade бросает; ApiExceptionHandler возвращает HTTP 404.'),
'HealthProbe.java':('Небольшая отдельная Java-программа без Spring-зависимостей. Проверяет HTTP health и завершает процесс с кодом 0 или 1 для Docker healthcheck.','Dockerfile копирует class отдельно; compose.yaml запускает; /actuator/health проверяет также PostgreSQL.'),
'ApplicantService.java':('Бизнес-операции кандидатов: нормализация полей/email, уникальность, получение и CRUD, запрет удаления при наличии заявок. Содержит также функции старой консоли.','ApplicantRepository, Rules; Spring-сервер вызывает через ExamCenterFacade.'),
'ExaminerService.java':('Правила и CRUD независимого справочника экзаменаторов: имя/email/предмет, статус и уникальность. Сохраняет вспомогательные поиски консоли.','ExaminerRepository, Rules, ExamCenterFacade.'),
'ExamApplicationService.java':('Проверяет активность и наличие кандидата, будущее время, отсутствие активного дубля, допустимость изменения и перехода статуса. Балл принимает только при корректном завершении.','ApplicantService, Rules, Clock, ExamApplicationRepository; вызывается серверной фасадой.'),
'CenterService.java':('Небольшой сервис загрузки полного снимка. Отделяет запрос данных от способа их отображения или расчёта.','CenterRepository читает; ExamCenterFacade.snapshot и statistics используют результат.'),
'ApplicantRepository.java':('Параметризованные SQL для кандидатов: INSERT/SELECT/UPDATE/DELETE, поиск email без регистра и проверка связанных заявок. В сервере получает соединение из Spring DataSource.','ApplicantService; applicants и exam_applications; RepositoryChecks контролирует число изменённых строк.'),
'ExaminerRepository.java':('SQL справочника экзаменаторов и проверка уникальности email. Преобразует ResultSet в Examiner, закрывает JDBC-ресурсы.','ExaminerService; таблица examiners; DataSource и RepositoryChecks.'),
'ExamApplicationRepository.java':('SQL заявок, JOIN с кандидатом, проверка дубликата, вставка/изменение, смена статуса с проверкой исходного состояния. Различает SQL NULL и реальный нулевой балл.','ExamApplicationService; exam_applications и applicants; RepositoryChecks.'),
'CenterRepository.java':('Читает три таблицы в одной read-only REPEATABLE READ транзакции. Возвращает целый согласованный снимок или откатывает неудавшееся чтение.','CenterService; три репозитория используют переданное Connection; CenterSnapshot.'),
'Database.java':('Локальное JDBC-подключение для серверной диагностической консоли и самостоятельных тестов репозиториев. Читает свойства/окружение/локальный файл, задаёт тайм-ауты. В HTTP-сервере вместо него используется Spring DataSource.','Main/ConsoleUi, TestDatabase, no-arg конструкторы репозиториев; JavaFX не содержит этот класс.'),
'RepositoryChecks.java':('Требует, чтобы изменение затронуло ровно одну строку. Иначе сообщает об устаревшей или исчезнувшей записи через SQLState 40001.','Три репозитория; ApiExceptionHandler переводит в HTTP 409.'),
'Main.java':('Сохранённая точка входа диагностической консоли КР1. Не запускает HTTP-сервер и не является обычным способом работы пользователя.','ConsoleUi и прежние сервисы; scripts/run-console.sh.'),
'ConsoleUi.java':('Старое текстовое меню для сравнения с КР1 и диагностики. Расположено на серверной стороне и обращается к тем же правилам через JDBC.','Main, три сервиса, XlsxExporter; основной GUI использует REST независимо от консоли.'),
'ApiClient.java':('Транспорт JavaFX: Java HttpClient, адрес backend, тайм-ауты, Jackson и JSON. Восстанавливает модели, ошибки поля и сетевые сообщения; не повторяет сохранение автоматически.','ApiContracts, ApiException, ValidationException; все три HTTP-адаптера и AppContext.'),
'ApiException.java':('Клиентская ошибка сервера или сети с HTTP-кодом. Код 0 означает, что HTTP-ответ не получен или адрес некорректен.','ApiClient создаёт; Errors переводит в текст для формы/диалога.'),
'ApplicantClient.java':('HTTP-операции кандидатов с именами методов, привычными существующим формам. Собирает ApplicantInput и вызывает POST/PUT/DELETE.','PersonFormController, ApplicantsController → ApiClient → /api/applicants.'),
'ExaminerClient.java':('HTTP-операции справочника экзаменаторов. Передаёт форму через JSON и читает ID новой записи.','PersonFormController, ExaminersController → ApiClient → /api/examiners.'),
'ApplicationClient.java':('HTTP-создание, изменение, удаление и смена статуса заявки. Передаёт ID кандидата, время и балл через явные DTO.','ApplicationFormController, ApplicationsController, StatusFormController → ApiClient.'),
'ExamCenterApplication.java':('Жизненный цикл JavaFX: локаль, AppContext, главное FXML, Scene/Stage, CSS, размеры окна и завершение фоновых задач.','run.sh запускает; AppContext загружает main-view.fxml и запрашивает snapshot.'),
'AppContext.java':('Общий контекст клиента: HTTP-адаптеры, ObservableList, состояние загрузки, фоновые задачи, навигация и диалоги. После сохранения получает свежий снимок; повторное чтение не теряется.','ApiClient, AsyncRunner, FXMLLoader и все FX-контроллеры; репозиториев и JDBC в нём нет.'),
'AsyncRunner.java':('Запускает медленные HTTP-запросы и экспорт в ограниченном пуле из трёх фоновых потоков через JavaFX Task. Результат и ошибку возвращает на поток интерфейса.','AppContext и контроллеры; счётчик занятости управляет индикатором и доступностью действий.'),
'Errors.java':('Понятные сообщения клиента: ошибка поля, HTTP/сеть, запись файла и непредвиденная проблема. Детали БД обрабатываются только на сервере.','ApiException, ValidationException, Dialogs и формы.'),
'Dialogs.java':('Системный FileChooser для XLSX, подтверждение удаления и окно ошибки. В тесте заменяется, чтобы не зависеть от ручного выбора файла.','BaseTableController, BaseFormController, Errors; FxScenarioIT.TestDialogs.'),
'Cells.java':('Настраивает отображение колонок, дат, русских статусов и календарей. Хранит общие форматтеры, чтобы окна показывали данные одинаково.','Labels, таблицы трёх разделов, формы и карточки.'),
'MainController.java':('Навигация четырёх разделов, общая строка состояния и индикатор, повторное использование уже открытых экранов, переходы к связанному кандидату.','main-view.fxml, AppContext.Navigation и контроллеры разделов; показывает подпись только для demo-режима.'),
'BaseTableController.java':('Общая механика таблиц: выбор строки, FilteredList/SortedList, доступность действий, число записей, экспорт текущей выборки и подтверждение удаления.','Три контроллера разделов, AppContext, Dialogs, ExportService и XlsxExporter.'),
'ApplicationsController.java':('Колонки заявок, поиск/статус/период/кандидат, доступность переходов, открытие формы/карточки, удаление и связанная навигация.','applications-view.fxml, Filters, Rules, ApplicationClient, ApplicationFormController и StatusFormController.'),
'ApplicantsController.java':('Колонки и действия кандидатов, поиск по имени/email/телефону, статус и домен, открытие карточки и формы.','applicants-view.fxml, BaseTableController, PersonFormController, ApplicantDetailsController и ApplicantClient.'),
'ExaminersController.java':('Колонки и действия экзаменаторов, поиск, фильтр статуса и предмета. Независимый CRUD справочника.','examiners-view.fxml, BaseTableController, PersonFormController и ExaminerClient.'),
'BaseFormController.java':('Общие состояния формы, сопоставление ключей полей и элементов, показ валидации, блокировка закрытия во время отправки и завершение сохранения.','AppContext.save, Errors, ValidationException; остальные контроллеры форм наследуют.'),
'PersonFormController.java':('Одна переиспользуемая форма кандидата и экзаменатора: общие ФИО/email и различающееся поле телефон/предмет, режим просмотра и сохранение.','person-form.fxml, Rules, ApplicantClient и ExaminerClient.'),
'ApplicationFormController.java':('Форма заявки с ComboBox кандидатов, названием, календарём и часами/минутами. Проверяет быстрые ошибки, передаёт сохранение серверу и сохраняет секунды старой даты, если пользователь её не менял.','application-form.fxml, AppContext.applicants, Rules и ApplicationClient.'),
'StatusFormController.java':('Диалог согласования, отмены и результата. Подсвечивает неверный балл/время и вызывает отдельный HTTP-переход статуса.','status-form.fxml, Rules.transition, ApplicationClient.changeStatus.'),
'ApplicantDetailsController.java':('Карточка кандидата с таблицей его заявок и переходами к фильтрованному списку/новой заявке. Блокирует новую заявку для заблокированного кандидата.','applicant-details.fxml, AppContext.snapshot/Navigation, модели кандидата и заявки.'),
'StatisticsController.java':('Запрашивает показатели у Spring API при смене периода или снимка. Показывает числа и диаграмму; номер запроса защищает от позднего ответа для старого периода.','statistics-view.fxml, ApiClient.statistics, Filters.period, AsyncRunner и ApiContracts.Statistics.'),
'RulesTest.java':('Проверяет строки/email, обязательность, границы дат, статусы и диапазон баллов на фиксированном Clock без запуска БД.','Rules, ValidationException и enum-статусы.'),
'FiltersStatisticsTest.java':('Проверяет совместные фильтры, включённые границы периода, точный домен и корректную статистику, включая пустой набор и нулевой балл.','Filters, StatisticsService и модели общего модуля.'),
'ExportTest.java':('Читает созданный XLSX и проверяет порядок, заголовки, native даты/числа, отсутствие результата, пустой набор и безопасный текст ячеек.','ExportService, XlsxExporter, Apache POI; временная папка JUnit.'),
'DatabaseIT.java':('Проверки на настоящем PostgreSQL: CRUD трёх сущностей, правила заявок, уникальные индексы, устаревший переход и параметризованный SQL.','TestDatabase, репозитории и предметные сервисы; только *_test.'),
'RestApiIT.java':('Отправляет настоящие HTTP-запросы в Spring: CRUD, обход GUI-валидации, плохой JSON, коды ошибок, статистика, конкурентный дубль и транзакционный откат.','TestServer, ApiClient, HttpClient, PostgreSQL; не подменяет репозитории mock-объектами.'),
'UnavailableDatabaseIT.java':('Запускает отдельный Spring с недоступной тестовой точкой БД. Проверяет HTTP 503 health/API и отсутствие паролей/stack trace в ответе.','BackendApplication, ApiExceptionHandler и Hikari; рабочую БД не останавливает.'),
'FxScenarioIT.java':('Сквозной пользовательский сценарий: реальные FXML/JavaFX, REST, сервер и тестовая PostgreSQL. Проверяет формы, CRUD, карточки, статусы, фильтры, Excel и восстановление после сетевой ошибки, сохраняет снимки.','TestServer, TestDatabase, AppContext, TestDialogs; в Docker-прогоне сервер находится в отдельном контейнере.'),
'TestServer.java':('Поднимает Spring на случайном HTTP-порту только с явно выбранной *_test БД. Для Docker-UI проверяет /api/info и совпадение тестовой БД перед любыми действиями.','RestApiIT и FxScenarioIT; внешний адрес задаёт только scripts/test-docker-ui.sh.'),
'TestDatabase.java':('Подготавливает схему/демо-фикстуру, очищает только явно переданную БД с суффиксом _test и дополнительно проверяет current_database().','Все интеграционные сценарии; Database и sql/schema.sql/demo.sql.'),
'IntegrationSmoke.java':('Унаследованная самостоятельная проверка консольных сервисов и трёхлистового экспорта. Сохранена для сравнения с КР1; не входит в стандартное число JUnit-тестов.','Предметные сервисы, репозитории, DataExporter; не запускается обычным GUI.'),
}
FXML={
'main-view.fxml':('Главный BorderPane, боковая навигация, область текущего экрана и строка статуса.','MainController'),
'applications-view.fxml':('Таблица заявок, поисковые поля/календарь периода, действия со статусом, карточками и экспортом.','ApplicationsController'),
'applicants-view.fxml':('Список кандидатов, фильтры статуса/домена и CRUD/экспорт.','ApplicantsController'),
'examiners-view.fxml':('Список экзаменаторов, фильтры статуса/предмета и CRUD/экспорт.','ExaminersController'),
'statistics-view.fxml':('Период, подписи показателей, столбчатая диаграмма и сообщение об ошибке.','StatisticsController'),
'person-form.fxml':('Переиспользуемая форма кандидата/экзаменатора с общими полями и ошибками.','PersonFormController'),
'application-form.fxml':('Выбор кандидата, экзамена и даты/времени, данные карточки и кнопки формы.','ApplicationFormController'),
'status-form.fxml':('Отдельное окно подтверждения статуса или ввода балла.','StatusFormController'),
'applicant-details.fxml':('Карточка кандидата с таблицей связанных заявок и переходами.','ApplicantDetailsController'),
}
EXACT={
'pom.xml':('Корневой Maven-агрегатор, версия Java 21, Spring Boot parent, общие тестовые зависимости и порядок модулей.','common/pom.xml, desktop/pom.xml, backend/pom.xml'),
'common/pom.xml':('Собирает общую библиотеку exam-common и подключает Apache POI для Excel.','Корневой pom; используется backend и desktop.'),
'desktop/pom.xml':('Зависимости GUI/HTTP/JSON и javafx-maven-plugin. Не содержит PostgreSQL-драйвер и серверных runtime-зависимостей.','common/pom.xml; run.sh и run.ps1.'),
'backend/pom.xml':('Spring Web/JDBC/Actuator/PostgreSQL, executable JAR и профили интеграционных/UI-тестов. Desktop подключён только в test-scope.','Корневой pom, Dockerfile, scripts/test*.sh.'),
'backend/Dockerfile':('Собирает runtime-образ Java 21 с executable JAR и отдельным HealthProbe. Запускает Java под непривилегированным UID 10001; явно задаёт права читаемости файлов и Europe/Moscow.','backend/target, compose.yaml.'),
'compose.yaml':('Объявляет PostgreSQL, наш backend и необязательный backend-demo, порты, healthcheck, сеть и постоянный volume. Сохраняет прежнее имя проекта, чтобы не потерять данные КР1.','.env, backend/Dockerfile, docker/init, setup/demo scripts.'),
'.dockerignore':('Ограничивает контекст сборки JAR и class HealthProbe. Локальная .env, база, Git и архив преподавателя не отправляются в Docker build.','Dockerfile и scripts/setup.sh.'),
'.gitignore':('Исключает секреты, config, сборки всех модулей, резервные копии, IDE-кэш и внешний Student Hub из Git.','Git и структура всего проекта.'),
'.env.example':('Шаблон имён БД, пользователя, локального пароля-заглушки и портов HTTP/БД. Это не действующие учётные данные.','setup.sh создаёт реальную .env со случайным паролем; Compose читает её.'),
'run.sh':('Выбирает JDK 21, собирает common/desktop, читает только HTTP-настройку клиента и запускает JavaFX из desktop/pom.xml. Не загружает пароль БД.','java21.sh, config/client.properties, EXAM_API_URL, ExamCenterApplication.'),
'run.ps1':('PowerShell-запуск клиента с JDK 21 и Maven, чтением HTTP-адреса и сборкой нужных модулей.','desktop/pom.xml, scripts/setup.ps1, JAVA_HOME.'),
'Запустить КР2.command':('Сохраняет знакомый запуск двойным щелчком на Mac: сначала подготовка серверов, затем GUI. Название оставлено для удобства текущего проекта.','scripts/setup.sh → run.sh.'),
'Демонстрация КР2.command':('Запуск тренировочного окружения двойным щелчком после общей подготовки.','scripts/setup.sh → scripts/demo.sh.'),
'scripts/java21.sh':('Выбирает JDK 21 локально для команды и проверяет наличие Maven, не меняя глобальные настройки пользователя.','Все shell-сценарии Java/Maven.'),
'scripts/setup.sh':('Создаёт локальный пароль при первом запуске, поднимает PostgreSQL, применяет схему/seed только для пустой БД, собирает модули и запускает backend с ожиданием health.','compose.yaml, .env, sql, Maven, config/client.properties.'),
'scripts/setup.ps1':('Аналог подготовки для Windows: локальная конфигурация, база, Maven и Docker backend. В этой сессии Windows-фактическая проверка не проводилась.','compose.yaml, SQL, run.ps1.'),
'scripts/demo.sh':('Создаёт отдельную БД *_demo только при отсутствии, применяет схему/демо-данные, поднимает backend-demo и направляет GUI на порт 8082. Перед запуском клиента удаляет пароль БД из окружения.','sql/demo.sql, compose.yaml, run.sh.'),
'scripts/backend-local.sh':('Запускает executable Spring JAR на компьютере разработчика, обычно на 8083, с подключением к опубликованному PostgreSQL.','backend/pom.xml, .env, EXAM_DB_URL; удобен для просмотра восстановленной копии.'),
'scripts/test.sh':('Создаёт отдельную *_test БД, запускает Maven integration: чистые тесты, JDBC и REST/недоступность БД.','TestDatabase, DatabaseIT, RestApiIT, UnavailableDatabaseIT.'),
'scripts/test-ui.sh':('Запускает UI-профиль Maven с открытием настоящих окон и тестовым Spring HTTP-сервером.','FxScenarioIT, TestServer, TestDatabase.'),
'scripts/test-docker-ui.sh':('Собирает серверный образ, запускает временный backend на *_test БД и случайном localhost-порту, выполняет UI-сценарий через Docker и удаляет только свой контейнер.','Dockerfile, TestServer /api/info, FxScenarioIT; журнал backend/target/docker-ui-backend.log.'),
'scripts/backup.sh':('Создаёт pg_dump рабочей БД в формате custom и удаляет незавершённый файл при ошибке.','compose postgres и backups/; пароль берётся из локальной .env.'),
'scripts/restore-copy.sh':('Восстанавливает dump только в новую БД с суффиксом _restore. Отказывается менять уже существующую БД и печатает команды запуска отдельного backend/клиента.','backup.sh, backend-local.sh, EXAM_DB_URL, EXAM_API_URL.'),
'scripts/run-console.sh':('Запускает сохранённую диагностическую консоль из backend-модуля через Maven exec. Это дополнительная утилита разработчика с прямым JDBC.','Main, ConsoleUi, .env и предметные сервисы.'),
'scripts/package-source.py':('Собирает переносимый ZIP собственных модулей, SQL, скриптов и документации. Исключает target, секреты и внешний проект преподавателя; пишет новый архив и совместимый старый alias.','dist/exam-center-spring-source.zip и dist/exam-center-kr2-source.zip.'),
'scripts/verify-release.py':('Распаковывает текущий ZIP в новую папку с пробелами, создаёт отдельный Compose-проект, проверяет запуск/повторную подготовку и API-тесты, сверяет хеш кода и удаляет только собственный проверочный volume.','package-source.py и docs/qa/release-check.json; рабочая БД не используется.'),
'scripts/catalog-files.py':('Генерирует полный каталог каждого передаваемого файла с назначением и явными связями. Отказывает при неизвестном исходном Java/FXML-файле.','docs/file-catalog.json и scripts/build-guide.py.'),
'scripts/build-guide.py':('Собирает Word-руководство с использованием python-docx из актуальных пользовательских/технических документов и каталога файлов. Проверка страниц выполняется отдельно.','docs/user-guide.md, architecture.md, api.md, file-catalog.json, testing.md, defense.md.'),
'scripts/sample-prepare.sh':('Готовит локальную конфигурацию исходного Student Hub отдельно от нашей системы.','student-hub-suite-rebuilt и docker/sample-override.yaml.'),
'scripts/sample-start.sh':('Поднимает PostgreSQL и Spring Boot готового учебного примера и проверяет его health.','sample-prepare.sh, Student Hub, порт 8080; не наш backend 8081.'),
'scripts/sample-client.sh':('Запускает исходный JavaFX-клиент преподавателя для изучения примера.','student-hub-suite-rebuilt/javafx-client и JDK 21.'),
'scripts/sample-stop.sh':('Останавливает только систему преподавателя, сохраняя её volume.','Student Hub Compose, sample-override.yaml.'),
'sql/schema.sql':('Создаёт три таблицы, миграцию статуса кандидата, внешние ключи, CHECK и уникальные индексы email/активного экзамена. Повторное применение не очищает данные.','setup scripts, TestDatabase, PostgreSQL.'),
'sql/seed.sql':('Начальные учебные данные новой пустой рабочей БД. Не применяется повторно к заполненным таблицам.','scripts/setup.sh/setup.ps1 и schema.sql.'),
'sql/demo.sql':('Согласованные демонстрационные кандидаты, все статусы и результаты, включая ноль и прошедшую согласованную заявку. Даты относительны моменту первоначального заполнения.','scripts/demo.sh и TestDatabase.fixture.'),
'backend/src/main/resources/application.yml':('HTTP-порт, внешняя конфигурация Spring DataSource/Hikari, строгие JSON-типы и Actuator health. Пароля в исходнике нет.','BackendApplication, Compose environment и ApiClient.'),
'backend/src/main/resources/database.properties':('Безопасные JDBC-значения по умолчанию без пароля только для старой диагностики/тестов. Основной сервер использует Spring application.yml.','Database.java и локальный config/database.properties.'),
'desktop/src/main/resources/simplelogger.properties':('Формат технического журнала обычного JavaFX-клиента.','SLF4J simple и Errors.'),
'common/src/main/resources/log4j2.component.properties':('Техническая настройка логирования библиотек Apache POI/Log4j.','Apache POI и XlsxExporter.'),
'desktop/src/main/resources/ru/mirea/examcenter/fx/app.css':('Цвета, отступы, размеры и состояния таблиц/форм/кнопок, включая ошибки. Вся визуальная тема приложения.','AppContext.style и FXML styleClass.'),
}
DOCS={
'README.md':'Главная точка входа: актуальная архитектура, быстрый запуск, порты, команды тестов/сборки и ссылки на остальные документы.',
'docs/architecture.md':'Техническое объяснение трёх модулей, транзакций, HTTP, потоков, снимков и границ версии.',
'docs/api.md':'Полная справка REST: методы, адреса, тела, статусы, ошибки и безопасный просмотр curl.',
'docs/user-guide.md':'Пошаговое использование всех функций, формы/ограничения, сортировка/Excel, ошибки и восстановление.',
'docs/requirements-map.md':'Явно фиксирует переход к собственному backend и согласованную замену требования прямого JDBC из JavaFX.',
'docs/defense.md':'Сценарий показа и темы подготовки к защите версии Spring Boot.',
'docs/testing.md':'Фактический отчёт текущих проверок, окружение, результаты и непроверенные платформы.',
'docs/er-diagram.md':'Текстовая Mermaid-модель трёх таблиц КР1 и связи кандидата с заявками.',
'docs/er-diagram.svg':'Готовое изображение той же схемы для просмотра и защиты.',
'docs/instructor-example.md':'Отдельная инструкция и исторические доказательства развёртывания Student Hub преподавателя.',
'docs/kr1-baseline.md':'Историческая фиксация состояния и резервных копий КР1 до изменений.',
'docs/kr1-archive.md':'Старый README КР1. Используется для истории, его команды не являются инструкцией текущего запуска.',
'docs/file-catalog.json':'Машиночитаемый каталог назначения каждого файла; используется для полноты Word-руководства.',
'docs/Как работает приложение.docx':'Основной Word-гайд текущей версии: функции, архитектура, API, файлы, проверки и защита.',
}
QA={
'01-applications.png':'главный список заявок', '02-small-window.png':'минимальный размер окна', '03-applicants.png':'раздел кандидатов', '04-validation.png':'ошибка формы', '05-applicant-details.png':'карточка кандидата и связанные заявки', '06-application-form.png':'форма новой заявки', '07-result-form.png':'ввод результата', '08-filters.png':'совместные фильтры и порядок строк', '09-examiners.png':'справочник экзаменаторов', '10-examiner-card.png':'карточка экзаменатора', '11-statistics.png':'серверная статистика', '12-delete-confirmation.png':'настоящее подтверждение удаления', 'filtered.xlsx':'XLSX, сопоставленный с текущей выборкой UI', 'test-summary.json':'машиночитаемые результаты текущих JUnit-проверок', 'release-check.json':'проверки текущего архива и чистого развёртывания', 'kr1-data-verification.json':'историческая построчная сверка данных при переносе КР1', 'spring-migration-data.json':'сверка рабочей БД с резервной копией до Spring-перехода', 'runtime-check.json':'состояние и адреса наших Docker-сервисов после проверки', 'client-dependencies.txt':'runtime-дерево зависимостей клиента без PostgreSQL-драйвера и Spring-сервера',
}
files=[]
for top in ['common','desktop','backend','sql','scripts','docker','docs']:
 for p in (ROOT/top).rglob('*'):
  if p.is_file() and not any(x in {'target','__pycache__','.DS_Store'} for x in p.parts):files.append(p)
files += [ROOT/p for p in ['pom.xml','compose.yaml','.dockerignore','.gitignore','.env.example','README.md','run.sh','run.ps1','Запустить КР2.command','Демонстрация КР2.command']]
# Planned generated outputs are included even before the first build of the manual.
files += [ROOT/'docs/file-catalog.json']
result=[]
for p in sorted(set(files)):
 path=p.relative_to(ROOT).as_posix();name=p.name
 if path in EXACT: role,links=EXACT[path]
 elif p.suffix=='.java':
  if name not in ROLES:raise ValueError('Unknown Java file: '+path)
  role,links=ROLES[name]
 elif p.suffix=='.fxml':
  role,controller=FXML[name];links=controller+' через fx:controller, fx:id и onAction; AppContext.load; app.css.'
 elif path in DOCS:role,links=DOCS[path],'README.md и полное руководство Word.'
 elif path.startswith('docs/qa/'):
  if name in QA: role='Доказательство проверки: '+QA[name]+'.'
  elif name.startswith('instructor-'):role='Историческое доказательство проверки отдельного примера преподавателя Student Hub.'
  else:raise ValueError('Unknown QA file: '+path)
  links='docs/testing.md; соответствующий тест или docs/instructor-example.md.'
 elif path.startswith('docker/init/'):
  role='Создаёт дополнительную *_test БД при первоначальном развёртывании пустого PostgreSQL volume.';links='Compose postgres и test scripts; повторные сценарии также создают тестовую БД при необходимости.'
 elif path=='docker/sample-override.yaml':role='Локальные исправления портов/Compose готового примера Student Hub без перезаписи его исходников.';links='scripts/sample-*.sh и docs/instructor-example.md.'
 else:raise ValueError('Undocumented file: '+path)
 result.append({'path':path,'purpose':role,'links':links})
(ROOT/'docs/file-catalog.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print('Catalog:',len(result),'files')
