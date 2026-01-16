CREATE TABLE FILES
(
    ID            BIGINT          NOT NULL AUTO_INCREMENT,
    NAME          VARCHAR(255)    NOT NULL COMMENT 'Base name of the file including extension',
    URI           TEXT            NOT NULL,
    CHECKSUM      CHAR(64) UNIQUE NOT NULL COMMENT 'SHA-256 checksum of the file',
    IDENTIFIED_AT TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (ID)
);