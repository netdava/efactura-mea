(ns efactura-mea.web.middleware
  (:require [efactura-mea.config :refer [conf]]
            [efactura-mea.db.ds :refer [ds]]))

(defn pagination-params-middleware 
  "Add default pagination to query params if no pagination is present:
   
   - page - integer default 1
   - per-page - integer default 20
   "
  [handler]
  (fn [request]
    (let [{:keys [query-params]} request
          {:strs [page per-page size]} query-params
          page (or (some-> page Integer/parseInt) 1)
          per-page (or (some-> per-page Integer/parseInt) 20)
          size (or (some-> size Integer/parseInt) 20)
          new-params (merge query-params {"page" page "per-page" per-page "size" size})]
      ;; Apelăm handler-ul cu request-ul modificat 
      (handler (assoc request :query-params new-params)))))

(defn wrap-app-config
  "Add config and datasource to requests"
  [handler]
  (fn [request]
    (let [updated-request (assoc request :conf conf :ds ds)]
      (handler updated-request))))
