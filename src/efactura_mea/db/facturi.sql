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
insert OR IGNORE into detalii_facturi_anaf (
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
    values (:cif, CURRENT_TIMESTAMP, :url, :tip, :status_code, :response)

-- :name insert-into-company-automated-processes
-- :command :execute
-- :result :raw
insert OR IGNORE into company_automated_proc (
    company_id,
    desc_aut_status,
    date_modified
) values (:company_id, :desc_aut_status, CURRENT_TIMESTAMP);

-- :name update-automated-download-status
-- :command :execute
-- :result :raw
update company_automated_proc
set desc_aut_status = :status,
    date_modified = CURRENT_TIMESTAMP
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
select * from apeluri_api_anaf where cif = :cif LIMIT :limit OFFSET :offset

-- :name select-facturi-descarcate
-- :command :execute
-- :result :raw
select * from lista_mesaje where cif = :cif LIMIT :limit OFFSET :offset

-- :name count-logs
-- :command :execute
-- :result :raw
select count(*) as total from apeluri_api_anaf where cif = :cif

-- :name select-company-cif :? :1
-- :command :execute
-- :result :raw
select cif from company where id = :id

-- :name get-companies-data
-- :command :execute
-- :result :raw
select cif,id,name,website,address from company

-- :name get-company-data
-- :command :execute
-- :result :raw
SELECT company.id, company.name, company.cif, company_automated_proc.desc_aut_status, company_automated_proc.date_modified, company.website, company.address
FROM company
INNER JOIN company_automated_proc
ON company.id = company_automated_proc.company_id
where company.cif = :cif;

-- :name select-access-token :? :1
-- :command :execute
-- :result :raw
select access_token from tokens where cif = :cif

-- :name select-acc-token-exp-date :? :1
-- :command :execute
-- :result :raw
select expiration_date from tokens where cif = :cif

-- :name select-queue-lista-mesaje :? :1
-- :command :execute
-- :result :raw
SELECT * FROM descarcare_lista_mesaje ORDER BY id DESC LIMIT 1

-- :name count-lista-mesaje-descarcate
SELECT count(*) as total FROM lista_mesaje where cif = :cif;

-- :name test-companie-inregistrata? :? :*
-- :command :execute
-- :result :raw
select exists (select 1 from company where cif = :cif) as "exists";


-- :name insert-company :insert :*
-- :command :execute
-- :result :raw
insert into company (
    cif,
    name,
    website,
    address)
    values (:cif, :name, :website, :address)

-- :name insert-company-tokens :insert :*
-- :command :execute
-- :result :raw
insert into tokens (
    cif,
    access_token,
    refresh_token,
    expires_in,
    expiration_date,
    _updated)
    values (:cif, :access_token, :refresh_token, :expires_in, :expiration_date, :updated)
    on conflict(cif) do update set 
    access_token=excluded.access_token,
    refresh_token=excluded.refresh_token,
    expires_in=excluded.expires_in,
    expiration_date=excluded.expiration_date,
    _updated=excluded._updated 

-- :name insert-into-descarcare-lista-mesaje :insert :*
-- :command :execute
-- :result :raw
insert into descarcare_lista_mesaje (
    data_start_procedura,
    lista_mesaje)
    values (:data_start_procedura, :lista_mesaje)

-- :name clear-download-queue
DELETE FROM descarcare_lista_mesaje WHERE id = :id

-- :name get-facturi-descarcate-by-id
SELECT id_descarcare 
FROM lista_mesaje 
WHERE id_descarcare IN (:v*:ids);


-- :name get-facturi-in-date-range
-- :command :execute
-- :result :raw
SELECT id_descarcare, data_creare, data_emitere, id_solicitare
FROM detalii_facturi_anaf
WHERE cif = :cif AND data_emitere >= :start-date AND data_emitere <= :end-date || '2359';

-- :name count-facturi-in-date-range
-- :command :execute
-- :result :raw
SELECT count(*) as total
FROM detalii_facturi_anaf
WHERE cif = :cif AND data_emitere >= :start-date AND data_emitere <= :end-date || '2359';