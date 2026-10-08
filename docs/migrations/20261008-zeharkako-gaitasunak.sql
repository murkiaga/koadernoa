-- Zeharkako gaitasunak ikastetxe-mailakoak dira eta bakoitzak bere eredua du.
ALTER TABLE ethazi_gaitasuna
    ADD COLUMN IF NOT EXISTS eredua_id BIGINT NULL;

ALTER TABLE ethazi_gaitasuna
    MODIFY COLUMN zikloa_id BIGINT NULL;

ALTER TABLE ethazi_mailakatze_eredua
    MODIFY COLUMN zikloa_id BIGINT NULL;

ALTER TABLE ethazi_gaitasuna
    ADD CONSTRAINT uk_ethazi_gaitasuna_eredua UNIQUE (eredua_id),
    ADD CONSTRAINT fk_ethazi_gaitasuna_eredua
        FOREIGN KEY (eredua_id) REFERENCES ethazi_mailakatze_eredua(id);
