(ns efactura-mea.db.ds
  (:require
   [efactura-mea.config :refer [conf]]
   [efactura-mea.db.migrations :as m]
   [hugsql.adapter.next-jdbc :as next-adapter]
   [hugsql.core :as hugsql]
   [mount.core :as mount :refer [defstate]]
   [next.jdbc :as jdbc]))

(defn set-next-jdbc-adapter
  "Set the HugSQL adapter for next.jdbc"
  []
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))

(defstate ds
  :start (let [db-spec (:db-spec conf)
               migratus-cfg (:migratus conf)
               _ (set-next-jdbc-adapter)
               ds (jdbc/get-datasource db-spec)
               _ (m/migrate migratus-cfg)]
           ds))
