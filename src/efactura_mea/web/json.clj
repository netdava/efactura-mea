(ns efactura-mea.web.json
  (:require [jsonista.core :as j]
            [muuntaja.core :as m]))

(def object-mapper (j/object-mapper {:pretty true}))

(def m (m/create (assoc m/default-options
                        :return :input-stream
                        :mapper object-mapper)))

