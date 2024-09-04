(ns efactura-mea.db.db-ops
  (:require [efactura-mea.db.facturi :as f]
            [java-time :as jt]
            [next.jdbc :as jdbc]))

(defn enable-foreignkey-constraint [spec]
  ;; Enable foreign key constraints
  (jdbc/execute! spec ["PRAGMA foreign_keys = ON"]))

(defn convert-to-wall-mode [spec]
  (jdbc/execute! spec ["PRAGMA journal_mode=WAL"]))

(defn db-init-pref [db-spec]
  (enable-foreignkey-constraint db-spec)
  (convert-to-wall-mode db-spec))


(defn fetch-cif [db id]
  (:cif (f/select-company-cif db {:id id})))

(defn fetch-access-token [db cif]
  (:access_token (f/select-access-token db {:cif cif})))

(defn fetch-apeluri-anaf-logs [db cif]
  (f/select-apeluri-api-anaf db {:cif cif}))

(defn init-automated-download [db]
  (let [companies (f/get-companies-data db)]
    (doseq [c companies]
      (let [id (:id c)]
        (f/insert-into-company-automated-processes db {:company_id id :desc_aut_status "off"})))))

(defn get-companies-data [ds]
  (f/get-companies-data ds))

(defn get-company-data [db cif]
  (first (f/get-company-data db {:cif cif})))

(defn companies-with-status [db]
  (let [companies (get-companies-data db)
        a (atom nil)]
    (doseq [c companies]
      (let [cif (:cif c)
            d (get-company-data db cif)]
        (swap! a conj d)))
    @a))

(defn detalii-factura-anaf 
  [db id]
  (println "iau detalii-factura-anaf pentru " id)
  (first
   (f/select-detalii-factura-descarcata db {:id id})))

(defn update-company-desc-aut-status [db id status]
  (f/update-automated-download-status db {:id id :status status}))

(defn test-companie-inregistrata
  [db cif]
  (let [c (f/test-companie-inregistrata? db {:cif cif})]
    (-> c
        first
        :exists
        (= 1))))

(defn create-sql-tables
  [ds]
  (db-init-pref ds)
  (f/create-facturi-anaf-table ds)
  (f/create-detalii-facturi-anaf-table ds)
  (f/create-company-table ds)
  (f/create-tokens-table ds)
  (f/create-apeluri-api-anaf ds)
  (f/create-descarcare-lista-mesaje ds)
  (f/create-automated-processes-table ds)
  (init-automated-download ds))

(defn scrie-factura->db [factura ds]
  (let [now (jt/zoned-date-time)
        {:keys [id data_creare tip cif id_solicitare detalii]} factura]
    (f/insert-row-factura ds {:data_descarcare now
                              :id_descarcare id
                              :cif cif
                              :tip tip
                              :detalii detalii
                              :data_creare data_creare
                              :id_solicitare id_solicitare})))

(defn scrie-detalii-factura-anaf->db [factura ds]
  (let [{:keys [id_descarcare id_solicitare data_creare cif tip serie_numar data_emitere data_scadenta furnizor client total valuta]} factura]
    (f/insert-row-detalii-factura ds {:id_descarcare id_descarcare
                                      :id_solicitare id_solicitare
                                      :data_creare data_creare
                                      :cif cif
                                      :tip tip
                                      :serie_numar serie_numar
                                      :data_emitere data_emitere
                                      :data_scadenta data_scadenta
                                      :furnizor furnizor
                                      :client client
                                      :total total
                                      :valuta valuta})))

(defn scrie-companie->db
  [ds cif name]
  (f/insert-company ds {:cif cif :name name}))


(defn log-api-calls [ds cif response tip-apel]
  (let [now (jt/zoned-date-time)
        uri (:uri (:request response))
        url (.toString uri)
        status (:status response)
        response (case tip-apel
                   "descarcare" (:headers response)
                   (:body response))]
    (f/insert-row-apel-api ds {:cif cif
                               :data_apelare now
                               :url url
                               :tip tip-apel
                               :status_code status
                               :response response})))

(defn track-descarcare-mesaje [ds lista-mesaje]
  (let [now (jt/zoned-date-time)]
    (f/insert-into-descarcare-lista-mesaje ds {:data_start_procedura now
                                               :lista_mesaje lista-mesaje})))

