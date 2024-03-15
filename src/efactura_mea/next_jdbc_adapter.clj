(ns efactura-mea.next-jdbc-adapter
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]))

(defn set-next-jdbc-adapter
  "Set the HugSQL adapter for next.jdbc"
  []
  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc)))
