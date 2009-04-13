-- MySQL SQL
CREATE TABLE `wikihelp` (
    `topic` VARCHAR(255) NOT NULL,
    `created_dttm` DATETIME NOT NULL,
    `created_oprid` VARCHAR(100) NOT NULL,
    `modified_dttm` DATETIME NOT NULL,
    `modified_oprid` VARCHAR(100) NOT NULL,
    `online_flag` SMALLINT NOT NULL,
    `text` MEDIUMTEXT NOT NULL
);

-- Oracle SQL
CREATE TABLE wikihelp (
    topic VARCHAR(255) NOT NULL,
    created_dttm DATE NOT NULL,
    created_oprid VARCHAR(100) NOT NULL,
    modified_dttm DATE NOT NULL,
    modified_oprid VARCHAR(100) NOT NULL,
    online_flag SMALLINT NOT NULL,
    text NCLOB NOT NULL
)
