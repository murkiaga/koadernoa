-- Jokabide desegokien jakinarazpen elebidunak. MySQL 8.
-- Segurua da behin baino gehiagotan edo Hibernatek migrazioaren zati bat egin ondoren exekutatzea.
-- Zutabeak nullable dira aurretik gordetako erregistroek funtzionatzen jarrai dezaten.
DROP PROCEDURE IF EXISTS jokabide_elebitasuna_gehitu;
DELIMITER //
CREATE PROCEDURE jokabide_elebitasuna_gehitu()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='portaera_arrazoia' AND COLUMN_NAME='testua_es'
  ) THEN
    ALTER TABLE portaera_arrazoia ADD COLUMN testua_es VARCHAR(2000) NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='neurri_zuzentzailea' AND COLUMN_NAME='testua_es'
  ) THEN
    ALTER TABLE neurri_zuzentzailea ADD COLUMN testua_es VARCHAR(2000) NULL;
  END IF;

  -- TEXT erabiltzen da: bigarren VARCHAR(10000) batek MySQLen errenkada-tamainaren
  -- 65.535 byteko muga gaindi dezake utf8mb4 karaktere-jokoarekin.
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='jokabide_desegokia' AND COLUMN_NAME='deskribapen_zehatza_es'
  ) THEN
    ALTER TABLE jokabide_desegokia ADD COLUMN deskribapen_zehatza_es TEXT NULL;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='jokabide_desegokia' AND COLUMN_NAME='aplikatutako_neurria_es'
  ) THEN
    ALTER TABLE jokabide_desegokia ADD COLUMN aplikatutako_neurria_es TEXT NULL;
  END IF;
END//
DELIMITER ;

CALL jokabide_elebitasuna_gehitu();
DROP PROCEDURE jokabide_elebitasuna_gehitu;

-- Gaztelania hutsezko jakinarazpenak gorde ahal izateko.
ALTER TABLE jokabide_desegokia
  MODIFY COLUMN deskribapen_zehatza VARCHAR(10000) NULL;

-- 201/2008 Dekretuaren BOPVko gaztelaniazko testu ofizialak.
-- Euskarazko testua ez da ukitzen, erabiltzaileak pertsonalizatuta eduki dezakeelako.
UPDATE portaera_arrazoia
SET testua_es = 'La simple desobediencia a los profesores, profesoras o autoridades académicas cuando no comporte actitudes de menosprecio, insulto o indisciplina deliberada, así como no atender las indicaciones del resto del personal del centro docente en ejercicio de sus funciones.'
WHERE kodea = '30.d' AND (testua_es IS NULL OR TRIM(testua_es) = '');

UPDATE neurri_zuzentzailea
SET testua_es = 'Reflexión sobre la conducta inadecuada concreta y sus consecuencias.'
WHERE kodea = '34.a' AND (testua_es IS NULL OR TRIM(testua_es) = '');
