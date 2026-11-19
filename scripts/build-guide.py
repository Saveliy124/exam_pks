#!/usr/bin/env python3
"""Create the Word manual from current documentation and the full file catalog.
Run with the bundled Python runtime containing python-docx; render separately for visual QA.
"""
from pathlib import Path
import json,re
from docx import Document
from docx.shared import Cm,Pt,RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.table import WD_TABLE_ALIGNMENT,WD_CELL_VERTICAL_ALIGNMENT
ROOT=Path(__file__).resolve().parents[1]
OUTPUT=ROOT/'docs/Как работает приложение.docx'
doc=Document();section=doc.sections[0]
# Preserve the A4 format of the existing manual.
section.page_width=Cm(21);section.page_height=Cm(29.7)
section.top_margin=Cm(1.8);section.bottom_margin=Cm(1.8);section.left_margin=Cm(2);section.right_margin=Cm(2)
styles=doc.styles
normal=styles['Normal'];normal.font.name='Arial';normal.font.size=Pt(10.5);normal.font.color.rgb=RGBColor(25,35,40)
normal.paragraph_format.line_spacing=1.12;normal.paragraph_format.space_after=Pt(6)
for name,size,before,after in [('Title',23,0,12),('Heading 1',16,16,8),('Heading 2',12,12,6),('Heading 3',11,9,5)]:
 st=styles[name];st.font.name='Arial';st.font.size=Pt(size);st.font.color.rgb=RGBColor(0,0,0);st.font.bold=name!='Title';st.paragraph_format.space_before=Pt(before);st.paragraph_format.space_after=Pt(after);st.paragraph_format.keep_with_next=True
 for child in list(st._element.get_or_add_pPr()):
  if child.tag==qn('w:pBdr'):st._element.get_or_add_pPr().remove(child)
# Page numbers help navigate a long reference manual.
p=section.footer.paragraphs[0];p.alignment=2
field=OxmlElement('w:fldSimple');field.set(qn('w:instr'),'PAGE');p._p.append(field)

def inline(p,text):
 def link(m):
  label=m.group(1);target=m.group(2).strip('<>').split('#')[0]
  return label if target==label or target.endswith('/'+label.strip('`')) else label+' ('+target+')'
 text=re.sub(r'\[([^]]+)\]\(([^)]+)\)',link,text)
 for token in re.split(r'(\*\*[^*]+\*\*|`[^`]+`)',text):
  if not token:continue
  if token.startswith('**') and token.endswith('**'):run=p.add_run(token[2:-2]);run.bold=True
  elif token.startswith('`') and token.endswith('`'):run=p.add_run(token[1:-1]);run.font.name='Courier New';run.font.size=Pt(9)
  else:p.add_run(token)

def paragraph(text):
 p=doc.add_paragraph();inline(p,text);return p

def table(rows,widths=None):
 n=len(rows[0]);t=doc.add_table(rows=len(rows),cols=n);t.autofit=False;t.alignment=WD_TABLE_ALIGNMENT.CENTER
 if widths is None:widths=[6,11] if n==2 else [4.1,5.5,7.4]
 for j,w in enumerate(widths):t.columns[j].width=Cm(w)
 for i,row in enumerate(rows):
  trpr=t.rows[i]._tr.get_or_add_trPr();trpr.append(OxmlElement('w:cantSplit'))
  if i==0:trpr.append(OxmlElement('w:tblHeader'))
  for j,value in enumerate(row):
   c=t.cell(i,j);c.width=Cm(widths[j]);c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
   pp=c.paragraphs[0];pp.paragraph_format.space_after=Pt(0);pp.paragraph_format.line_spacing=1.07;inline(pp,value)
   for run in pp.runs:run.font.size=Pt(9);run.bold=i==0 or run.bold;run.font.color.rgb=RGBColor(255,255,255) if i==0 else RGBColor(25,35,40)
   pr=c._tc.get_or_add_tcPr();shd=OxmlElement('w:shd');shd.set(qn('w:fill'),'17313D' if i==0 else ('F5F8FA' if i%2==0 else 'FFFFFF'));pr.append(shd)
   border=OxmlElement('w:tcBorders')
   for side in ['top','left','bottom','right']:
    el=OxmlElement('w:'+side);el.set(qn('w:val'),'single');el.set(qn('w:sz'),'4');el.set(qn('w:color'),'D9D9D9');border.append(el)
   pr.append(border);margins=OxmlElement('w:tcMar')
   for side,val in [('top',85),('bottom',85),('left',100),('right',100)]:
    el=OxmlElement('w:'+side);el.set(qn('w:w'),str(val));el.set(qn('w:type'),'dxa');margins.append(el)
   pr.append(margins)
 doc.add_paragraph().paragraph_format.space_after=Pt(2)
 return t

