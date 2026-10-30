-- Reference schema for MySQL. You do NOT need to run this: the app creates
-- the database and tables automatically (spring.jpa.hibernate.ddl-auto=update).
CREATE DATABASE IF NOT EXISTS reconengine;
USE reconengine;

CREATE TABLE IF NOT EXISTS transactions (
    id          VARCHAR(64)    PRIMARY KEY,
    utr         VARCHAR(64)    NOT NULL,
    gateway_amt DECIMAL(14, 2) NOT NULL,
    bank_amt    DECIMAL(14, 2) NOT NULL,
    mdr_fee     DECIMAL(14, 2) NOT NULL,
    status      VARCHAR(32)    NOT NULL,
    root_cause  VARCHAR(1000),
    updated_at  DATETIME(6)
);

CREATE TABLE IF NOT EXISTS app_users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    created_at    DATETIME(6)  NOT NULL
);
