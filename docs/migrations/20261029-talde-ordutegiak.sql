CREATE TABLE IF NOT EXISTS talde_ordutegia (
    id BIGINT NOT NULL AUTO_INCREMENT,
    taldea_id BIGINT NOT NULL,
    ikasturtea_id BIGINT NOT NULL,
    hasiera_data DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_talde_ordutegia_hasiera UNIQUE (taldea_id, ikasturtea_id, hasiera_data),
    CONSTRAINT fk_talde_ordutegia_taldea FOREIGN KEY (taldea_id) REFERENCES taldea(id),
    CONSTRAINT fk_talde_ordutegia_ikasturtea FOREIGN KEY (ikasturtea_id) REFERENCES ikasturtea(id)
);

CREATE TABLE IF NOT EXISTS talde_ordutegi_lerroa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    talde_ordutegia_id BIGINT NOT NULL,
    asteguna VARCHAR(20) NOT NULL,
    ordu_zenbakia INT NOT NULL,
    moduloa_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_talde_ordutegi_gelaxka UNIQUE (talde_ordutegia_id, asteguna, ordu_zenbakia),
    CONSTRAINT fk_talde_ordutegi_lerroa_ordutegia FOREIGN KEY (talde_ordutegia_id) REFERENCES talde_ordutegia(id) ON DELETE CASCADE,
    CONSTRAINT fk_talde_ordutegi_lerroa_moduloa FOREIGN KEY (moduloa_id) REFERENCES moduloa(id)
);
