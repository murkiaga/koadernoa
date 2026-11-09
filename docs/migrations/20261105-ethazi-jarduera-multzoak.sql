CREATE TABLE IF NOT EXISTS ethazi_erronka_jarduera_multzoa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    erronka_id BIGINT NOT NULL,
    izena VARCHAR(200) NOT NULL,
    ordena INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_jarduera_multzoa_erronka_izena UNIQUE (erronka_id, izena),
    CONSTRAINT fk_jarduera_multzoa_erronka FOREIGN KEY (erronka_id) REFERENCES ethazi_erronka (id) ON DELETE CASCADE
);

CREATE INDEX ix_jarduera_multzoa_erronka_ordena
    ON ethazi_erronka_jarduera_multzoa (erronka_id, ordena);

ALTER TABLE ethazi_erronka_kronograma_jarduera
    ADD COLUMN multzoa_id BIGINT NULL;

ALTER TABLE ethazi_erronka_kronograma_jarduera
    ADD CONSTRAINT fk_kronograma_jarduera_multzoa
        FOREIGN KEY (multzoa_id) REFERENCES ethazi_erronka_jarduera_multzoa (id) ON DELETE SET NULL;

CREATE INDEX ix_kronograma_jarduera_multzoa
    ON ethazi_erronka_kronograma_jarduera (multzoa_id);
