CREATE TABLE IF NOT EXISTS ethazi_erronka_kronograma_jarduera (
    id BIGINT NOT NULL AUTO_INCREMENT,
    erronka_id BIGINT NOT NULL,
    mota VARCHAR(20) NOT NULL,
    moduloa_id BIGINT NULL,
    aurreikusitako_moduloa_id BIGINT NULL,
    izenburua VARCHAR(200) NOT NULL,
    deskribapena TEXT NULL,
    hasiera_data DATE NOT NULL,
    bukaera_data DATE NOT NULL,
    iraupen_orduak DECIMAL(7,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_kronograma_jarduera_erronka FOREIGN KEY (erronka_id) REFERENCES ethazi_erronka(id) ON DELETE CASCADE,
    CONSTRAINT fk_kronograma_jarduera_moduloa FOREIGN KEY (moduloa_id) REFERENCES moduloa(id),
    CONSTRAINT fk_kronograma_jarduera_aurreikusitako_moduloa FOREIGN KEY (aurreikusitako_moduloa_id) REFERENCES moduloa(id),
    CONSTRAINT ck_kronograma_jarduera_datak CHECK (bukaera_data >= hasiera_data),
    CONSTRAINT ck_kronograma_jarduera_iraupena CHECK (iraupen_orduak > 0),
    CONSTRAINT ck_kronograma_jarduera_mota_moduloa CHECK (
        (mota = 'MODULUA' AND moduloa_id IS NOT NULL AND aurreikusitako_moduloa_id IS NULL)
        OR (mota = 'KOMUNA' AND moduloa_id IS NULL)
    ),
    INDEX ix_kronograma_jarduera_erronka_data (erronka_id, hasiera_data, bukaera_data),
    INDEX ix_kronograma_jarduera_moduloa (moduloa_id)
);
