(ns efactura-mea.config
  (:require
   [cprop.core :refer [load-config]]
   [mount.core :as mount :refer [defstate]]))

(defn download-dir
  [cfg]
  (str (:data-dir cfg) "/date"))

(defstate conf
  :start (load-config)
  :stop nil)

