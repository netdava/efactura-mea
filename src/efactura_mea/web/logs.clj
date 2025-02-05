(ns efactura-mea.web.logs
  (:require
   [clojure.data.json :as json]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.web.layout :as layout]
   [efactura-mea.web.ui.componente :as ui]
   [efactura-mea.web.ui.pagination :as pagination]
   [efactura-mea.web.utils :as wu]
   [hiccup2.core :as h]
   [honey.sql :as sql]
   [malli.core :as m]
   [next.jdbc :as jdbc]
   [reitit.core :as r]
   [ring.util.response :as rur]))



(defn table-config [opts]
  (let [{:keys [page per-page url]} opts]
    {:locale true
     :sortMode "remote"
     :langs {:default
             {:pagination
              {:counter {:showing "pagina"
                         :of "/"
                         :rows "rows"
                         :pages "pagini"}}}}
     :ajaxURL url
     :columns [{:title "ID" :field "id" :width 60}
               {:formatter "" :width 80 :headerSort false}
               {:title "Cif" :field "cif" :width 120}
               {:title "Data apelare"  :field "data_apelare"}
               {:title "Url" :field "url"}
               {:title "Tip apel" :field "tip" :width 150}
               {:title "Cod răspuns" :field "status_code" :width 80}]
     :layout "fitColumns"
     :pagination true
     :paginationMode "remote"
     :paginationSize per-page
     :paginationInitialPage page
     :paginationCounter "pages"
     :paginationSizeSelector [20, 50, 100]
     :rowHeader {:headerSort false
                 :width 40
                 :frozen true
                 :formatter "rowSelection"
                 :titleFormatter "rowSelection"
                 :cellClick "function(e, cell){
                                   cell.getRow().toggleSelect();"}}))


(defn generate-table-script [opts]
  (let [{:keys [router cif page per-page]} opts
        url (wu/route-name->url router :efactura-mea.web.companii/get-logs {:cif cif})
        cfg-opts {:page page :per-page per-page :url url}
        cfg (json/write-str (table-config cfg-opts))]
    [:script
     (str "document.addEventListener('DOMContentLoaded', function() {
            var tableConfig = " cfg "
            tableConfig.columns[1].formatter = function(cell, formatterParams) { 
              
               var select = document.createElement('select');
                select.name = 'action';
                select.id = 'action';
                                     
                var option2 = document.createElement('option');
                option2.value = 'pdf';
                option2.textContent = 'pdf';
                select.appendChild(option2);

                var option3 = document.createElement('option');
                option3.value = 'zip';
                option3.textContent = 'zip';
                select.appendChild(option3);

                return select; };
            var table = new Tabulator('#logs-table', tableConfig)});")]))

(defn logs-api-calls
  [opts]
  (let [{:keys [cif]} opts
        t (str "Istoric apeluri api Anaf - cif " cif)]
    [:div#main-container.block
     [:div#example-table]
     (ui/title t)
     [:div#logs-table]
     (generate-table-script opts)]))

(defn handler-logs
  [req]
  (let [{:keys [path-params query-params ds ::r/router headers]} req
        {:strs [hx-request]} headers
        {:keys [cif]} path-params
        {:strs [page size]} query-params
        opts {:page page :per-page size :cif cif :router router}
        content (logs-api-calls opts)
        sidebar (ui/sidebar-company-data opts)
        body (if (= hx-request "true")
               (str (h/html content))
               (layout/main-layout content sidebar))]
    (-> (rur/response body)
        (rur/content-type "text/html"))))

(defn validate-anaf-api-calls-input
  [fetch-opts]
  (let [{:keys [columns]} fetch-opts
        enum-columns (apply vector :enum columns)
        validation-schema [:map
                           [:field {:optional true} enum-columns]
                           [:sort-order {:optional true} [:enum "asc" "desc"]]]
        valid? (m/validate (m/schema validation-schema) fetch-opts)]
    valid?))

(defn fetch-columns-names-from-db-table
  [ds table-name]
  (let [query-columns (sql/format {:raw (str "PRAGMA table_info(" table-name ")")})
        columns (jdbc/execute! ds query-columns)
        columns-names (mapv #(:name %) columns)]
    columns-names))

(defn handler-get-logs
  [req]
  (let [{:keys [path-params query-params ds]} req
        {:keys [cif]} path-params
        {:strs [page size]} query-params
        sort-field (or (get query-params "sort[0][field]") "id")
        sort-dir (or (get query-params "sort[0][dir]") "asc")
        count-logs (db/count-apeluri-anaf-logs ds cif)
        columns-names (fetch-columns-names-from-db-table ds "apeluri_api_anaf")
        fetch-opts {:columns columns-names
                    :field sort-field
                    :sort-order sort-dir
                    :table-name "apeluri_api_anaf"
                    :cif cif
                    :page page
                    :size size}
        valid-sort-params? (validate-anaf-api-calls-input fetch-opts)
        api-call-logs-from-tabulator (if valid-sort-params? (db/anaf-api-calls-sorted-results ds fetch-opts) [])
        total-pages (pagination/calculate-pages-number count-logs size)
        logs (for [log api-call-logs-from-tabulator]
               (let [{:keys [id cif data_apelare url tip status_code]} log]
                 {:id id
                  :cif cif
                  :data_apelare data_apelare
                  :url url
                  :tip tip
                  :status_code status_code}))
        response-data {:last_page total-pages
                       :data logs}
        logs->json (json/write-str response-data)]
    (-> (rur/response logs->json)
        (rur/content-type "application/json"))))

(comment
  (let [vs [:map
            [:field {:optional true} [:enum "id" "cif" "data_apelare" "url" "tip" "status_code" "response"]]
            [:sort-order {:optional true} [:enum "asc" "desc"]]]
        d {:columns ["id" "cif" "data_apelare" "url" "tip" "status_code" "response"], :field "id", :sort-order "asc", :table-name "apeluri_api_anaf", :cif 35586426, :page 1, :size 20}]
    (m/validate (m/schema vs) d))

  0)