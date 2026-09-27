# ER диаграмма экзаменационного центра

```mermaid
erDiagram
    APPLICANTS ||--o{ EXAM_APPLICATIONS : "подает"
    APPLICANTS {
        BIGINT id PK
        VARCHAR full_name
        VARCHAR email UK
        VARCHAR phone
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
```

Один кандидат может подать несколько заявок. Каждая заявка принадлежит ровно одному кандидату. Внешний ключ `exam_applications.applicant_id` указывает на `applicants.id`.
