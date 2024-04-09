(ns efactura-mea.db.db-ops
  (:require [efactura-mea.db.facturi :as f])
  (:import [java.time ZonedDateTime]))


(defn fetch-cif [db id]
  (:cif (f/select-company-cif db {:id id})))

(defn fetch-access-token [db cif]
  (:access_token (f/select-access-token db {:cif cif})))

(defn fetch-facturi-descarcate [db]
  (f/select-lista-mesaje-descarcate db))

(defn create-sql-tables
  [ds]
  (f/create-facturi-anaf-table ds)
  (f/create-company-table ds)
  (f/create-tokens-table ds)
  (f/create-apeluri-api-anaf ds)
  (f/create-descarcare-lista-mesaje ds))

(defn scrie-factura->db [factura ds]
  (let [now (ZonedDateTime/now)
        {:keys [id data_creare tip cif id_solicitare detalii]} factura]
    (f/insert-row-factura ds {:data_descarcare now
                              :id_descarcare id
                              :cif cif
                              :tip tip
                              :detalii detalii
                              :data_creare data_creare
                              :id_solicitare id_solicitare})))


(defn log-api-calls [ds response tip-apel]
  (let [now (ZonedDateTime/now)
        uri (:uri (:request response))
        url (.toString uri)
        status (:status response)
        response (:body response)]
    (f/insert-row-apel-api ds {:data_apelare now
                               :url url
                               :tip tip-apel
                               :status_code status
                               :response response})))

(defn track-descarcare-mesaje [ds lista-mesaje]
  (let [now (ZonedDateTime/now)]
    (f/insert-into-descarcare-lista-mesaje ds {:data_start_procedura now
                                               :lista_mesaje lista-mesaje})))

