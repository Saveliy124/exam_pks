#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Build the KR3 file catalog and Word manual from the maintained documentation."""
from pathlib import Path
import json,re,xml.etree.ElementTree as ET
from docx import Document
from docx.shared import Inches,Pt,RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
root=Path(__file__).resolve().parents[1]
dest=root/'docs/kr3';dest.mkdir(exist_ok=True)
descriptions={
'ExamCenterWebApplication.java':'Запускает Spring Boot и регистрирует типизированные параметры AppProperties. Встроенная тестовая учетная запись Spring отключена; вход работает только через собственный AuthService.',
'AppProperties.java':'Описывает JWT, начального администратора, CORS, cookies, окончательное удаление и лимиты CSV. Spring собирает значения из application.yml и окружения.',
'BootstrapAdmin.java':'При отсутствии пользователей создает администратора с BCrypt-хешем и обязательной сменой пароля. Секрет читает из окружения и не печатает.',
'SecurityConfig.java':'Настраивает stateless SecurityFilterChain, права маршрутов, CORS, JSON 401/403 и защитные HTTP-заголовки. Подключает JWT и проверку временного пароля.',
'FilterConfiguration.java':'Отключает повторную регистрацию security-фильтров в servlet-контейнере: они должны срабатывать один раз внутри цепочки Spring Security.',
'OpenApiConfig.java':'Задает название и схему Bearer JWT в OpenAPI. Swagger используется для изучения REST-контрактов и демонстрации на защите.',
'ClockConfiguration.java':'Создает Clock с московским часовым поясом для проверки будущей даты и момента завершения экзамена.',
'HealthProbe.java':'Маленькая Java-программа без зависимостей Spring проверяет HTTP health. Docker вызывает ее для определения готовности сервера.',
'AuthController.java':'Предоставляет login, refresh, logout, me и смену пароля. Web refresh передает в HttpOnly cookie, native refresh возвращает отдельным полем JSON.',
'ApplicationController.java':'REST заявок, страницы, фильтры, карточка, CRUD, архив, восстановление, массовые операции и CSV. Контроллер принимает DTO и проверяет роли.',
'DirectoryController.java':'REST кандидатов, экзаменаторов и дисциплин. Управление дисциплинами и экзаменаторами ограничено администратором.',
'NoteController.java':'REST заметок конкретной заявки. Список доступен для чтения, запись — оператору и администратору.',
'AnalyticsController.java':'Возвращает аналитическую сводку сервиса AnalyticsService.',
'AdminUserController.java':'REST управления пользователями с пагинацией, созданием, изменением и сбросом временного пароля. Доступ только ADMIN.',
'AuditController.java':'Закрытый для обычных сотрудников REST поиска и постраничного просмотра журнала аудита.',
'AuthService.java':'Проверяет вход, выдает access JWT и серверную refresh-сессию, вращает refresh, отзывает сессии, меняет пароль. Повторное использование refresh отзывает активные сессии.',
'UserAdminService.java':'Создает и изменяет учетные записи, хеширует временные пароли, проверяет версии и запрещает администратору отключить самого себя.',
'LoginAttemptService.java':'Сохраняет неудачные попытки входа и блокировку в отдельной транзакции с блокировкой строки пользователя. Счетчик не теряется при отказе входа.',
'ApplicationService.java':'Основная бизнес-логика заявок. Проверяет версии, кандидата, дисциплину, даты и статусы, пересчитывает риск. Массовые действия выполняет независимыми транзакциями.',
'DirectoryService.java':'Проверяет и сохраняет кандидатов, экзаменаторов и дисциплины, нормализует email, сверяет версии и создает события аудита.',
'ExamRules.java':'Чистые правила допустимых переходов статусов, будущей даты и результата 0–100 после экзамена. Clock позволяет проверять границы времени.',
'RiskCalculator.java':'Вычисляет риск по статусу, готовности и документам. Используется обычным изменением заявки и массовой сменой статуса.',
'ExamMapper.java':'Преобразует JPA-сущности в выходные DTO внутри сервисной транзакции. Hibernate-объекты не попадают напрямую в контроллер.',
'NoteService.java':'Хранит историю заметок, версии, закрепление и авторство. Оператор редактирует свои заметки, администратор — любые.',
'CsvApplicationService.java':'Читает и проверяет CSV, сохраняет предварительный результат, применяет его один раз, сверяя версии. Экспортирует все строки фильтра пакетами по 100.',
'AnalyticsService.java':'В согласованной read-only транзакции вычисляет количества, среднюю готовность и группировки по дисциплине, статусу и риску.',
'AuditService.java':'Записывает значимые действия без секретов и предоставляет поиск журнала. Обычные изменения аудируются в транзакции изменения; входы — отдельно.',
'JwtService.java':'Подписывает и проверяет access JWT, issuer и срок действия. Включает securityVersion для отзыва прежних токенов при смене пароля.',
'JwtAuthenticationFilter.java':'Достает Bearer JWT, проверяет подпись и актуальную учетную запись. Права перечитывает из базы; отдельный cookie принимает только на Swagger URL.',
'PasswordChangeRequiredFilter.java':'Закрывает рабочие API для пользователя с временным паролем, оставляя доступ к смене пароля, данным своей учетной записи и выходу.',
'AuthOriginFilter.java':'Проверяет Origin браузерных auth-запросов. Вместе с SameSite Strict ограничивает использование refresh-cookie с постороннего сайта.',
'PasswordPolicy.java':'Проверяет сложность, минимум 12 символов и максимум 72 байта UTF-8 для BCrypt, а также отсутствие логина в пароле.',
'CurrentUserService.java':'Получает principal из SecurityContext и актуальную учетную запись для сервисов и авторства заметок.',
'UserPrincipal.java':'Представляет текущего пользователя внутри Spring Security и преобразует роли в GrantedAuthority. В публичные DTO хеш пароля не включается.',
'ApplicationFilter.java':'Типизированные параметры поиска и всех фильтров, включая пару оператор/число и режим архива. Bean Validation проверяет диапазоны.',
'ApplicationSpecifications.java':'Строит Criteria API predicates с параметрами. Экранирует wildcard-символы свободного поиска; пользовательский текст не вставляется в SQL.',
'PageableFactory.java':'Разрешает только известные поля и направления сортировки. Проверяет номер страницы и ограничивает размер 100 записями; добавляет стабильный порядок по id.',
'CorrelationIdFilter.java':'Проверяет входящий correlation ID или создает новый, добавляет его в ответ и MDC журнала, затем очищает контекст потока.',
'RequestMetadata.java':'Читает ограниченные метаданные запроса для аудита: correlation ID, реальный remoteAddr и user agent. Не доверяет подставленному X-Forwarded-For.',
'GlobalExceptionHandler.java':'Преобразует исключения в единый ApiError с HTTP-статусом. Подробности и stack trace остаются на сервере; HTML-навигация получает страницы ошибок.',
'AppErrorController.java':'Обрабатывает стандартный servlet-маршрут ошибки и сохраняет корректный статус для HTML и JSON.',
'ApiError.java':'Единый JSON ошибки: время, HTTP-статус, код, сообщение, путь, correlation ID и ошибки отдельных полей.',
'PageResponse.java':'Стабильный публичный DTO страницы: content, page, size, totalElements, totalPages, first и last.',
'ExamDtos.java':'Входные и выходные DTO заявок, кандидатов, экзаменаторов, дисциплин, заметок, массовых действий и аналитики. Вход проверяется Bean Validation.',
'AuthDtos.java':'Контракты login, refresh, смены пароля, сведений о текущем пользователе и выдачи токенов; различает WEB и NATIVE.',
'UserDtos.java':'DTO администрирования пользователей. Запросы могут содержать временный пароль, ответы не содержат пароль или хеш.',
'ImportDtos.java':'DTO предварительной проверки, одноразового применения импорта и ошибок строк; режимы CREATE_ONLY и UPSERT.',
'AuditDtos.java':'Выходная модель события аудита без Hibernate-связей и секретов.',
'ExamApplication.java':'JPA-модель существующей таблицы exam_applications. Сохраняет внутренний BIGINT и публикует UUID; содержит дисциплину, кандидата, статусы, показатели, теги, архив и @Version.',
'Applicant.java':'JPA-модель кандидата из КР1 с UUID публичного API, контактами, статусом и версией. Заявки ссылаются на внутренний ключ кандидата.',
'Examiner.java':'JPA-модель справочника экзаменаторов КР1 с публичным UUID, предметом, активностью и версией.',
'ExamDiscipline.java':'Справочная JPA-сущность с UUID, кодом, названием и активностью, на которую ссылаются заявки.',
'ApplicationNote.java':'JPA-сущность заметки со ссылками на заявку и автора, текстом, закреплением, датами и @Version.',
'UserAccount.java':'Учетная запись с BCrypt-хешем, ролями, активностью, временным паролем, блокировкой, версией и securityVersion.',
'RefreshSession.java':'Хранит SHA-256 refresh-токена, владельца, срок, отзыв и связь с заменившей сессией. Исходный refresh в таблице отсутствует.',
'AuditEvent.java':'Неизменяемое событие аудита с пользователем, типом, UUID сущности, временем, описанием и correlation ID.',
'CsvImportPreview.java':'Сохраняет проверенные строки CSV, владельца, режим, SHA-256 файла, срок и отметку применения; предотвращает подмену и повторное применение.',
'AuditedVersionedEntity.java':'Общий абстрактный JPA-класс: createdAt, updatedAt и @Version. Lifecycle callbacks заполняют даты при вставке и изменении.',
'BusinessRulesTest.java':'16 независимых проверок границ риска, времени, переходов, нулевого балла, политики паролей, фильтров, сортировки и CSV.',
'FullApiIT.java':'16 интеграционных сценариев полной цепочки Spring Security → REST → сервисы → JPA → PostgreSQL. Включает конфликт двух одновременных изменений.',
'index.html':'Минимальная HTML-оболочка веб-приложения: контейнер, стили и JavaScript-модуль. Раздается тем же сервером, что и API.',
'app.js':'Все экраны, формы, таблицы, диалоги, фильтры и действия пользователя. Показывает ошибки, учитывает роли и экранирует вставляемый текст.',
'api.js':'HTTP-клиент браузера: JSON, Bearer JWT в памяти, одно общее обновление access при 401, logout и скачивание файлов. Refresh доступен только серверу через cookie.',
'styles.css':'Внешний вид и адаптивная компоновка экранов, таблиц, форм, окон и навигации для широкого и узкого экрана.',
'favicon.svg':'Векторная иконка вкладки браузера.',
'package.json':'Зависимость Playwright только для тестирования. Фронтенду для запуска не нужны npm, Node.js или сборщик.',
'application.yml':'Настройки PostgreSQL, Hibernate validate, Flyway, multipart, Swagger, health, JWT, CORS и начального администратора. Реальные секреты приходят из окружения.',
'pom.xml':'Maven-зависимости Spring Web, Security, JPA, Validation, Flyway, JWT, CSV, OpenAPI и тестов. Копирует web-ресурсы в JAR; профиль kr3-integration запускает IT.',
'Dockerfile':'Упаковывает собранный JAR и HealthProbe в Java 21 JRE. Приложение выполняется от непривилегированного пользователя.',
'docker-compose.yml':'Запускает отдельные PostgreSQL 17 и backend, передает окружение, ограничивает порты localhost, настраивает healthchecks и постоянный том.',
'V1__kr1_schema.sql':'Создает исходные три таблицы КР1, индексы и ограничения. IF NOT EXISTS позволяет обработать заранее перенесенную копию.',
'V2__security_and_exam_workflow.sql':'Добавляет пользователей, роли, сессии, аудит, дисциплины, UUID, версии, архив, теги, заметки и CSV previews. Дисциплины строит из прежних заявок.',
'V3__empty_database_examples.sql':'Добавляет небольшой учебный набор только если все три исходные таблицы пусты. Перенесенные пользовательские записи не дополняет.',
'V4__discipline_slot_integrity.sql':'Защищает уникальность активной записи кандидата по UUID дисциплины и времени, в том числе после переименования дисциплины.',
'kr3-env.py':'Добавляет недостающие случайные секреты в .env, не меняет имеющиеся значения и закрывает доступ к файлу другим пользователям.',
'kr3-start.sh':'Проверяет Java, Maven и Docker, создает настройки, поднимает базу, однократно копирует КР1 из действующей КР2, собирает и запускает КР3.',
'kr3-stop.sh':'Останавливает только Compose-проект КР3 без удаления тома PostgreSQL.',
'kr3-test-api.py':'Пересоздает только отдельную *_test базу, передает секреты через окружение и запускает unit и PostgreSQL интеграционные тесты.',
'kr3-test-browser.py':'Поднимает временный Docker backend на отдельной *_test базе и случайном localhost-порту, запускает Playwright и удаляет свой контейнер.',
'kr3-browser-test.mjs':'Проводит 11 сквозных браузерных сценариев: вход, роли, CRUD, заметки, архив, CSV, страницы, сбои сети, Swagger, адаптивность и пароли.',
'kr3-check-data.py':'Сравнивает исходные поля трех таблиц КР1 с перенесенной базой КР3 и сохраняет только количества и SHA-256, без личных данных.',
'kr3-build-guide.py':'Создает этот Word-файл, каталог файлов и отчет тестов из исходной документации и результатов проверок.',
'Запустить КР3.command':'Mac-ярлык запуска подготовки КР3 и открытия localhost в браузере.',
'kr3.yml':'GitHub Actions: сборка и unit-тесты, затем полные API-тесты на PostgreSQL 17 в отдельной тестовой базе.'
}
files=[]
for top in ['web-backend/src','web']:
 for p in sorted((root/top).rglob('*')):
  if not p.is_file() or any(x in p.parts for x in ['node_modules','target','test-results']):continue
  if '/errors/' in str(p):purpose='Статическое HTML-представление соответствующей HTTP-ошибки или общие стили таких страниц. Возвращается с исходным статусом без раскрытия stack trace.'
  elif p.name in descriptions:purpose=descriptions[p.name]
  elif p.parent.name=='domain' and p.suffix=='.java':purpose='Enum допустимых значений предметной области; используется JPA, DTO, бизнес-правилами и JSON.'
  elif p.parent.name=='repository':purpose='Spring Data JPA-репозиторий соответствующей сущности. Предоставляет типизированные запросы и, где нужно, блокировку строки для транзакций.'
  elif p.parent.name=='error':purpose='Прикладное исключение соответствующего HTTP-сценария; GlobalExceptionHandler превращает его в единый ответ.'
  else:raise RuntimeError('Add file description: '+str(p))
  files.append({'path':str(p.relative_to(root)),'purpose':purpose})
