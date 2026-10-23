-- Erronka bakoitzeko zeharkako errubrika bakarra, moduluetatik independentea.
CREATE TABLE IF NOT EXISTS ethazi_zeharkako_errubrika (
    id BIGINT NOT NULL AUTO_INCREMENT, erronka_id BIGINT NOT NULL,
    heteroebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 85.00,
    koebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    autoebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 5.00,
    PRIMARY KEY (id), CONSTRAINT uk_zeharkako_errubrika_erronka UNIQUE (erronka_id),
    CONSTRAINT fk_zeharkako_errubrika_erronka FOREIGN KEY (erronka_id) REFERENCES ethazi_erronka(id)
);
CREATE TABLE IF NOT EXISTS ethazi_zeharkako_errubrika_maila (
    id BIGINT NOT NULL AUTO_INCREMENT, errubrika_id BIGINT NOT NULL, ordena INT NOT NULL, izena VARCHAR(120) NOT NULL,
    balioa DECIMAL(4,2) NOT NULL DEFAULT 0.00,
    PRIMARY KEY (id), CONSTRAINT uk_zeharkako_errubrika_maila_ordena UNIQUE (errubrika_id, ordena),
    CONSTRAINT fk_zeharkako_maila_errubrika FOREIGN KEY (errubrika_id) REFERENCES ethazi_zeharkako_errubrika(id)
);
CREATE TABLE IF NOT EXISTS ethazi_zeharkako_ebidentzia (
    id BIGINT NOT NULL AUTO_INCREMENT, errubrika_id BIGINT NOT NULL, adierazlea_id BIGINT NOT NULL,
    ordena INT NOT NULL, deskribapena TEXT NOT NULL, pisua DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    mota VARCHAR(40) NOT NULL DEFAULT 'IRAKASLEAK_SOILIK',
    PRIMARY KEY (id), CONSTRAINT uk_zeharkako_ebidentzia_ordena UNIQUE (errubrika_id, ordena),
    CONSTRAINT fk_zeharkako_ebidentzia_errubrika FOREIGN KEY (errubrika_id) REFERENCES ethazi_zeharkako_errubrika(id),
    CONSTRAINT fk_zeharkako_ebidentzia_adierazlea FOREIGN KEY (adierazlea_id) REFERENCES ethazi_lorpen_adierazlea(id)
);

ALTER TABLE zikloa
    ADD COLUMN IF NOT EXISTS ethazi_heteroebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 85.00,
    ADD COLUMN IF NOT EXISTS ethazi_koebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    ADD COLUMN IF NOT EXISTS ethazi_autoebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 5.00;
CREATE TABLE IF NOT EXISTS ethazi_zeharkako_ebidentzia_maila (
    id BIGINT NOT NULL AUTO_INCREMENT, ebidentzia_id BIGINT NOT NULL, maila_id BIGINT NOT NULL, deskribapena TEXT NOT NULL,
    PRIMARY KEY (id), CONSTRAINT uk_zeharkako_ebidentzia_maila UNIQUE (ebidentzia_id, maila_id),
    CONSTRAINT fk_zeharkako_ebidentzia_maila_ebidentzia FOREIGN KEY (ebidentzia_id) REFERENCES ethazi_zeharkako_ebidentzia(id),
    CONSTRAINT fk_zeharkako_ebidentzia_maila_maila FOREIGN KEY (maila_id) REFERENCES ethazi_zeharkako_errubrika_maila(id)
);
CREATE TABLE IF NOT EXISTS ethazi_zeharkako_ebidentzia_ebaluazioa (
    id BIGINT NOT NULL AUTO_INCREMENT, erronka_id BIGINT NOT NULL, moduloa_id BIGINT NOT NULL,
    ebidentzia_id BIGINT NOT NULL, ikaslea_id BIGINT NOT NULL, maila_id BIGINT NOT NULL, nota DECIMAL(5,2) NOT NULL,
    PRIMARY KEY (id), CONSTRAINT uk_zeharkako_ebidentzia_ebaluazioa UNIQUE (moduloa_id, ebidentzia_id, ikaslea_id),
    CONSTRAINT fk_zebe_erronka FOREIGN KEY (erronka_id) REFERENCES ethazi_erronka(id),
    CONSTRAINT fk_zebe_moduloa FOREIGN KEY (moduloa_id) REFERENCES moduloa(id),
    CONSTRAINT fk_zebe_ebidentzia FOREIGN KEY (ebidentzia_id) REFERENCES ethazi_zeharkako_ebidentzia(id),
    CONSTRAINT fk_zebe_ikaslea FOREIGN KEY (ikaslea_id) REFERENCES ikaslea(id),
    CONSTRAINT fk_zebe_maila FOREIGN KEY (maila_id) REFERENCES ethazi_zeharkako_errubrika_maila(id)
);

-- Garapeneko aurreko bertsio batek taulak dagoeneko sortu bazituen.
ALTER TABLE ethazi_zeharkako_errubrika
    ADD COLUMN IF NOT EXISTS heteroebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 85.00,
    ADD COLUMN IF NOT EXISTS koebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 10.00,
    ADD COLUMN IF NOT EXISTS autoebaluazio_pisua DECIMAL(5,2) NOT NULL DEFAULT 5.00;
ALTER TABLE ethazi_zeharkako_errubrika_maila
    ADD COLUMN IF NOT EXISTS balioa DECIMAL(4,2) NOT NULL DEFAULT 0.00;
ALTER TABLE ethazi_zeharkako_ebidentzia
    ADD COLUMN IF NOT EXISTS pisua DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN IF NOT EXISTS mota VARCHAR(40) NOT NULL DEFAULT 'IRAKASLEAK_SOILIK';
