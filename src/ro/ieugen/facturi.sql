-- :name create-facturi-anaf-table
-- :command :execute
-- :result :raw
create table facturi_anaf (
    id INTEGER PRIMARY KEY,
    abstract_id TEXT,
    data_creare TEXT,
    tip TEXT,
    cif TEXT,
    id_solicitare TEXT,
    detalii TEXT  
)

-- :name create-company-table
-- :command :execute
-- :result :raw
CREATE TABLE company (
    id INTEGER PRIMARY KEY,
    cif TEXT,
    name TEXT
);

-- :name create-tokens-table
-- :command :execute
-- :result :raw
CREATE TABLE tokens (
    id INTEGER PRIMARY KEY,
    cif TEXT,
    access_token TEXT,
    refresh_token TEXT,
    expiration_date TEXT,
    FOREIGN KEY (cif) REFERENCES company(cif)
);


-- :name insert-row-factura :insert :*
-- :command :execute
-- :result :raw
insert into facturi_anaf (
    abstract_id,
    data_creare,
    tip,
    cif,
    id_solicitare,
    detalii)
    values (:id, :data_creare, :tip, :cif, :id_solicitare, :detalii)

-- :name test-factura-descarcata? :? :*
-- :command :execute
-- :result :raw
select id from facturi_anaf where abstract_id = :id

-- :name select-company-cif :? :1
-- :command :execute
-- :result :raw
select cif from company where id = :id

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

