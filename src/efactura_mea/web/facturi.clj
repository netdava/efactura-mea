(ns efactura-mea.web.facturi
  (:require
   [efactura-mea.layout :as layout]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.util :as util :refer [build-path get-invoice-data]]
   [efactura-mea.config :as config]
   [efactura-mea.ui.pagination :as pagination]
   [efactura-mea.ui.componente :as ui]
   [efactura-mea.db.facturi :as f]
   [hiccup2.core :as h]
   [clojure.string :as str]))

(defn parse-tip-factura [tip-factura]
  (let [tip (str/lower-case tip-factura)]
    (case tip
      "factura primita" "primita"
      "factura trimisa" "trimisa"
      "erori factura" "eroare"
      tip)))

(defn add-path-for-download
  "Primeste lista de mesaje descarcate pentru afisare in UI
   Genereaza pentru fiecare mesaj calea unde a fost descarcat,
   pentru identificare si extragere meta-date."
  [conf mesaje-cerute]
  (reduce (fn [acc mesaj]
            (let [download-dir (config/download-dir conf)
                  {:keys [data_creare cif id_descarcare]} mesaj
                  p (str download-dir "/" cif "/")
                  date-path (build-path data_creare)
                  download-to (str p date-path "/" id_descarcare ".zip")
                  updated-path (merge mesaj {:download-path download-to})]
              (conj acc updated-path)))
          []
          mesaje-cerute))

(defn combine-invoice-data [detalii-anaf-pt-o-factura]
  (let [{:keys [download-path]} detalii-anaf-pt-o-factura
        detalii-xml (get-invoice-data download-path)
        detalii-xml (assoc detalii-xml :href download-path)]
    (merge detalii-anaf-pt-o-factura detalii-xml)))

(defn gather-invoices-data [p]
  (reduce (fn [acc invoice-path]
            (merge acc (combine-invoice-data invoice-path)))
          []
          p))

(defn opis-facturi-descarcate [facturi ds]
  (for [f facturi]
    (when f (let [{:keys [tip id_descarcare]} f
                  is-downloaded? (> (count (f/test-factura-descarcata? ds {:id id_descarcare})) 0)
                  tip-factura (parse-tip-factura tip)
                  invoice-details (assoc f :tip tip-factura)
                  _ (when is-downloaded? (db/scrie-detalii-factura-anaf->db invoice-details ds))]
              (ui/row-factura-descarcata-detalii invoice-details)))))

(defn sortare-facturi-data-creare
  [facturi]
  (sort #(compare (:data_emitere %2) (:data_emitere %1)) facturi))

(defn afisare-facturile-mele
  "Receives messages data, pagination details,
   return html table with pagination;"
  [mesaje ds opts]
  (let [{:keys [page per-page uri cif]} opts
        count-mesaje (db/count-lista-mesaje ds cif)
        facturi-sortate (sortare-facturi-data-creare mesaje)
        detalii->table-rows (opis-facturi-descarcate facturi-sortate ds)
        total-pages (pagination/calculate-pages-number count-mesaje per-page)
        table-with-pagination (h/html
                               (ui/tabel-facturi-descarcate detalii->table-rows)
                               (pagination/make-pagination total-pages page per-page uri))]
    table-with-pagination))

(defn handler-afisare-facturi-descarcate
  [req]
  (let [{:keys [path-params query-params ds conf uri headers]} req
        {:strs [page per-page]} query-params
        {:strs [hx-request]} headers
        cif (:cif path-params)
        opts {:cif cif :page page :per-page per-page :uri uri}
        mesaje-cerute (db/fetch-mesaje ds cif page per-page)
        mesaje (gather-invoices-data (add-path-for-download conf mesaje-cerute))
        table-with-pagination (afisare-facturile-mele mesaje ds opts)
        content (ui/facturi-descarcate table-with-pagination)
        sidebar (layout/sidebar-company-data opts)]
    (if (= hx-request "true")
      content
      (layout/main-layout (:body content) sidebar))))

(defn handler-lista-mesaje-spv
  [req]
  (let [{:keys [path-params query-params headers conf ds]} req
        {:keys [cif]} path-params
        {:strs [page per-page]} query-params
        {:strs [hx-request]} headers
        opts {:cif cif :page page :per-page per-page}
        mesaje-cerute (db/fetch-mesaje ds cif page per-page)
        mesaje (gather-invoices-data (add-path-for-download conf mesaje-cerute))
        content (afisare-facturile-mele mesaje ds opts)
        sidebar (layout/sidebar-company-data opts)]
    (if (= hx-request "true")
      content
      (layout/main-layout (:body content) sidebar))))

(defn handler-facturi-spv
  [req]
  (let [{:keys [path-params query-params ds uri headers]} req
        {:strs [page per-page]} query-params
        {:strs [hx-request]} headers
        cif (:cif path-params)
        opts {:cif cif :page page :per-page per-page :uri uri}
        content (ui/facturi-spv opts ds)
        sidebar (layout/sidebar-company-data opts)]
    (if (= hx-request "true")
      content
      (layout/main-layout (:body content) sidebar))))

