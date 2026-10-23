-- Use only in the isolated ethazi_migration_test database, after the migration.
DELIMITER //
CREATE PROCEDURE assert_ethazi_migration()
BEGIN
  IF (SELECT COUNT(*) FROM ikaskuntza_emaitza)<>1 OR NOT EXISTS (
      SELECT 1 FROM ikaskuntza_emaitza WHERE id=10 AND eei_kodea='0423'
      AND deskribapena='EU testua' AND deskribapena_es='ES texto' AND deskribapena_en='EN text' AND moduloa_id IS NULL) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Outcome IDs/translations were not merged correctly';
  END IF;
  IF (SELECT COUNT(*) FROM ethazi_lorpen_adierazlea_ikaskuntza_emaitza)<>1
      OR (SELECT COUNT(*) FROM ethazi_kiniela_pisua WHERE emaitza_id=10 AND pisua=50)<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Links/weights were not remapped';
  END IF;
  IF (SELECT COUNT(*) FROM ethazi_kiniela_lotura WHERE emaitza_id=10)<>3
     OR NOT EXISTS(SELECT 1 FROM ethazi_kiniela_lotura WHERE moduloa_id=1 AND oharra='EU oharra')
     OR NOT EXISTS(SELECT 1 FROM ethazi_kiniela_lotura WHERE moduloa_id=2 AND oharra='ES nota')
     OR NOT EXISTS(SELECT 1 FROM ethazi_kiniela_lotura WHERE moduloa_id=3 AND oharra='EN note') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Module-specific notes lost';
  END IF;
  IF (SELECT COUNT(*) FROM ethazi_kiniela_lotura_erronka e JOIN ethazi_kiniela_lotura l ON l.id=e.lotura_id
      WHERE e.erronka_id=l.moduloa_id*100)<>3 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Challenge selections lost';
  END IF;
END//
DELIMITER ;
CALL assert_ethazi_migration();
DROP PROCEDURE assert_ethazi_migration;
SELECT 'Migration assertions passed' AS result;
