-- Ziklo bakoitzeko ETHAZI gaitasun moten erreferentzia-pisu orokorrak.
ALTER TABLE zikloa
    ADD COLUMN IF NOT EXISTS ethazi_teknikoen_pisua DECIMAL(5,2) NULL,
    ADD COLUMN IF NOT EXISTS ethazi_zeharkakoen_pisua DECIMAL(5,2) NULL;

UPDATE zikloa
SET ethazi_teknikoen_pisua = COALESCE(ethazi_teknikoen_pisua, 70.00),
    ethazi_zeharkakoen_pisua = COALESCE(ethazi_zeharkakoen_pisua, 30.00);

ALTER TABLE zikloa
    MODIFY COLUMN ethazi_teknikoen_pisua DECIMAL(5,2) NOT NULL DEFAULT 70.00,
    MODIFY COLUMN ethazi_zeharkakoen_pisua DECIMAL(5,2) NOT NULL DEFAULT 30.00;
