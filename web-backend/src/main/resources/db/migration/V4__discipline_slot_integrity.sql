-- A discipline may be renamed; uniqueness is tied to its stable UUID.
CREATE UNIQUE INDEX uq_active_discipline_slot
 ON exam_applications(applicant_id,discipline_id,scheduled_at)
 WHERE status IN ('NEW','APPROVED') AND NOT archived;