for name in ['web-backend/pom.xml','web-backend/Dockerfile','web-backend/docker-compose.yml','Запустить КР3.command','.github/workflows/kr3.yml']+[str(p.relative_to(root)) for p in sorted((root/'scripts').glob('kr3-*')) if p.suffix!='.pyc']:
 files.append({'path':name,'purpose':descriptions[Path(name).name]})
(dest/'file-catalog.json').write_text(json.dumps(files,ensure_ascii=False,indent=2),encoding='utf-8')
reports=[]
for directory in ['common/target/surefire-reports','web-backend/target/surefire-reports','web-backend/target/failsafe-reports']:
 for p in (root/directory).glob('TEST-*.xml'):
  r=ET.parse(p).getroot();reports.append({k:r.attrib.get(k) for k in ['name','tests','failures','errors','skipped']})
summary={'date':'2026-11-19','junit':reports,'junitTests':sum(int(r['tests']) for r in reports),'junitFailures':sum(int(r['failures'])+int(r['errors']) for r in reports)}
browser=json.loads((dest/'qa/browser-results.json').read_text());summary['browserScenarios']=len(browser['results']);summary['browserFailures']=sum(r['status']!='PASS' for r in browser['results']);summary['unhandledJavaScriptErrors']=browser['unhandledJavaScriptErrors']
(dest/'qa/test-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
testing=f'''# Проверки КР3\n\nДата: 19 ноября 2026 года.\n\nJUnit: {summary['junitTests']} проверок, ошибок и падений {summary['junitFailures']}. Браузер: {summary['browserScenarios']} сквозных сценариев, падений {summary['browserFailures']}. Необработанных JavaScript-ошибок: {len(summary['unhandledJavaScriptErrors'])}.\n\nСостав JUnit: 44 проверки общей библиотеки КР2, 16 чистых проверок КР3 и 16 интеграционных сценариев на PostgreSQL. API проверяет реальную цепочку фильтров безопасности, контроллеров, сервисов и JPA, включая одновременные изменения. Браузер работает через настоящий HTTP с backend в Docker.\n\n'''
testing+='\n'.join('- '+r['name']+': '+r['status'] for r in browser['results'])+'\n\n'
testing+='Проверены перенос полей КР1, миграции на существующей и пустой базе, health, обычный перезапуск с сохранением данных. Отчеты: qa/test-summary.json, qa/browser-results.json и qa/data-migration.json. Скриншоты 01–07 показывают тестовые данные.\n\n'
testing+='Среда проверки: macOS ARM, JDK 21, Maven 3.9, PostgreSQL 17 в Docker, Chromium из Яндекс Браузера. Это функциональная проверка всех реализованных экранов и основных отрицательных сценариев, а не гарантия отсутствия любых возможных дефектов. Промышленная нагрузка, внешнее размещение с HTTPS, Windows и Android-клиент в этот прогон не входят.\n'
(dest/'testing.md').write_text(testing,encoding='utf-8')
doc=Document();sec=doc.sections[0];sec.page_width=Inches(8.5);sec.page_height=Inches(11);sec.top_margin=Inches(.7);sec.bottom_margin=Inches(.9);sec.footer_distance=Inches(.3);sec.left_margin=Inches(.75);sec.right_margin=Inches(.75)
for name in ['Normal','Title','Subtitle','Heading 1','Heading 2','Heading 3']:
 s=doc.styles[name];s.font.name='Calibri';s.font.color.rgb=RGBColor(0,0,0)
doc.styles['Normal'].font.size=Pt(11);doc.styles['Normal'].paragraph_format.space_after=Pt(7);doc.styles['Normal'].paragraph_format.line_spacing=1.12
doc.styles['Title'].font.size=Pt(26)
for name,size in [('Heading 1',17),('Heading 2',13),('Heading 3',11)]:doc.styles[name].font.size=Pt(size);doc.styles[name].paragraph_format.keep_with_next=True
doc.core_properties.title='КР3 Экзаменационный центр руководство';doc.core_properties.author=''
doc.add_paragraph('КР3 Экзаменационный центр',style='Title')
doc.add_paragraph('Руководство пользователя и устройство проекта',style='Subtitle')
doc.add_paragraph('Версия от 19 ноября 2026 года. Документ объясняет запуск, все функции веб-приложения, права сотрудников, серверную архитектуру и назначение файлов. Основная рабочая версия КР3 находится в web-backend и web; настольная КР2 сохранена отдельно.')
def markdown(source):
 for line in source.splitlines():
  if not line.strip():continue
  if line.startswith('# '):continue
  if line.startswith('## '):doc.add_heading(line[3:],level=1);continue
  if line.startswith('### '):doc.add_heading(line[4:],level=2);continue
  p=doc.add_paragraph();pieces=re.split(r'(`[^`]+`)',line)
  for part in pieces:
   r=p.add_run(part.strip('`') if part.startswith('`') else part)
   if part.startswith('`'):r.font.name='Consolas';r.font.size=Pt(9)
markdown((dest/'user-guide.md').read_text())
doc.add_heading('Как устроен сервер',level=1)
doc.add_paragraph('REST-контроллеры принимают DTO и проверяют права. Сервисы применяют правила, пересчитывают риск, задают транзакции и создают аудит. Репозитории Spring Data JPA работают с Hibernate-сущностями и PostgreSQL. Flyway выполняет SQL по порядку, а Hibernate validate сверяет отображение сущностей с готовой схемой. Ни контроллер, ни браузер не получают JPA-сущности напрямую.')
doc.add_paragraph('В браузере index.html подключает app.js и styles.css. app.js создает экраны и вызывает api.js. Последний добавляет Bearer JWT, обновляет его через refresh при истечении и передает ответы формам. Access хранится в памяти страницы, refresh — в HttpOnly cookie. Для будущего Android предусмотрен NATIVE-режим login и refresh с отдельным токеном в JSON.')
doc.add_paragraph('Публичные адреса используют UUID. Числовые ключи трех старых таблиц оставлены внутри PostgreSQL, чтобы перенести КР1 без разрушения связей. Внешний код заявки — отдельное поле externalId для работы сотрудника и UPSERT CSV. @Version защищает от потери изменений при работе нескольких сотрудников.')
doc.add_heading('Файлы и их связи',level=1)
doc.add_paragraph(f'Ниже описаны все {len(files)} файлов кода и конфигурации новой КР3. Старые common, desktop и backend описаны в прежнем руководстве docs/Как работает приложение.docx. Они сохраняют назначение КР2. Полный машиночитаемый список КР3 находится в docs/kr3/file-catalog.json.')
doc.add_paragraph('Пути основных Java-файлов ниже указаны относительно web-backend/src/main/java/ru/mirea/examcenter/kr3. Пути тестов — относительно аналогичного пакета src/test/java и помечены tests. Для остальных файлов путь указан от корня проекта.')
last=''
for row in files:
 p=Path(row['path']);group=p.parent.name
 if group!=last:doc.add_heading(group,level=2);last=group
 label=row['path'].replace('web-backend/src/main/java/ru/mirea/examcenter/kr3/','').replace('web-backend/src/test/java/ru/mirea/examcenter/kr3/','tests/')
 a=doc.add_paragraph();a.paragraph_format.keep_with_next=True;a.paragraph_format.keep_together=True;r=a.add_run(label);r.bold=True;r.font.size=Pt(10)
 doc.add_paragraph(row['purpose'])
doc.add_heading('Локальные данные и документация',level=1)
doc.add_paragraph('Корневой .env содержит только локальные пароли и JWT-секреты, игнорируется Git. .env.example задает имена настроек без рабочих секретов. backups/kr3 хранит резервную копию исходников и дамп КР1; их нельзя публиковать вместе с учетными данными. Том exam-center-kr3_exam_web_data хранит PostgreSQL между перезапусками. target, node_modules, личные логи и дампы являются создаваемыми файлами и в GitHub не включаются.')
doc.add_paragraph('docs/kr3/user-guide.md содержит исходный текст руководства; requirements-map.md связывает пункты задания с реализацией; implementation-plan.md фиксирует последовательность; testing.md и qa/*.json содержат результаты проверок. README.md объясняет быстрый запуск. .github/workflows/kr3.yml повторяет unit и API проверки на GitHub.')
doc.add_heading('Результаты проверки',level=1);markdown(testing)
doc.add_heading('Что изучить для защиты',level=1)
for text in ['Java: классы, интерфейсы, наследование, enum, коллекции, исключения и records. На примере AuditedVersionedEntity объясните абстрактный класс и наследование JPA-сущностей.', 'Spring Boot: запуск контекста, dependency injection через конструкторы, компоненты и бины, REST-контроллеры, DTO, Bean Validation и единый обработчик ошибок.', 'Spring Security: BCrypt, роли, JWT, отличие access от refresh, HttpOnly и SameSite cookies, обязательная смена пароля, блокировка входа и отзыв сессий.', 'JPA и PostgreSQL: Entity, ManyToOne, ElementCollection, Repository, Specification, @Version, транзакция и откат, ограничения и индексы, миграции Flyway и validate.', 'Web: DOM, формы и события, async/await, fetch, JSON, HTTP-методы и коды, экранирование пользовательского текста, обновление токена и серверная пагинация.', 'Docker и тесты: образ и контейнер, Compose, healthcheck, port mapping, volume; отличие unit-теста от интеграционного и браузерного сценария.']:
 doc.add_paragraph(text)
footer=sec.footer.paragraphs[0];footer.alignment=2;r=footer.add_run();field=OxmlElement('w:fldSimple');field.set(qn('w:instr'),'PAGE');r._r.addnext(field)
for element in [doc.styles.element,doc.element]:
 for border in list(element.iter(qn('w:pBdr'))):border.getparent().remove(border)
doc.save(dest/'КР3 Руководство по приложению.docx')
print(f'Created Word guide, catalog of {len(files)} files, and test summary')
