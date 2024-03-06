(ns ro.ieugen.api
  (:require [babashka.http-client :as http]
            [clojure.java.io :as io]
            [jsonista.core :as j]))

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
  (let [f (io/file file-path)
        _ (doto (-> (.getParentFile f)
                    (.mkdirs)))]
    (with-open [output-stream (io/output-stream file-path)]
      (io/copy (io/input-stream (:body response)) output-stream))))

(defn obtine-lista-facturi 
  "Obtine lista de facturi pe o perioada de 60 zile din urmă;
   - funcționează doar cu endpointurile de test/prod :lista-mesaje;
   - <environment> can be :prod or :test ."
  [environment]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)}}
        url (get-in config [:endpoint :lista-mesaje environment])
        days "60"
        cif (:cif config)
        endpoint (format url days cif)
        r (http/get endpoint headers)
        body (:body r)
        object-mapper (j/object-mapper {:decode-key-fn true})
        lista-facturi (j/read-value body object-mapper)]
    lista-facturi))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod :lista-mesaje;
   - <environment> can be :prod or :test ."
  [id download-to environment]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
        url (get-in config [:endpoint :descarcare environment])
        endpoint (format url id)
        response (http/get endpoint headers)]
    (save-zip-file response (str download-to "/" id ".zip"))))

(defn list-files-from-dir [dir]
  (let [directory (clojure.java.io/file dir)
        dir? #(.isDirectory %)]
    (map #(.getName %) ;; .getPath?!
         (filter (comp not dir?)
                 (tree-seq dir? #(.listFiles %) directory)))))

(defn is-file-in-dir? [file dir]
  (let [files (list-files-from-dir dir)]
    (some #(= file %) files)))


(comment
  (is-file-in-dir? "3192491497.zip" "facturi-descarcate-zip")

  (obtine-lista-facturi :prod)

  (descarca-factura "3182888140" "my-files/abc" :prod)
  )
