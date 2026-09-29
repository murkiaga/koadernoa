-- MySQL 8. Run ONCE, with the application stopped and a verified database backup.
-- Run WITHOUT --force: any SIGNAL/error must stop the migration.
-- First review the proposed equivalences below. Same EEI + ordena is the identity.
-- If those ordinals do not mean the same outcome, correct the source data first.
SELECT m.eei_kodea, ie.ordena, ie.id, ie.kodea, m.hizkuntza, ie.deskribapena
FROM ikaskuntza_emaitza ie JOIN moduloa m ON m.id=ie.moduloa_id
ORDER BY m.eei_kodea, ie.ordena, ie.id;

DELIMITER //
CREATE PROCEDURE ethazi_translation_preflight()
BEGIN
  IF EXISTS (SELECT 1 FROM ikaskuntza_emaitza ie JOIN moduloa m ON m.id=ie.moduloa_id
             WHERE m.eei_kodea IS NULL OR TRIM(m.eei_kodea)='' OR m.hizkuntza IS NULL
                OR m.hizkuntza NOT IN ('EUSKARA','GAZTELERA','ERDERA','INGELERA')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Set EEI codes and explicit languages on all modules with outcomes first.';
  END IF;
  IF EXISTS (SELECT 1 FROM ikaskuntza_emaitza ie JOIN moduloa m ON m.id=ie.moduloa_id
             GROUP BY m.eei_kodea, ie.ordena, IF(m.hizkuntza='ERDERA','GAZTELERA',m.hizkuntza)
             HAVING COUNT(DISTINCT BINARY ie.deskribapena)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting descriptions for the same EEI/order/language. Resolve before merging.';
  END IF;
  IF EXISTS (SELECT 1 FROM ethazi_kiniela_pisua p JOIN ikaskuntza_emaitza ie ON ie.id=p.emaitza_id
             JOIN moduloa m ON m.id=ie.moduloa_id GROUP BY p.adierazlea_id, m.eei_kodea, ie.ordena
             HAVING COUNT(DISTINCT p.pisua)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting shared weights. Resolve before merging outcomes.';
  END IF;
  IF EXISTS (SELECT 1 FROM ethazi_kiniela_lotura l JOIN ikaskuntza_emaitza ie ON ie.id=l.emaitza_id
             GROUP BY l.adierazlea_id, ie.moduloa_id, ie.ordena HAVING COUNT(*)>1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate contextual notes. Resolve duplicate module/outcome ordinals first.';
  END IF;
END//
DELIMITER ;
CALL ethazi_translation_preflight();
DROP PROCEDURE ethazi_translation_preflight;

-- MySQL DDL commits implicitly. The full backup is the rollback for this section.
ALTER TABLE ikaskuntza_emaitza ADD COLUMN eei_kodea VARCHAR(255),
  ADD COLUMN deskribapena_es TEXT, ADD COLUMN deskribapena_en TEXT,
  MODIFY COLUMN moduloa_id BIGINT NULL;
ALTER TABLE ethazi_lorpen_adierazlea ADD COLUMN deskribapena_es TEXT, ADD COLUMN deskribapena_en TEXT;
ALTER TABLE ethazi_gaitasuna ADD COLUMN deskribapena_es TEXT, ADD COLUMN deskribapena_en TEXT;
ALTER TABLE ethazi_gaitasun_maila ADD COLUMN deskribapena_es TEXT, ADD COLUMN deskribapena_en TEXT;
ALTER TABLE ethazi_mailakatze_eredua ADD COLUMN izena_es TEXT, ADD COLUMN izena_en TEXT;
ALTER TABLE ethazi_mailakatze_maila ADD COLUMN izena_es TEXT, ADD COLUMN izena_en TEXT;
ALTER TABLE ethazi_kiniela_lotura ADD COLUMN moduloa_id BIGINT NULL;
-- Keep a supporting index before replacing the old two-column unique index.
ALTER TABLE ethazi_kiniela_lotura ADD INDEX ix_kiniela_adierazlea(adierazlea_id);
SET @old_index = (SELECT INDEX_NAME FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ethazi_kiniela_lotura' AND NON_UNIQUE=0 AND INDEX_NAME<>'PRIMARY'
  GROUP BY INDEX_NAME HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)='adierazlea_id,emaitza_id' LIMIT 1);
SET @drop_index = IF(@old_index IS NULL, 'SELECT 1', CONCAT('ALTER TABLE ethazi_kiniela_lotura DROP INDEX `', @old_index, '`'));
PREPARE stmt FROM @drop_index;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Retain the exact old-to-new IDs and old data for auditing/recovery.
CREATE TABLE ethazi_ie_migration_backup AS SELECT ie.*, m.hizkuntza AS old_language, m.eei_kodea AS source_eei
  FROM ikaskuntza_emaitza ie JOIN moduloa m ON m.id=ie.moduloa_id;
CREATE TABLE ethazi_ie_migration_map AS
  SELECT ie.id AS old_id, canonical.new_id FROM ikaskuntza_emaitza ie
  JOIN moduloa m ON m.id=ie.moduloa_id
  JOIN (SELECT m.eei_kodea, ie.ordena, MIN(ie.id) AS new_id FROM ikaskuntza_emaitza ie
        JOIN moduloa m ON m.id=ie.moduloa_id GROUP BY m.eei_kodea, ie.ordena) canonical
    ON canonical.eei_kodea=m.eei_kodea AND canonical.ordena=ie.ordena;
CREATE TABLE ethazi_ie_migration_text AS
 SELECT map.new_id, MIN(b.source_eei) AS eei_kodea,
   MAX(CASE WHEN b.old_language='EUSKARA' THEN b.deskribapena END) AS eu,
   MAX(CASE WHEN b.old_language IN ('GAZTELERA','ERDERA') THEN b.deskribapena END) AS es,
   MAX(CASE WHEN b.old_language='INGELERA' THEN b.deskribapena END) AS en
 FROM ethazi_ie_migration_map map JOIN ethazi_ie_migration_backup b ON b.id=map.old_id GROUP BY map.new_id;
CREATE TABLE ethazi_ie_migration_links AS
 SELECT DISTINCT j.lorpen_adierazlea_id, map.new_id AS ikaskuntza_emaitza_id
 FROM ethazi_lorpen_adierazlea_ikaskuntza_emaitza j JOIN ethazi_ie_migration_map map ON map.old_id=j.ikaskuntza_emaitza_id;
CREATE TABLE ethazi_ie_migration_weights AS
 SELECT DISTINCT p.adierazlea_id, map.new_id AS emaitza_id, p.pisua
 FROM ethazi_kiniela_pisua p JOIN ethazi_ie_migration_map map ON map.old_id=p.emaitza_id;

DELIMITER //
CREATE PROCEDURE ethazi_translation_data()
BEGIN
  DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
  START TRANSACTION;
  UPDATE ethazi_kiniela_lotura l JOIN ikaskuntza_emaitza ie ON ie.id=l.emaitza_id
    JOIN ethazi_ie_migration_map map ON map.old_id=ie.id
    SET l.moduloa_id=ie.moduloa_id, l.emaitza_id=map.new_id;
  -- Link IDs stay intact, including their challenge selections and notes.
  DELETE FROM ethazi_lorpen_adierazlea_ikaskuntza_emaitza;
  INSERT INTO ethazi_lorpen_adierazlea_ikaskuntza_emaitza(lorpen_adierazlea_id,ikaskuntza_emaitza_id) SELECT * FROM ethazi_ie_migration_links;
  DELETE FROM ethazi_kiniela_pisua;
  INSERT INTO ethazi_kiniela_pisua(adierazlea_id,emaitza_id,pisua) SELECT * FROM ethazi_ie_migration_weights;
  DELETE ie FROM ikaskuntza_emaitza ie JOIN ethazi_ie_migration_map map ON map.old_id=ie.id WHERE map.old_id<>map.new_id;
  UPDATE ikaskuntza_emaitza ie JOIN ethazi_ie_migration_text t ON t.new_id=ie.id
    SET ie.eei_kodea=t.eei_kodea, ie.deskribapena=COALESCE(t.eu,''), ie.deskribapena_es=t.es,
        ie.deskribapena_en=t.en, ie.moduloa_id=NULL;
  COMMIT;
END//
DELIMITER ;
CALL ethazi_translation_data();
DROP PROCEDURE ethazi_translation_data;
ALTER TABLE ikaskuntza_emaitza MODIFY COLUMN eei_kodea VARCHAR(255) NOT NULL,
  ADD CONSTRAINT uk_ie_eei_ordena UNIQUE(eei_kodea,ordena);
ALTER TABLE ethazi_kiniela_lotura MODIFY COLUMN moduloa_id BIGINT NOT NULL,
  ADD CONSTRAINT fk_kiniela_moduloa FOREIGN KEY(moduloa_id) REFERENCES moduloa(id),
  ADD CONSTRAINT uk_kiniela_modulua UNIQUE(adierazlea_id,emaitza_id,moduloa_id);
-- Remaining new challenge columns/table are additive and created by ddl-auto=update.
