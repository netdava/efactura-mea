(ns efactura-mea.db.ds
  (:require
   [efactura-mea.config :refer [conf]]
   [hugsql.core :as hugsql]
   [hugsql.adapter.next-jdbc :as next-adapter] 
   [mount.core :as mount :refer [defstate]]
   [next.jdbc :as jdbc]))

(defn set-next-jdbc-adapter
  "Set the HugSQL adapter for next.jdbc"
  []
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))

(defstate ds :start
  (let [db-spec (:db-spec conf)]
    (set-next-jdbc-adapter)
    (jdbc/get-datasource db-spec)))
