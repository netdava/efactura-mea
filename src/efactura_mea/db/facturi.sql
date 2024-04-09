-- :name create-facturi-anaf-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS facturi_anaf (
    id INTEGER PRIMARY KEY,
    data_descarcare TEXT,
    id_descarcare TEXT ,
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

-- :name create-apeluri-api-anaf
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS apeluri_api_anaf (
    id INTEGER PRIMARY KEY,
    data_apelare TEXT,
    url  TEXT,
    tip TEXT,
    status_code INTEGER,
    response TEXT
) STRICT;

-- :name create-descarcare-lista-mesaje
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS descarcare_lista_mesaje (
    id INTEGER PRIMARY KEY,
    data_start_procedura TEXT,
    lista_mesaje TEXT
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

-- :name insert-row-apel-api :insert :*
-- :command :execute
-- :result :raw
insert into apeluri_api_anaf (
    data_apelare,
    url,
    tip,
    status_code,
    response
    )
    values (:data_apelare, :url, :tip, :status_code, :response)

-- :name insert-row-apel-api-lista-mesaje :insert :*
-- :command :execute
-- :result :raw
insert into apeluri_api_anaf (
    data_apelare,
    url,
    tip,
    status_code,
    response
    )
    values (:data_apelare, :url, :tip, :status_code, :response)

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

-- :name select-queue-lista-mesaje :? :1
-- :command :execute
-- :result :raw
SELECT * FROM descarcare_lista_mesaje ORDER BY id DESC LIMIT 1

-- :name select-lista-mesaje-descarcate
SELECT * FROM facturi_anaf;

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

-- :name insert-into-descarcare-lista-mesaje :insert :*
-- :command :execute
-- :result :raw
insert into descarcare_lista_mesaje (
    data_start_procedura,
    lista_mesaje)
    values (:data_start_procedura, :lista_mesaje)

-- :name delete-row-download-queue
DELETE FROM descarcare_lista_mesaje WHERE id = :id