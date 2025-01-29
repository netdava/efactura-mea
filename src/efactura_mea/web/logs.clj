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
   [ring.util.response :as rur]))

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
  (let [{:keys [page per-page url]} opts]
    {:locale true
     :langs {:default
             {:pagination
              {:counter {:showing "pagina"
                         :of "/"
                         :rows "rows"
                         :pages "pagini"}}}}
     :ajaxURL url
     :columns [{:title "ID" :field "id"}
               {:title "Cif" :field "cif"}
               {:title "Data apelare"  :field "data_apelare"}
               {:title "Url" :field "url"}
               {:title "Tip apel" :field "tip"}
               {:title "Cod răspuns" :field "status_code"}]
     :layout "fitColumns"
     :pagination true
     :paginationMode "remote"
     :paginationSize per-page
     :paginationInitialPage page
     :paginationCounter "pages"
     :paginationSizeSelector [20, 50, 100]}))


(defn generate-table-script [opts]
  (let [{:keys [router cif page per-page]} opts
        url (wu/route-name->url router :efactura-mea.web.companii/get-logs {:cif cif})
        url (str "http://localhost:8080" url)
        cfg-opts {:page page :per-page per-page :url url}
        cfg (json/write-str (table-config cfg-opts))]
    [:script
     (str "document.addEventListener('DOMContentLoaded', function() {
                                                  var table = new Tabulator('#logs-table', " cfg ")});")]))

(defn logs-api-calls
  [ds opts]
  (let [{:keys [cif]} opts
        t (str "Istoric apeluri api Anaf - cif " cif)]
    [:div#main-container.block
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
  (println "requestul nostru : " req)
  (let [{:keys [path-params query-params ds ::r/router headers]} req
        {:strs [hx-request]} headers
        {:keys [cif]} path-params
        _ (println "querryyyy: " query-params)
        {:strs [page size]} query-params
        count-logs (db/count-apeluri-anaf-logs ds cif)
        api-call-logs (db/fetch-apeluri-anaf-logs ds cif page size)
        total-pages (pagination/calculate-pages-number count-logs size)
        logs (for [log api-call-logs]
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

