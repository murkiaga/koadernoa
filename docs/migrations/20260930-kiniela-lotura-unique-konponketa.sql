-- MySQL 8. Repairs installations where ddl-auto retained the obsolete
-- UNIQUE(adierazlea_id, emaitza_id), which prevents per-module notes.
-- Safe to run more than once. Stop the application and take a backup first.

DELIMITER //
CREATE PROCEDURE ethazi_repair_kiniela_unique()
BEGIN
  DECLARE missing_modules BIGINT DEFAULT 0;
  DECLARE contextual_indexes INT DEFAULT 0;

  SELECT COUNT(*) INTO missing_modules
  FROM ethazi_kiniela_lotura WHERE moduloa_id IS NULL;
  IF missing_modules > 0 THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT='Kiniela links without moduloa_id exist. Run the full 20260929 ETHAZI language migration first.';
  END IF;

  SELECT COUNT(*) INTO contextual_indexes FROM (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ethazi_kiniela_lotura'
      AND NON_UNIQUE=0 AND INDEX_NAME<>'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)='adierazlea_id,emaitza_id,moduloa_id'
  ) indexes_found;
  IF contextual_indexes=0 THEN
    ALTER TABLE ethazi_kiniela_lotura
      ADD CONSTRAINT uk_kiniela_modulua UNIQUE(adierazlea_id,emaitza_id,moduloa_id);
  END IF;

  SET @obsolete_indexes = (SELECT GROUP_CONCAT(CONCAT('DROP INDEX `', REPLACE(INDEX_NAME,'`','``'), '`') SEPARATOR ', ')
    FROM (SELECT INDEX_NAME FROM information_schema.STATISTICS
      WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='ethazi_kiniela_lotura'
        AND NON_UNIQUE=0 AND INDEX_NAME<>'PRIMARY'
      GROUP BY INDEX_NAME
      HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)='adierazlea_id,emaitza_id') old_indexes);
  SET @repair_sql = IF(@obsolete_indexes IS NULL, 'SELECT 1',
    CONCAT('ALTER TABLE ethazi_kiniela_lotura ', @obsolete_indexes));
  PREPARE repair_stmt FROM @repair_sql;
  EXECUTE repair_stmt;
  DEALLOCATE PREPARE repair_stmt;
END//
DELIMITER ;

CALL ethazi_repair_kiniela_unique();
DROP PROCEDURE ethazi_repair_kiniela_unique;
