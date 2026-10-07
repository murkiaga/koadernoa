-- MySQL 8. Errubrikako ebidentzia-adierazle lotura-taulan garapenean
-- sortutako bi zutabe-izendapenak bateratzen ditu. Segurua da behin baino
-- gehiagotan exekutatzea. Gelditu aplikazioa eta egin babeskopia lehenengo.

DELIMITER //
CREATE PROCEDURE ethazi_repair_errubrika_adierazle_columns()
BEGIN
  DECLARE compatible_columns INT DEFAULT 0;

  SELECT COUNT(*) INTO compatible_columns
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE()
    AND TABLE_NAME='ethazi_erronka_ebidentzia_adierazlea'
    AND COLUMN_NAME IN (
      'erronka_ebidentzia_id', 'lorpen_adierazleak_id',
      'ebidentzia_id', 'adierazlea_id'
    );

  IF compatible_columns=4 THEN
    UPDATE ethazi_erronka_ebidentzia_adierazlea
    SET erronka_ebidentzia_id=ebidentzia_id,
        lorpen_adierazleak_id=adierazlea_id
    WHERE (erronka_ebidentzia_id IS NULL OR erronka_ebidentzia_id=0
           OR lorpen_adierazleak_id IS NULL OR lorpen_adierazleak_id=0)
      AND ebidentzia_id IS NOT NULL AND ebidentzia_id<>0
      AND adierazlea_id IS NOT NULL AND adierazlea_id<>0;

    ALTER TABLE ethazi_erronka_ebidentzia_adierazlea
      MODIFY COLUMN ebidentzia_id BIGINT NULL,
      MODIFY COLUMN adierazlea_id BIGINT NULL;
  END IF;
END//
DELIMITER ;

CALL ethazi_repair_errubrika_adierazle_columns();
DROP PROCEDURE ethazi_repair_errubrika_adierazle_columns;
