-- Ebidentzia bakoitza taldeka edo pertsonalki kalifikatzeko euskarria.
ALTER TABLE ethazi_erronka_ebidentzia
    ADD COLUMN IF NOT EXISTS ebaluazio_mota VARCHAR(20) NOT NULL DEFAULT 'TALDEKAKOA';

CREATE TABLE IF NOT EXISTS ethazi_erronka_ebidentzia_ikasle_nota (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ebidentzia_id BIGINT NOT NULL,
    ikaslea_id BIGINT NOT NULL,
    maila_id BIGINT NULL,
    nota DECIMAL(5,2) NULL,
    ondo_egindakoak TEXT NULL,
    hobetu_beharrekoak TEXT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_erronka_ebidentzia_ikasle_nota UNIQUE (ebidentzia_id, ikaslea_id),
    CONSTRAINT fk_eein_ebidentzia FOREIGN KEY (ebidentzia_id) REFERENCES ethazi_erronka_ebidentzia(id),
    CONSTRAINT fk_eein_ikaslea FOREIGN KEY (ikaslea_id) REFERENCES ikaslea(id),
    CONSTRAINT fk_eein_maila FOREIGN KEY (maila_id) REFERENCES ethazi_erronka_errubrika_maila(id)
);

CREATE TABLE IF NOT EXISTS ethazi_erronka_errubrika_talde_oharra (
    id BIGINT NOT NULL AUTO_INCREMENT, errubrika_id BIGINT NOT NULL, taldea_id BIGINT NOT NULL, oharra TEXT NOT NULL,
    PRIMARY KEY (id), CONSTRAINT uk_errubrika_talde_oharra UNIQUE (errubrika_id, taldea_id),
    CONSTRAINT fk_eeto_errubrika FOREIGN KEY (errubrika_id) REFERENCES ethazi_erronka_errubrika(id),
    CONSTRAINT fk_eeto_taldea FOREIGN KEY (taldea_id) REFERENCES ethazi_erronka_taldea(id)
);

CREATE TABLE IF NOT EXISTS ethazi_erronka_errubrika_ikasle_oharra (
    id BIGINT NOT NULL AUTO_INCREMENT, errubrika_id BIGINT NOT NULL, ikaslea_id BIGINT NOT NULL, oharra TEXT NOT NULL,
    PRIMARY KEY (id), CONSTRAINT uk_errubrika_ikasle_oharra UNIQUE (errubrika_id, ikaslea_id),
    CONSTRAINT fk_eeio_errubrika FOREIGN KEY (errubrika_id) REFERENCES ethazi_erronka_errubrika(id),
    CONSTRAINT fk_eeio_ikaslea FOREIGN KEY (ikaslea_id) REFERENCES ikaslea(id)
);
