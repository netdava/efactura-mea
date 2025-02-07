(ns efactura-mea.web.companii.facturi
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.db.facturi :as f]
   [efactura-mea.util :as util :refer [build-path get-invoice-data]]
   [efactura-mea.web.layout :as layout]
   [efactura-mea.web.ui.componente :as ui]
   [efactura-mea.web.ui.pagination :as pagination]
   [efactura-mea.web.utils :as wu]
   [hiccup2.core :as h]
   [reitit.core :as r]
   [ring.util.response :as rur]))

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
  [download-dir mesaje-cerute]
  (reduce
   (fn [acc mesaj]
     (let [{:keys [data_creare cif id_descarcare]} mesaj
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

(defn gather-invoices-data
  [p]
  (reduce
   (fn [acc invoice-path]
     (merge acc (combine-invoice-data invoice-path)))
   []
   p))

#_(defn opis-facturi-descarcate
  [facturi ds]
  (for [ff facturi]
    (when ff
      (let [{:keys [tip id_descarcare]} ff
            is-downloaded? (> (count (f/test-factura-descarcata? ds {:id id_descarcare})) 0)
            tip-factura (parse-tip-factura tip)
            invoice-details (assoc ff :tip tip-factura)
            _ (when is-downloaded? (db/scrie-detalii-factura-anaf->db invoice-details ds))]
        (ui/row-factura-descarcata-detalii invoice-details)))))

(defn opis-facturi-descarcate
  [facturi ds]
  (for [ff facturi]
    (when ff
      (let [{:keys [tip id_descarcare]} ff
            is-downloaded? (> (count (f/test-factura-descarcata? ds {:id id_descarcare})) 0)
            tip-factura (parse-tip-factura tip)
            invoice-details (assoc ff :tip tip-factura)
            _ (when is-downloaded? (db/scrie-detalii-factura-anaf->db invoice-details ds))]
        invoice-details))))

(defn sortare-facturi-data-creare
  [facturi]
  (sort #(compare (:data_emitere %2) (:data_emitere %1)) facturi))

#_(defn afisare-facturile-mele
  "Receives messages data, pagination details,
   return html table with pagination;"
  [mesaje ds opts]
  (let [{:keys [page per-page uri cif]} opts
        count-mesaje (db/count-lista-mesaje ds cif)
        facturi-sortate (sortare-facturi-data-creare mesaje)
        detalii->table-rows (opis-facturi-descarcate facturi-sortate ds)
        total-pages (pagination/calculate-pages-number count-mesaje per-page)
        table-with-pagination [:div (ui/tabel-facturi-descarcate detalii->table-rows)
                                    (pagination/make-pagination total-pages page per-page uri)]]
    table-with-pagination))

(defn afisare-facturile-mele
  "Receives messages data, pagination details,
   returns map with last_page and data"
  [mesaje ds opts]
  (let [{:keys [page per-page uri cif]} opts
        count-mesaje (db/count-lista-mesaje ds cif)
        facturi-sortate (sortare-facturi-data-creare mesaje)
        detalii->table-rows (opis-facturi-descarcate facturi-sortate ds)
        total-pages (pagination/calculate-pages-number count-mesaje per-page)]
    {:last_page total-pages
     :data detalii->table-rows}))

(defn fetch-facturi-descarcate
  [fetch-opts]
  (let [{:keys [ds cif size]} fetch-opts
        facturi-descarcate (db/fetch-sorted-lista-mesaje fetch-opts)
        items (db/count-lista-mesaje ds cif)
        total-pages (pagination/calculate-pages-number items size)]
    {:last_page total-pages
     :data facturi-descarcate}))

(defn facturi-tabulator-config [opts]
  (let [{:keys [page url size]} opts]
    {:locale true
     :langs {:default
             {:pagination
              {:counter {:showing "pagina"
                         :of "/"
                         :rows "rows"
                         :pages "pagini"}}}}
     :ajaxURL url
     :columns [{:title "Id descărcare" :field "id_solicitare"}
               {:title "Serie/număr" :field "serie_numar"}
               {:title "Data urcare SPV"  :field "data_creare"}
               {:title "emisă" :field "data_emitere"}
               {:title "scadentă" :field "data_scadenta"}
               {:title "furnizor" :field "furnizor"}
               {:title "client" :field "client"}
               {:title "valoare" :field "total"}
               {:title "monedă" :field "valuta"}
               {:title "tip" :field "tip"}]
     :layout "fitColumns"
     :sortMode "remote"
     :pagination true
     :paginationMode "remote"
     :paginationSize size
     :paginationInitialPage page
     :paginationCounter "pages"
     :paginationSizeSelector [20, 50, 100]}))


(defn facturi-tabulator [opts]
  (let [{:keys [router cif page per-page size]} opts
        url (wu/route-name->url router :efactura-mea.web.companii/endpoint-facturi {:cif cif})
        cfg-opts {:page page :per-page per-page :url url :size size}
        cfg (json/write-str (facturi-tabulator-config cfg-opts))]
    [:script
     (str "document.addEventListener('DOMContentLoaded', function() {
                                                  var table = new Tabulator('#facturile-mele', " cfg ")});")]))


(defn facturi-descarcate
  [opts]
  [:div#main-container.block
   (ui/title "Facturi descărcate local")
   [:div#facturile-mele]
   (facturi-tabulator opts)])

(defn handler-afisare-facturi-descarcate
  [req] 
  (let [{:keys [path-params query-params ds conf uri headers ::r/router]} req
        {:strs [page per-page size]} query-params
        {:strs [hx-request]} headers
        cif (:cif path-params)
        opts {:cif cif :page page :per-page per-page :uri uri :router router :size size}
        sidebar (ui/sidebar-company-data opts)
        body (if (= hx-request "true")
               (str (h/html (facturi-descarcate opts)))
               (layout/main-layout (facturi-descarcate opts) sidebar))]
    (-> (rur/response body)
        (rur/content-type "text/html"))))

(defn parse-facturile-mele
  [data-facturile-mele]
  (let [{:keys [data]} data-facturile-mele
        new-data (mapv (fn [f]
                         (let [{:keys [data_creare]} f
                               an (subs data_creare 0 4)
                               luna (subs data_creare 4 6)
                               zi (subs data_creare 6 8)
                               data_creare (str an "-" luna "-" zi)]
                           (assoc f :data_creare data_creare))) data)]
    (assoc data-facturile-mele :data new-data)))


(defn handler-facturi-descarcate
  [req]
  (let [{:keys [path-params query-params ds]} req
        {:strs [page size]} query-params
        cif (:cif path-params)
        order-by (or (get query-params "sort[0][field]") "data_emitere")
        sort-dir (or (get query-params "sort[0][dir]") "asc")
        fetch-opts {:ds ds :cif cif :size size :page page :order-by order-by :sort-dir sort-dir :table-name "detalii_facturi_anaf"}
        data-facturile-mele (fetch-facturi-descarcate fetch-opts)
        parsed-facturile-mele (parse-facturile-mele data-facturile-mele)
        facturile-mele->json (json/write-str parsed-facturile-mele)]
    (-> (rur/response facturile-mele->json)
        (rur/content-type "application/json"))))


(defn handler-lista-mesaje-spv
  [req]
  (let [{:keys [path-params query-params headers conf ds ::r/router]} req
        {:keys [cif]} path-params
        {:strs [page per-page]} query-params
        {:strs [hx-request]} headers
        opts {:cif cif :page page :per-page per-page :router router}
        mesaje-cerute (db/fetch-mesaje ds cif page per-page)
        mesaje (gather-invoices-data (add-path-for-download conf mesaje-cerute))
        content (afisare-facturile-mele mesaje ds opts)
        sidebar (ui/sidebar-company-data opts)]
    (if (= hx-request "true")
      content
      (layout/main-layout (:body content) sidebar))))

(defn handler-facturi-spv
  [req]
  (let [{:keys [path-params query-params ds uri headers ::r/router]} req
        {:strs [page per-page]} query-params
        cif (:cif path-params)
        opts {:cif cif :page page :per-page per-page :uri uri
              :router router}
        content (ui/facturi-spv opts ds)
        sidebar (ui/sidebar-company-data opts)
        body (layout/main-layout content sidebar)]
    (-> (rur/response body)
        (rur/content-type "text/html"))))



