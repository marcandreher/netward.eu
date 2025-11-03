-- Create PowerDNS schema
CREATE TABLE IF NOT EXISTS domains (
  id                    INT AUTO_INCREMENT,
  name                  VARCHAR(255) NOT NULL,
  master                VARCHAR(128) DEFAULT NULL,
  last_check            INT DEFAULT NULL,
  type                  VARCHAR(8) NOT NULL,
  notified_serial       INT UNSIGNED DEFAULT NULL,
  account               VARCHAR(40) CHARACTER SET 'utf8' DEFAULT NULL,
  options               VARCHAR(64000) DEFAULT NULL,
  catalog               VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB CHARACTER SET 'latin1';

-- Safely recreate indexes
DROP INDEX IF EXISTS name_index ON domains;
DROP INDEX IF EXISTS catalog_idx ON domains;
CREATE UNIQUE INDEX name_index ON domains(name);
CREATE INDEX catalog_idx ON domains(catalog);


CREATE TABLE IF NOT EXISTS records (
  id                    BIGINT AUTO_INCREMENT,
  domain_id             INT DEFAULT NULL,
  name                  VARCHAR(255) DEFAULT NULL,
  type                  VARCHAR(10) DEFAULT NULL,
  content               VARCHAR(64000) DEFAULT NULL,
  ttl                   INT DEFAULT NULL,
  prio                  INT DEFAULT NULL,
  disabled              TINYINT(1) DEFAULT 0,
  ordername             VARCHAR(255) BINARY DEFAULT NULL,
  auth                  TINYINT(1) DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB CHARACTER SET 'latin1';

DROP INDEX IF EXISTS nametype_index ON records;
DROP INDEX IF EXISTS domain_id ON records;
DROP INDEX IF EXISTS ordername ON records;
CREATE INDEX nametype_index ON records(name, type);
CREATE INDEX domain_id ON records(domain_id);
CREATE INDEX ordername ON records(ordername);


CREATE TABLE IF NOT EXISTS supermasters (
  ip                    VARCHAR(64) NOT NULL,
  nameserver            VARCHAR(255) NOT NULL,
  account               VARCHAR(40) CHARACTER SET 'utf8' NOT NULL,
  PRIMARY KEY (ip, nameserver)
) ENGINE=InnoDB CHARACTER SET 'latin1';


CREATE TABLE IF NOT EXISTS comments (
  id                    INT AUTO_INCREMENT,
  domain_id             INT NOT NULL,
  name                  VARCHAR(255) NOT NULL,
  type                  VARCHAR(10) NOT NULL,
  modified_at           INT NOT NULL,
  account               VARCHAR(40) CHARACTER SET 'utf8' DEFAULT NULL,
  comment               TEXT CHARACTER SET 'utf8' NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB CHARACTER SET 'latin1';

DROP INDEX IF EXISTS comments_name_type_idx ON comments;
DROP INDEX IF EXISTS comments_order_idx ON comments;
CREATE INDEX comments_name_type_idx ON comments(name, type);
CREATE INDEX comments_order_idx ON comments(domain_id, modified_at);


CREATE TABLE IF NOT EXISTS domainmetadata (
  id                    INT AUTO_INCREMENT,
  domain_id             INT NOT NULL,
  kind                  VARCHAR(32),
  content               TEXT,
  PRIMARY KEY (id)
) ENGINE=InnoDB CHARACTER SET 'latin1';

DROP INDEX IF EXISTS domainmetadata_idx ON domainmetadata;
CREATE INDEX domainmetadata_idx ON domainmetadata(domain_id, kind);


CREATE TABLE IF NOT EXISTS cryptokeys (
  id                    INT AUTO_INCREMENT,
  domain_id             INT NOT NULL,
  flags                 INT NOT NULL,
  active                BOOL,
  published             BOOL DEFAULT 1,
  content               TEXT,
  PRIMARY KEY (id)
) ENGINE=InnoDB CHARACTER SET 'latin1';

DROP INDEX IF EXISTS domainidindex ON cryptokeys;
CREATE INDEX domainidindex ON cryptokeys(domain_id);


CREATE TABLE IF NOT EXISTS tsigkeys (
  id                    INT AUTO_INCREMENT,
  name                  VARCHAR(255),
  algorithm             VARCHAR(50),
  secret                VARCHAR(255),
  PRIMARY KEY (id)
) ENGINE=InnoDB CHARACTER SET 'latin1';

DROP INDEX IF EXISTS namealgoindex ON tsigkeys;
CREATE UNIQUE INDEX namealgoindex ON tsigkeys(name, algorithm);


-- Create PowerDNS Admin and application databases
CREATE DATABASE IF NOT EXISTS pdns_admin CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS netward CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- Grant permissions to pdns user from any host
GRANT ALL PRIVILEGES ON powerdns.* TO 'pdns'@'%';
GRANT ALL PRIVILEGES ON pdns_admin.* TO 'pdns'@'%';
GRANT ALL PRIVILEGES ON netward.* TO 'pdns'@'%';
FLUSH PRIVILEGES;
