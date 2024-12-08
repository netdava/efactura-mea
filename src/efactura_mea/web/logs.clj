(ns efactura-mea.web.logs
  (:require
   [efactura-mea.layout :as layout]
   [efactura-mea.ui.componente :as ui]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.ui.pagination :as pagination]
   [hiccup2.core :as h]))

(defn row-log-api-call
  [{:keys [id data_apelare url tip status_code]}]
  (h/html
   [:tr
    [:td.is-size-7 id]
    [:td.is-size-7 data_apelare]
    [:td.is-size-7 url]
    [:td.is-size-7 tip]
    [:td.is-size-7 status_code]]))

(defn logs-list
  [ds cif page per-page]
  (let [uri (str "/logs/" cif)
        count-logs (db/count-apeluri-anaf-logs ds cif)
        api-call-logs (db/fetch-apeluri-anaf-logs ds cif page per-page)
        total-pages (pagination/calculate-pages-number count-logs per-page)
        logs (for [c api-call-logs]
               (row-log-api-call c))]
    (h/html
     [:table.table.is-hoverable
      logs]
     (pagination/make-pagination total-pages page per-page uri))))

(defn logs-api-calls
  [ds opts]
  (let [{:keys [page per-page cif]} opts
        t (str "Istoric apeluri api Anaf - cif " cif)]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (str (h/html
                 [:div#main-container.block
                  (ui/title t)
                  [:div#logs-table
                   (logs-list ds cif page per-page)]]))}))

(defn handler-logs
  [req]
  (let [{:keys [path-params query-params ds uri headers]} req
        {:strs [page per-page]} query-params
        {:strs [hx-request]} headers
        cif (:cif path-params)
        opts {:page page :per-page per-page :uri uri :cif cif}
        content (logs-api-calls ds opts)
        sidebar (layout/sidebar-company-data opts)]
    (if (= hx-request "true")
      content
      (layout/main-layout (:body content) sidebar))))

