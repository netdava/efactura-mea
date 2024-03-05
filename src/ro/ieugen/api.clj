(ns ro.ieugen.api
  (:require [babashka.http-client :as http]
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
  (let [f (io/file file-path)
        _ (doto (-> (.getParentFile f)
                    (.mkdirs)))]
    (with-open [output-stream (io/output-stream file-path)]
      (io/copy (io/input-stream (:body response)) output-stream))))

(defn obtine-lista-facturi 
  "Obtine lista de facturi pe o perioada de 60 zile din urmă;
   - funcționează doar cu endpointurile de test/prod :lista-mesaje"
  [url days cif]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)}}
        endpoint (format url days cif)
        r (http/get endpoint headers)
        l (:body r)
        lista-mesaje (json/read-str l :key-fn keyword)]
    lista-mesaje))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod :lista-mesaje"
  [url id]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
        endpoint (format url id)
        response (http/get endpoint headers)]
    (save-zip-file response (str "facturi-descarcate-zip/nas/" id ".zip"))))

(defn list-files-from-dir [dir]
  (let [directory (clojure.java.io/file dir)
        dir? #(.isDirectory %)]
    (map #(.getName %) ;; .getPath?!
         (filter (comp not dir?)
                 (tree-seq dir? #(.listFiles %) directory)))))

(defn already-downloaded? [file-name]
  (some #(= file-name %) '("3192491497.zip" "3182888140.zip")))


(comment
  
  (let [f "3192491497.zip"
        l (list-files-from-dir "facturi-descarcate-zip")]
    (already-downloaded? f))
  
  (let [url (get-in config [:endpoint :lista-mesaje :prod])
        cif (:cif config)]
    (obtine-lista-facturi url "60" cif))
  
  (let [
        url (get-in config [:endpoint :descarcare :prod])
        id "3182888140"]
    (descarca-factura url id))
  
  (let [a :prod]
    (get-in config [:endpoint :descarcare a]))
  )
