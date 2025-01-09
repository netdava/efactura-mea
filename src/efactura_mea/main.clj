(ns efactura-mea.main
  (:require
   [efactura-mea.config :as config]
   [efactura-mea.db.ds :refer [ds]]
   [efactura-mea.web.api :as api]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.systems :refer [web-app]]
   [clojure.tools.logging :refer [info]]
   [lambdaisland.cli :as cli]
   [mount.core :as mount])
  (:gen-class))

(defn cli-web-server
  "Pornește efacturier în modul serviciu web."
  [flags]
  (info "Starting Application Server")
  (mount/start web-app)
  (db/db-config ds)
  (api/pornire-serviciu-descarcare-automata ds config/conf))

(defn -main
  "Punct de intrare efactura-mea"
  [& args]
  (cli/dispatch*
   {:name "efacturier"
    :doc "Soluție de gestiune eFactura ANAF"
    :strict? true
    :commands ["server" {:command #'cli-web-server}]}
   args))


