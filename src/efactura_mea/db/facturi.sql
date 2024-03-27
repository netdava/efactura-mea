-- :name create-facturi-anaf-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS facturi_anaf (
    id INTEGER,
    data_descarcare TEXT,
    id_descarcare TEXT PRIMARY KEY,
    cif TEXT,
    tip TEXT,
    detalii TEXT,
    data_creare TEXT,
    id_solicitare TEXT
) STRICT;

-- :name create-company-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS company (
    id INTEGER PRIMARY KEY,
    cif TEXT,
    name TEXT
) STRICT;

-- :name create-tokens-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS tokens (
    id INTEGER PRIMARY KEY,
    cif TEXT,
    access_token TEXT,
    refresh_token TEXT,
    expiration_date TEXT,
    FOREIGN KEY (cif) REFERENCES company(cif)
) STRICT;


-- :name insert-row-factura :insert :*
-- :command :execute
-- :result :raw
insert into facturi_anaf (
    data_descarcare,
    id_descarcare,
    cif,
    tip,
    detalii,
    data_creare,
    id_solicitare)
    values (:data_descarcare, :id_descarcare, :cif, :tip, :detalii, :data_creare, :id_solicitare)

-- :name test-factura-descarcata? :? :*
-- :command :execute
-- :result :raw
select id from facturi_anaf where id_descarcare = :id

-- :name select-company-cif :? :1
-- :command :execute
-- :result :raw
select cif from company where id = :id

-- :name select-access-token :? :1
-- :command :execute
-- :result :raw
select access_token from tokens where cif = :cif

-- :name insert-company :insert :*
-- :command :execute
-- :result :raw
insert into company (
    cif,
    name)
    values (:cif, :name)

-- :name insert-company-tokens :insert :*
-- :command :execute
-- :result :raw
insert into tokens (
    cif,
    access_token,
    refresh_token,
    expiration_date)
    values (:cif, :access_token, :refresh_token, :expiration_date)

