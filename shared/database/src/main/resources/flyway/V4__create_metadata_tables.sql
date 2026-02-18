-- ============================================================
-- 1) METADATA (HOVEDTABELL)
-- Én rad per (SOURCE, SOURCE_ID)
-- ============================================================

CREATE TABLE METADATA
(
    ID           BIGINT       NOT NULL AUTO_INCREMENT,

    SOURCE       VARCHAR(32)  NOT NULL,
    SOURCE_ID   VARCHAR(64)  NOT NULL,

    TITLE        VARCHAR(512) NOT NULL,
    COVER        VARCHAR(1024),
    BANNER_IMAGE VARCHAR(1024),

    MEDIA_TYPE   VARCHAR(32)  NOT NULL,

    LAST_UPDATED TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (ID),
    UNIQUE (SOURCE, SOURCE_ID)
);

-- ============================================================
-- 2) METADATA_TITLES (ALTERNATIVE TITLER)
-- ============================================================

CREATE TABLE METADATA_TITLES
(
    ID          BIGINT       NOT NULL AUTO_INCREMENT,
    METADATA_ID BIGINT       NOT NULL,
    TITLE       VARCHAR(512) NOT NULL,

    PRIMARY KEY (ID),
    FOREIGN KEY (METADATA_ID) REFERENCES METADATA (ID) ON DELETE CASCADE
);

-- ============================================================
-- 3) METADATA_SUMMARIES (SUMMARY PER SPRÅK)
-- ============================================================

CREATE TABLE METADATA_SUMMARIES
(
    ID          BIGINT      NOT NULL AUTO_INCREMENT,
    METADATA_ID BIGINT      NOT NULL,
    LANGUAGE    VARCHAR(16) NOT NULL,
    DESCRIPTION TEXT        NOT NULL,

    PRIMARY KEY (ID),
    FOREIGN KEY (METADATA_ID) REFERENCES METADATA (ID) ON DELETE CASCADE
);

-- ============================================================
-- 4) METADATA_GENRES (GENRELISTE)
-- ============================================================

CREATE TABLE METADATA_GENRES
(
    ID          BIGINT       NOT NULL AUTO_INCREMENT,
    METADATA_ID BIGINT       NOT NULL,
    GENRE       VARCHAR(128) NOT NULL,

    PRIMARY KEY (ID),
    FOREIGN KEY (METADATA_ID) REFERENCES METADATA (ID) ON DELETE CASCADE
);
