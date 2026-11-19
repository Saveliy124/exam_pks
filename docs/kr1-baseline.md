# Исходное состояние КР1

Коммит: `6fccad7`.

Резервная копия проекта, истории Git и PostgreSQL: `/Users/savochka1111/Desktop/уник/пкс-backups/kr1-before-kr2-20261016-143351`.

База перенесена через pg_dump / pg_restore с 55432 на отдельный PostgreSQL 16 в Docker, порт 55433. Исходная БД сохранена. Число записей после восстановления совпадает:

```text
applicants|6
exam_applications|10
examiners|5
```
