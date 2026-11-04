CREATE TABLE IF NOT EXISTS ethazi_koadernoa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    taldea_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_ethazi_koadernoa_taldea UNIQUE (taldea_id),
    CONSTRAINT fk_ethazi_koadernoa_taldea FOREIGN KEY (taldea_id) REFERENCES taldea (id)
);

INSERT INTO ethazi_koadernoa (taldea_id)
SELECT t.id
FROM taldea t
WHERE NOT EXISTS (
    SELECT 1 FROM ethazi_koadernoa ek WHERE ek.taldea_id = t.id
);
