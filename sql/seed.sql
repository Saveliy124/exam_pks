INSERT INTO applicants(full_name, email, phone) VALUES
  ('Анна Смирнова', 'anna.smirnova@example.org', '+7 900 111-11-11'),
  ('Борис Иванов', 'boris.ivanov@example.org', '+7 900 222-22-22'),
  ('Вера Кузнецова', 'vera.kuznetsova@example.org', '+7 900 333-33-33'),
  ('Глеб Соколов', 'gleb.sokolov@example.org', '+7 900 444-44-44'),
  ('Дарья Орлова', 'daria.orlova@example.org', '+7 900 555-55-55')
ON CONFLICT (email) DO NOTHING;

INSERT INTO examiners(full_name, email, subject, status) VALUES
  ('Мария Петрова', 'maria.petrova@exam.org', 'Математика', 'ACTIVE'),
  ('Игорь Волков', 'igor.volkov@exam.org', 'Физика', 'ACTIVE'),
  ('Елена Морозова', 'elena.morozova@exam.org', 'Информатика', 'ACTIVE'),
  ('Ольга Белова', 'olga.belova@exam.org', 'Русский язык', 'INACTIVE'),
  ('Павел Романов', 'pavel.romanov@exam.org', 'История', 'ACTIVE')
ON CONFLICT (email) DO NOTHING;

WITH seed(email, exam_name, day_offset, status, score) AS (
    VALUES
      ('anna.smirnova@example.org', 'Математика', 7, 'NEW', NULL::smallint),
      ('anna.smirnova@example.org', 'Физика', 12, 'APPROVED', NULL::smallint),
      ('boris.ivanov@example.org', 'Информатика', 4, 'NEW', NULL::smallint),
      ('boris.ivanov@example.org', 'Английский язык', -5, 'COMPLETED', 87::smallint),
      ('vera.kuznetsova@example.org', 'Русский язык', 9, 'APPROVED', NULL::smallint),
      ('vera.kuznetsova@example.org', 'История', -8, 'COMPLETED', 74::smallint),
      ('gleb.sokolov@example.org', 'Химия', 15, 'CANCELLED', NULL::smallint),
      ('gleb.sokolov@example.org', 'Биология', 16, 'NEW', NULL::smallint),
      ('daria.orlova@example.org', 'Обществознание', 20, 'APPROVED', NULL::smallint),
      ('daria.orlova@example.org', 'География', -2, 'CANCELLED', NULL::smallint)
)
INSERT INTO exam_applications(applicant_id, exam_name, scheduled_at, status, score)
SELECT a.id, s.exam_name,
       date_trunc('hour', CURRENT_TIMESTAMP) + s.day_offset * interval '1 day',
       s.status, s.score
FROM seed s
JOIN applicants a ON a.email = s.email
WHERE NOT EXISTS (
    SELECT 1 FROM exam_applications e
    WHERE e.applicant_id = a.id AND e.exam_name = s.exam_name
);
