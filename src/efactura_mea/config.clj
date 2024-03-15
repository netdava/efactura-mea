(ns efactura-mea.config
  (:require [malli.core :as m]))

(def Configuration
  "Malli schema for configuration validation"
  [])

(defn download-dir
  [cfg]
  (str (:data-dir cfg) "/date"))

