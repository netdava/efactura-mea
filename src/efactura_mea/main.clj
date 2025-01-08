(ns efactura-mea.main
  (:require
   [efactura-mea.config :as config]
   [efactura-mea.db.ds :refer [ds]]
   [efactura-mea.web.api :as api]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.http-server]
   [efactura-mea.job-scheduler]
   [mount-up.core :as mu]
   [clojure.tools.logging :refer [info]]
   [mount.core :as mount])
  (:gen-class))

;; Log mount service up / down
(mu/on-upndown :info mu/log :before)

(defn -main 
  "Punct de intrare efactura-mea"
  [& args]
  (info "Starting Application Server")
  (mount/start)
  (db/db-config ds)
  (api/pornire-serviciu-descarcare-automata ds config/conf))
