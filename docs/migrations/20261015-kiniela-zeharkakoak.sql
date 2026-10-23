-- Zeharkako Kiniela ziklo-mailakoa da eta ez du IE/modulu testuingururik.
CREATE TABLE IF NOT EXISTS ethazi_zeharkako_kiniela_lotura (
    id BIGINT NOT NULL AUTO_INCREMENT,
    adierazlea_id BIGINT NOT NULL,
    gaitasuna_id BIGINT NOT NULL,
    zikloa_id BIGINT NOT NULL,
    pisua DECIMAL(5,2) NOT NULL DEFAULT 0,
    oharra TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_zeharkako_kiniela UNIQUE (adierazlea_id, gaitasuna_id, zikloa_id),
    CONSTRAINT fk_zeharkako_kiniela_adierazlea FOREIGN KEY (adierazlea_id) REFERENCES ethazi_lorpen_adierazlea(id),
    CONSTRAINT fk_zeharkako_kiniela_gaitasuna FOREIGN KEY (gaitasuna_id) REFERENCES ethazi_gaitasuna(id),
    CONSTRAINT fk_zeharkako_kiniela_zikloa FOREIGN KEY (zikloa_id) REFERENCES zikloa(id)
);

CREATE TABLE IF NOT EXISTS ethazi_zeharkako_kiniela_erronka (
    lotura_id BIGINT NOT NULL,
    erronka_id BIGINT NOT NULL,
    CONSTRAINT uk_zeharkako_kiniela_erronka UNIQUE (lotura_id, erronka_id),
    CONSTRAINT fk_zeharkako_kiniela_erronka_lotura FOREIGN KEY (lotura_id) REFERENCES ethazi_zeharkako_kiniela_lotura(id),
    CONSTRAINT fk_zeharkako_kiniela_erronka FOREIGN KEY (erronka_id) REFERENCES ethazi_erronka(id)
);
