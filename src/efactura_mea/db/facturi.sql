-- :name create-facturi-anaf-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS lista_mesaje (
    id INTEGER PRIMARY KEY,
    data_descarcare TEXT,
    id_descarcare TEXT ,
    cif TEXT,
    tip TEXT,
    detalii TEXT,
    data_creare TEXT,
    id_solicitare TEXT
) STRICT;

-- :name create-detalii-facturi-anaf-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS detalii_facturi_anaf (
    id INTEGER PRIMARY KEY,
    id_descarcare text,
    id_solicitare text,
    data_creare TEXT,
    cif TEXT,
    tip TEXT ,
    serie_numar TEXT,
    data_emitere TEXT,
    data_scadenta TEXT,
    furnizor TEXT,
    client TEXT,
    total TEXT,
    valuta TEXT
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
    cif TEXT,
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

-- :name create-automated-processes-table
-- :command :execute
-- :result :raw
CREATE TABLE IF NOT EXISTS company_automated_proc (
    company_id INTEGER PRIMARY KEY,
    desc_aut_status TEXT,
    FOREIGN KEY(company_id) REFERENCES company(id) 
) STRICT;

-- :name insert-row-factura :insert :*
-- :command :execute
-- :result :raw
insert into lista_mesaje (
    data_descarcare,
    id_descarcare,
    cif,
    tip,
    detalii,
    data_creare,
    id_solicitare)
    values (:data_descarcare, :id_descarcare, :cif, :tip, :detalii, :data_creare, :id_solicitare)

-- :name insert-row-detalii-factura :insert :*
-- :command :execute
-- :result :raw
insert into detalii_facturi_anaf (
    id_descarcare,
    id_solicitare,
    data_creare,
    cif,
    tip,
    serie_numar,
    data_emitere,
    data_scadenta,
    furnizor,
    client,
    total,
    valuta)
    values (:id_descarcare, :id_solicitare, :data_creare, :cif, :tip, :serie_numar, :data_emitere, :data_scadenta, :furnizor, :client, :total, :valuta)

-- :name insert-row-apel-api :insert :*
-- :command :execute
-- :result :raw
insert into apeluri_api_anaf (
    cif,
    data_apelare,
    url,
    tip,
    status_code,
    response
    )
    values (:cif, :data_apelare, :url, :tip, :status_code, :response)

-- :name insert-into-company-automated-processes
-- :command :execute
-- :result :raw
insert OR IGNORE into company_automated_proc (
    company_id,
    desc_aut_status
) values (:company_id, :desc_aut_status);

-- :name update-automated-download-status
-- :command :execute
-- :result :raw
update company_automated_proc
set desc_aut_status = :status
where company_id = :id;

-- :name insert-row-apel-api-lista-mesaje :insert :*
-- :command :execute
-- :result :raw
insert into apeluri_api_anaf (
    cif,
    data_apelare,
    url,
    tip,
    status_code,
    response
    )
    values (:cif, :data_apelare, :url, :tip, :status_code, :response)

-- :name test-factura-descarcata? :? :*
-- :command :execute
-- :result :raw
select id from lista_mesaje where id_descarcare = :id

-- :name select-factura-descarcata :? :*
-- :command :execute
-- :result :raw
select * from lista_mesaje where id_descarcare = :id

-- :name select-detalii-factura-descarcata :? :*
-- :command :execute
-- :result :raw
select * from detalii_facturi_anaf where id_descarcare = :id

-- :name select-apeluri-api-anaf
-- :command :execute
-- :result :raw
select * from apeluri_api_anaf where cif = :cif

-- :name select-company-cif :? :1
-- :command :execute
-- :result :raw
select cif from company where id = :id

-- :name get-companies-data
-- :command :execute
-- :result :raw
select cif,id,name from company

-- :name get-company-data
-- :command :execute
-- :result :raw
SELECT company.id, company.cif, company_automated_proc.desc_aut_status
FROM company
INNER JOIN company_automated_proc
ON company.id = company_automated_proc.company_id
where company.cif = :cif;

-- :name select-access-token :? :1
-- :command :execute
-- :result :raw
select access_token from tokens where cif = :cif

-- :name select-queue-lista-mesaje :? :1
-- :command :execute
-- :result :raw
SELECT * FROM descarcare_lista_mesaje ORDER BY id DESC LIMIT 1

-- :name select-lista-mesaje-descarcate
SELECT * FROM lista_mesaje;

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

-- :name clear-download-queue
DELETE FROM descarcare_lista_mesaje WHERE id = :id