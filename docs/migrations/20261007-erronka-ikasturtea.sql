-- MySQL 8. Hibernatek ikasturtea_id zutabea sortu ondoren exekutatzeko.
-- Gelditu aplikazioa eta egin babeskopia lehenengo.
SET @active_year = (SELECT id FROM ikasturtea WHERE aktiboa=1 ORDER BY id DESC LIMIT 1);
UPDATE ethazi_erronka e
JOIN (
  SELECT CAST(LEFT(izena,4) AS UNSIGNED) AS hasiera_urtea, MAX(id) AS id
  FROM ikasturtea WHERE izena REGEXP '^[0-9]{4}'
  GROUP BY CAST(LEFT(izena,4) AS UNSIGNED)
) i ON i.hasiera_urtea = CASE WHEN MONTH(e.hasiera_data)>=9
     THEN YEAR(e.hasiera_data) ELSE YEAR(e.hasiera_data)-1 END
SET e.ikasturtea_id=i.id WHERE e.ikasturtea_id IS NULL;
UPDATE ethazi_erronka SET ikasturtea_id=@active_year WHERE ikasturtea_id IS NULL;

DELIMITER //
CREATE PROCEDURE ethazi_require_challenge_year()
BEGIN
  IF EXISTS (SELECT 1 FROM ethazi_erronka WHERE ikasturtea_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='There are challenges without an academic year and no active year could be assigned';
  END IF;
  ALTER TABLE ethazi_erronka MODIFY COLUMN ikasturtea_id BIGINT NOT NULL;
END//
DELIMITER ;
CALL ethazi_require_challenge_year();
DROP PROCEDURE ethazi_require_challenge_year;
