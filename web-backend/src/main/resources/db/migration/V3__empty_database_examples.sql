-- Examples are inserted only when all three KR1 tables are empty.
-- Imported KR1 data are not changed or supplemented.
DO $$
DECLARE math_id UUID; physics_id UUID; person_id BIGINT;
BEGIN
 IF NOT EXISTS(SELECT 1 FROM applicants) AND NOT EXISTS(SELECT 1 FROM exam_applications) AND NOT EXISTS(SELECT 1 FROM examiners) THEN
  INSERT INTO exam_discipline(code,name) VALUES('MATH','Математика') RETURNING id INTO math_id;
  INSERT INTO exam_discipline(code,name) VALUES('PHYS','Физика') RETURNING id INTO physics_id;
  INSERT INTO applicants(full_name,email,phone,status) VALUES('Иван Петров','ivan@example.org','+79001112233','ACTIVE') RETURNING id INTO person_id;
  INSERT INTO applicants(full_name,email,phone,status) VALUES('Анна Смирнова','anna@example.org','','ACTIVE'),('Павел Орлов','pavel@example.org','','BLOCKED');
  INSERT INTO examiners(full_name,email,subject,status) VALUES('Ольга Соколова','examiner@example.org','Математика','ACTIVE'),('Николай Волков','physics@example.org','Физика','ACTIVE');
  INSERT INTO exam_applications(applicant_id,exam_name,scheduled_at,status,score,external_id,discipline_id,preparation_percent,documents_verified,risk_level)
   VALUES(person_id,'Математика',localtimestamp+interval '10 days','NEW',NULL,'DEMO-001',math_id,85,TRUE,'LOW'),
         (person_id,'Физика',localtimestamp+interval '14 days','APPROVED',NULL,'DEMO-002',physics_id,55,TRUE,'MEDIUM'),
         (person_id,'Математика',localtimestamp-interval '5 days','COMPLETED',82,'DEMO-003',math_id,90,TRUE,'NOT_APPLICABLE');
 END IF;
END $$;