def markdown(path,title):
 lines=path.read_text().splitlines();i=0
 doc.add_heading(title,1)
 while i<len(lines):
  line=lines[i].strip()
  if not line or line.startswith('# '):i+=1;continue
  if line.startswith('##'):
   level=min(len(line)-len(line.lstrip('#')),3)
   text=line.lstrip('#').strip();text=re.sub(r'^(\d+)\.\s*',r'\1 ',text)
   doc.add_heading(text,level);i+=1;continue
  if line.startswith('|'):
   rows=[]
   while i<len(lines) and lines[i].strip().startswith('|'):
    cells=[s.strip() for s in lines[i].strip().strip('|').split('|')]
    if not all(re.fullmatch(r':?-+:?',s) for s in cells):rows.append(cells)
    i+=1
   widths=[1.7,7.5,7.8] if rows and rows[0][0]=='Метод' else None
   table(rows,widths);continue
  if line.startswith('```'):
   block=[];i+=1
   while i<len(lines) and not lines[i].strip().startswith('```'):block.append(lines[i]);i+=1
   p=doc.add_paragraph();p.paragraph_format.keep_together=True;p.paragraph_format.left_indent=Cm(.3)
   rr=p.add_run('\n'.join(block));rr.font.name='Courier New';rr.font.size=Pt(8.5)
   i+=1;continue
  if line.startswith('- '):line='• '+line[2:]
  paragraph(line);i+=1

doc.add_paragraph('Экзаменационный центр',style='Title')
p=paragraph('Подробное руководство по Spring Boot и JavaFX');p.runs[0].bold=True
paragraph('Версия 3.0.0  |  23 октября 2026 года')
paragraph('Руководство объясняет, как пользоваться всеми функциями приложения и как они устроены внутри. Основная схема работы: настольное окно JavaFX отправляет HTTP-запросы нашему Spring Boot, а сервер проверяет правила и сохраняет данные в PostgreSQL. Это позволяет готовить следующий этап проекта и позже подключать другие клиенты к тому же API.')
paragraph('Сначала прочитайте инструкцию пользователя, затем устройство системы и справку API. В разделе файлов для каждого передаваемого файла указаны назначение и связи. В конце находятся результаты проверок и сценарий защиты. Для поиска класса или имени файла используйте поиск Word; заголовки также доступны в панели навигации.')
paragraph('Один кандидат может иметь несколько заявок. Экзаменатор остаётся отдельным справочником. Все исходные рабочие записи КР1 сохранены. Учебный Student Hub преподавателя существует отдельно и не обслуживает наше приложение.')
table([['Часть','Роль','Где работает'],['JavaFX desktop','Окна, ввод, фильтры, сортировка и Excel','На Mac пользователя'],['Spring Boot backend','REST, правила, транзакции и SQL','Docker, HTTP 8081'],['PostgreSQL','Постоянные таблицы и ограничения','Docker, опубликованный порт 55433'],['common','Общие модели, DTO и чистые функции','Библиотека двух модулей']])
paragraph('В обычном режиме HTTP-адрес 8081, в демонстрации 8082. Тесты используют отдельную БД с суффиксом _test. В данной учебной версии нет авторизации и пользовательских ролей; сервисы опубликованы только на localhost.')
markdown(ROOT/'docs/user-guide.md','Использование приложения')
markdown(ROOT/'docs/architecture.md','Устройство системы')
paragraph('JDK — инструменты языка Java и JVM. Maven скачивает библиотеки и собирает модули по pom.xml. JavaFX создаёт окна, FXML описывает структуру, CSS — оформление. Spring Boot запускает сервер и собирает его компоненты. DataSource/Hikari выдаёт соединения из пула. JDBC передаёт SQL в PostgreSQL. Jackson преобразует JSON и Java-объекты. Apache POI создаёт Excel. JUnit/AssertJ проверяют результаты. Docker Compose запускает серверы и сохраняет БД в volume.')
markdown(ROOT/'docs/api.md','Справка REST API')
doc.add_heading('Назначение каждого файла и связи',1)
paragraph('Каталог охватывает каждый файл собственного передаваемого проекта: исходники трёх модулей, ресурсы, сборку, сценарии, SQL и документацию. Файлы target, Maven-кэш, Git и системные .DS_Store создаются инструментами и не входят в исходный архив. Архив преподавателя — внешний проект; его роль и отдельный запуск описаны в instructor-example.md.')
entries=json.loads((ROOT/'docs/file-catalog.json').read_text())
groups=[('common/','Общий модуль common'),('backend/','Серверный модуль backend'),('desktop/','Настольный модуль desktop'),('scripts/','Сценарии подготовки и проверок'),('sql/','SQL схема и данные'),('docker/','Поддержка Docker и учебного примера'),('ROOT','Файлы в корне'),('docs/','Документация')]
for prefix,title in groups:
 doc.add_heading(title,2)
 selected=[e for e in entries if (('/' not in e['path']) if prefix=='ROOT' else e['path'].startswith(prefix)) and not e['path'].startswith('docs/qa/')]
 for e in selected:
  p=doc.add_paragraph();p.paragraph_format.keep_with_next=True;p.paragraph_format.space_before=Pt(6);p.paragraph_format.space_after=Pt(3)
  run=p.add_run(e['path']);run.font.name='Courier New';run.font.size=Pt(8.8);run.bold=True
  p=paragraph(e['purpose']);p.paragraph_format.keep_with_next=True
  p=paragraph('Связи: '+e['links']);p.paragraph_format.keep_together=True

