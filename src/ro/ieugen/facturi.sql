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
-- command :execute
-- :result :raw
select id from facturi_anaf where abstract_id = :id