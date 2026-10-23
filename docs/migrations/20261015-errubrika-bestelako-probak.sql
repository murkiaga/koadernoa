ALTER TABLE ethazi_erronka_errubrika
    ADD COLUMN IF NOT EXISTS errubrika_pisua DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    ADD COLUMN IF NOT EXISTS bestelako_proben_pisua DECIMAL(5,2) NOT NULL DEFAULT 0.00;

CREATE TABLE IF NOT EXISTS ethazi_erronka_bestelako_proba (
    id BIGINT NOT NULL AUTO_INCREMENT, errubrika_id BIGINT NOT NULL, izena VARCHAR(200) NOT NULL,
    data DATE NOT NULL, pisua DECIMAL(5,2) NOT NULL, PRIMARY KEY (id),
    CONSTRAINT fk_bestelako_proba_errubrika FOREIGN KEY (errubrika_id) REFERENCES ethazi_erronka_errubrika(id)
);
CREATE TABLE IF NOT EXISTS ethazi_erronka_bestelako_proba_adierazlea (
    proba_id BIGINT NOT NULL, adierazlea_id BIGINT NOT NULL,
    CONSTRAINT uk_bestelako_proba_adierazlea UNIQUE (proba_id, adierazlea_id),
    CONSTRAINT fk_bestelako_proba_adierazlea_proba FOREIGN KEY (proba_id) REFERENCES ethazi_erronka_bestelako_proba(id),
    CONSTRAINT fk_bestelako_proba_adierazlea_adierazlea FOREIGN KEY (adierazlea_id) REFERENCES ethazi_lorpen_adierazlea(id)
);
CREATE TABLE IF NOT EXISTS ethazi_erronka_bestelako_proba_nota (
    id BIGINT NOT NULL AUTO_INCREMENT, proba_id BIGINT NOT NULL, ikaslea_id BIGINT NOT NULL,
    nota DECIMAL(4,2) NOT NULL, PRIMARY KEY (id),
    CONSTRAINT uk_bestelako_proba_ikasle_nota UNIQUE (proba_id, ikaslea_id),
    CONSTRAINT fk_bestelako_proba_nota_proba FOREIGN KEY (proba_id) REFERENCES ethazi_erronka_bestelako_proba(id),
    CONSTRAINT fk_bestelako_proba_nota_ikaslea FOREIGN KEY (ikaslea_id) REFERENCES ikaslea(id)
);
