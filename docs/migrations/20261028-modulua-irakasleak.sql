CREATE TABLE IF NOT EXISTS moduloa_irakaslea (
    moduloa_id BIGINT NOT NULL,
    irakaslea_id BIGINT NOT NULL,
    PRIMARY KEY (moduloa_id, irakaslea_id),
    CONSTRAINT fk_moduloa_irakaslea_moduloa
        FOREIGN KEY (moduloa_id) REFERENCES moduloa(id),
    CONSTRAINT fk_moduloa_irakaslea_irakaslea
        FOREIGN KEY (irakaslea_id) REFERENCES irakaslea(id),
    INDEX ix_moduloa_irakaslea_irakaslea (irakaslea_id)
);