doc.add_heading('Файлы доказательств проверок',2)
paragraph('Следующие файлы находятся в docs/qa. Для каждого указано, что он подтверждает. Изображения 01–12 и filtered.xlsx обновлены после сквозного прогона через наш Docker backend; instructor-* относятся к прошлому отдельному примеру преподавателя.')
table([['Файл','Назначение']]+[[e['path'],e['purpose']] for e in entries if e['path'].startswith('docs/qa/')],[7,10])
doc.add_heading('Локальные и создаваемые файлы',2)
table([['Файл или каталог','Назначение и связи'],['.env','Реальные параметры Compose и пароль БД. Создаётся setup, исключён из Git/ZIP. Не содержит настройку пользовательского входа в приложение.'],['config/client.properties','HTTP-адрес обычного backend для run.sh/run.ps1; пароля БД нет.'],['config/database.properties','Локальные JDBC-реквизиты только диагностической консоли. В нормальном GUI не используется.'],['common/target desktop/target backend/target','Результаты Maven: class, ресурсы, JAR, отчёты тестов и снимки. Можно пересобрать. Старый корневой target относится к предыдущей структуре.'],['backups/*.dump','Резервные копии PostgreSQL, в том числе перед Spring-переходом. Не включаются в исходники.'],['dist/exam-center-spring-source.zip','Переносимая копия исходников и документов без .env/target/чужого архива. Старое имя ZIP записывается как alias той же копии.'],['student-hub-suite-rebuilt','Исходный внешний пример преподавателя, со своими backend/javafx-client/android-client и локальными секретами. Не наш сервер.'],['.git .idea .m2 и .DS_Store','История Git, настройки IDE, кэш Maven и служебные данные системы. Не составляют бизнес-логику приложения.']],[6.3,10.7])
markdown(ROOT/'docs/testing.md','Проверка работоспособности')
markdown(ROOT/'docs/defense.md','Подготовка к защите')
doc.add_heading('Где читать дальше',1)
paragraph('Официальная документация Spring Boot 3.5: https://docs.spring.io/spring-boot/3.5/ . Spring JDBC и транзакции: https://docs.spring.io/spring-framework/reference/data-access/ . JavaFX: https://openjfx.io/ . В проекте точные зависимости находятся в корневом и модульных pom.xml. Для понимания конкретной операции начните с пути, показанного в сценарии защиты, и откройте перечисленные классы в IDE.')
doc.core_properties.title='Экзаменационный центр руководство по Spring Boot и JavaFX'
doc.core_properties.subject='Функции приложения архитектура API назначение файлов и тестирование'
doc.core_properties.author=''
doc.save(OUTPUT)
print(OUTPUT)
