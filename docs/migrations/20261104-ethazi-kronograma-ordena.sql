ALTER TABLE ethazi_erronka_kronograma_jarduera
    ADD COLUMN ordena INT NOT NULL DEFAULT 0;

CREATE INDEX ix_kronograma_jarduera_erronka_moduloa_ordena
    ON ethazi_erronka_kronograma_jarduera (erronka_id, moduloa_id, ordena);
