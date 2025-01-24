(ns efactura-mea.web.logs
  (:require
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

(defn logs-list
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

(defn logs-api-calls
  [ds opts]
  (let [{:keys [cif]} opts
        t (str "Istoric apeluri api Anaf - cif " cif)]
    [:div#main-container.block
     (ui/title t)
     [:div#logs-table
      (logs-list ds opts)]]))

(defn handler-logs
  [req]
  (let [{:keys [path-params query-params ds ::r/router headers]} req
        {:strs [hx-request]} headers
        {:keys [cif]} path-params
        {:strs [page per-page]} query-params
        opts {:page page :per-page per-page :cif cif :router router}
        content (logs-api-calls ds opts)
        sidebar (ui/sidebar-company-data opts)
        body (if (= hx-request "true")
               (str (h/html content))
               (layout/main-layout content sidebar))]
    (-> (rur/response body)
        (rur/content-type "text/html"))))

