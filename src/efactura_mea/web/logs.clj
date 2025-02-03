(ns efactura-mea.web.logs
  (:require
   [clojure.data.json :as json]
   [efactura-mea.web.layout :as layout]
   [efactura-mea.web.ui.componente :as ui]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.web.ui.pagination :as pagination]
   [efactura-mea.web.utils :as wu]
   [hiccup2.core :as h]
   [reitit.core :as r]
   [ring.util.response :as rur]
   [efactura-mea.db.db-ops :as db-ops]))

(defn row-log-api-call
  [{:keys [id data_apelare url tip status_code]}]
  [:tr
   [:td.is-size-7 id]
   [:td.is-size-7 data_apelare]
   [:td.is-size-7 url]
   [:td.is-size-7 tip]
   [:td.is-size-7 status_code]])

#_(defn logs-list
    [ds opts]
    (let [{:keys [page per-page cif router]} opts
          uri (wu/route-name->url router :efactura-mea.web.companii/jurnal-spv {:cif cif})
          count-logs (db/count-apeluri-anaf-logs ds cif)
          api-call-logs (db/fetch-apeluri-anaf-logs ds cif page per-page)
          total-pages (pagination/calculate-pages-number count-logs per-page)
          logs (for [c api-call-logs]
                 (row-log-api-call c))]
      [:div
       [:table.table.is-hoverable
        logs]
       (pagination/make-pagination total-pages page per-page uri)]))

#_(defn logs-list
    [ds opts]
    (let [{:keys [page per-page cif]} opts
          count-logs (db/count-apeluri-anaf-logs ds cif)
          api-call-logs (db/fetch-apeluri-anaf-logs ds cif page per-page)
          total-pages (pagination/calculate-pages-number count-logs per-page)
          logs (for [log api-call-logs]
                 (let [{:keys [id cif data_apelare url tip status_code]} log]
                   {:id id
                    :cif cif
                    :data_apelare data_apelare
                    :url url
                    :tip tip
                    :status_code status_code}))]
      (println "api-call-loggssss " logs)
      logs))

(defn table-config [opts]
  (let [{:keys [page per-page url format-fn]} opts]
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
  [ds opts]
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
        content (logs-api-calls ds opts)
        sidebar (ui/sidebar-company-data opts)
        body (if (= hx-request "true")
               (str (h/html content))
               (layout/main-layout content sidebar))]
    (-> (rur/response body)
        (rur/content-type "text/html"))))

(defn handler-get-logs
  [req]
  (let [{:keys [path-params query-params ds]} req
        {:keys [cif]} path-params
        {:strs [page size sort]} query-params
        sort-field (get query-params "sort[0][field]")
        sort-dir (get query-params "sort[0][dir]")
        count-logs (db/count-apeluri-anaf-logs ds cif)
        fetch-opts {:field (keyword sort-field) 
                    :table-name :apeluri_api_anaf 
                    :sort-order (keyword sort-dir)
                    :cif cif
                    :page page
                    :size size}
        api-call-logs-from-tabulator (db-ops/fetch-sorted ds fetch-opts)
        _ (println "ceva cumva " api-call-logs-from-tabulator)
        api-call-logs (db/fetch-apeluri-anaf-logs ds cif page size)
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

