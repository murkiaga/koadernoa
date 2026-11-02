-- MySQL 8. Kudeatzaileak ikaslearen jokabide desegokia modulurik gabe
-- sortu ahal izateko. Segurua da behin baino gehiagotan exekutatzea.

ALTER TABLE jokabide_desegokia
  MODIFY COLUMN moduloa_id BIGINT NULL;
