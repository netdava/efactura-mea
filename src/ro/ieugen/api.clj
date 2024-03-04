(ns ro.ieugen.api
  (:require [clj-http.client :as c]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn get-access-token [name]
  (let [env (System/getenv)
        access-token (get env name)]
    access-token))

(def config
  {:endpoint
   {:lista-mesaje
    {:test "https://api.anaf.ro/test/FCTEL/rest/listaMesajeFactura?zile=%s&cif=%s"
     :prod "https://api.anaf.ro/prod/FCTEL/rest/listaMesajeFactura?zile=%s&cif=%s"}
    :descarcare
    {:test "https://api.anaf.ro/test/FCTEL/rest/descarcare?id=%s"
     :prod "https://api.anaf.ro/prod/FCTEL/rest/descarcare?id=%s"}}
   :cif "35586426"})

(defn save-zip-file [response file-path]
  (with-open [output-stream (io/output-stream file-path)]
    (io/copy (io/input-stream (:body response)) output-stream)))

(defn obtine-lista-facturi 
  "Obtine lista de facturi pe o perioada de 60 zile din urmă;
   - funcționează doar cu endpointurile de test/prod :lista-mesaje"
  [url days cif]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)}}
        endpoint (format url days cif)
        r (c/get endpoint headers)
        l (:body r)
        lista-mesaje (json/read-str l)]
    lista-mesaje))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod :lista-mesaje"
  [url id]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)} :as :byte-array}
        endpoint (format url id)
        response (c/get endpoint headers)]
    (save-zip-file response (str id "-factura.zip"))))

(comment
  (let [url (get-in config [:endpoint :lista-mesaje :prod])
        cif (:cif config)]
    (obtine-lista-facturi url "60" cif))
  
  (let [url (get-in config [:endpoint :descarcare :prod])
        id "3192376881"]
    (descarca-factura url id))
  )