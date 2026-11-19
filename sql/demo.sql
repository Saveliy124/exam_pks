-- Только для ПУСТОЙ отдельной демонстрационной/тестовой БД.
-- Рабочие данные КР1 этот файл не заменяет. Даты остаются актуальными при новом развёртывании.
BEGIN;
DO $$ BEGIN
    IF EXISTS (SELECT 1 FROM applicants) OR EXISTS (SELECT 1 FROM examiners) OR EXISTS (SELECT 1 FROM exam_applications) THEN
        RAISE EXCEPTION 'Демонстрационные данные разрешено добавлять только в пустую БД';
    END IF;
END $$;
INSERT INTO applicants(full_name,email,phone,status) VALUES
('Анна Смирнова','anna.demo@example.org','+7 900 100-20-01','ACTIVE'),
('Дмитрий Козлов','dmitry.demo@example.org','+7 900 100-20-02','ACTIVE'),
('Мария Соколова','maria.demo@example.org','+7 900 100-20-03','ACTIVE'),
('Илья Волков','ilya.demo@university.example','+7 900 100-20-04','ACTIVE'),
('Алексей Петров','alexey.demo@university.example','','BLOCKED'),
('София Орлова','sofia.demo@example.org','','ACTIVE');
INSERT INTO examiners(full_name,email,subject,status) VALUES
('Елена Морозова','elena.demo@example.org','Математика','ACTIVE'),
('Андрей Попов','andrey.demo@example.org','Физика','ACTIVE'),
('Ольга Васильева','olga.demo@example.org','Информатика','ACTIVE'),
('Сергей Новиков','sergey.demo@example.org','Русский язык','INACTIVE');
INSERT INTO exam_applications(applicant_id,exam_name,scheduled_at,status,score)
SELECT a.id, d.exam, CURRENT_DATE + d.offset_days * INTERVAL '1 day' + d.hour * INTERVAL '1 hour', d.status, d.score
FROM (VALUES
 ('anna.demo@example.org','Математика',2,10,'NEW',NULL::smallint),
 ('anna.demo@example.org','Информатика',-4,12,'COMPLETED',92::smallint),
 ('dmitry.demo@example.org','Физика',3,11,'APPROVED',NULL::smallint),
 ('dmitry.demo@example.org','Математика',-2,10,'APPROVED',NULL::smallint),
 ('maria.demo@example.org','Русский язык',-5,9,'COMPLETED',100::smallint),
 ('maria.demo@example.org','Физика',4,14,'CANCELLED',NULL::smallint),
 ('ilya.demo@university.example','Информатика',1,15,'NEW',NULL::smallint),
 ('ilya.demo@university.example','Математика',-7,10,'COMPLETED',0::smallint),
 ('sofia.demo@example.org','Русский язык',5,9,'APPROVED',NULL::smallint),
 ('alexey.demo@university.example','Математика',-8,10,'CANCELLED',NULL::smallint)
) AS d(email,exam,offset_days,hour,status,score)
JOIN applicants a ON a.email=d.email;
COMMIT;
