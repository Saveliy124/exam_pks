# ER диаграмма экзаменационного центра

```mermaid
erDiagram
    APPLICANTS ||--o{ EXAM_APPLICATIONS : "подает"
    APPLICANTS {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR phone
        VARCHAR status
    }
    EXAM_APPLICATIONS {
        BIGINT id PK
        BIGINT applicant_id FK
        VARCHAR exam_name
        TIMESTAMP scheduled_at
        VARCHAR status
        SMALLINT score
        TIMESTAMP created_at
    }
    EXAMINERS {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR subject
        VARCHAR status
    }
```

Один кандидат может подать несколько заявок. Каждая заявка принадлежит ровно одному кандидату. Внешний ключ `exam_applications.applicant_id` указывает на `applicants.id`.

`examiners` — отдельный справочник экзаменаторов. Для него реализованы собственные CRUD, фильтры и сортировка; назначение экзаменатора на заявку пока не является частью модели КР 1.
