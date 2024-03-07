(ns ro.ieugen.api
  (:require [babashka.http-client :as http]
            [clojure.java.io :as io]
            [jsonista.core :as j]
            [ro.ieugen.oauth2-anaf :refer [make-query-string]])
  (:import [java.util.zip ZipFile]))

(defn get-access-token [name]
  (let [env (System/getenv)
        access-token (get env name)]
    access-token))

(defn build-url
  "Build a url from a base and a opts-map {:endpoint <type>}
   - <type> can be :prod or :test;"
  [url-base opts]
  (let [type (:endpoint opts)]
    (format url-base (name type))))

(comment
  (build-url "https://api.anaf.ro/%s/FCTEL/rest/" {:endpoint :test}))

(defn save-zip-file [data file-path]
  (let [f (io/file file-path)
        _ (doto (-> (.getParentFile f)
                    (.mkdirs)))]
    (with-open [output-stream (io/output-stream file-path)]
      (io/copy (io/input-stream data) output-stream))))

(defn obtine-lista-facturi 
  "Obtine lista de facturi pe o perioada de 60 zile din urmă;
   - apeleaza mediul de :test din oficiu;
   - primeste un map {:endpoint <type>}, <type> poate fi :prod sau :test ."
  ([]
   (obtine-lista-facturi {:endpoint :test}))
  ([opts]
   (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
         headers {:headers {"Authorization" (str "Bearer " a-token)}}
         format-url "https://api.anaf.ro/%s/FCTEL/rest/listaMesajeFactura"
         base-url (build-url format-url opts)
         cif "35586426"
         q-str {"zile" "60"
                "cif" cif}
         endpoint (str base-url "?" (make-query-string q-str))
         r (http/get endpoint headers)
         body (:body r)
         object-mapper (j/object-mapper {:decode-key-fn true})
         lista-facturi (j/read-value body object-mapper)]
     lista-facturi)))

(comment
  (obtine-lista-facturi {:endpoint :prod}))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod;
   - opts un map {:endpoint <type>}, <type> poate fi :prod sau :test ."
  [id download-to opts]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
        format-url "https://api.anaf.ro/%s/FCTEL/rest/descarcare"
        base-url(build-url format-url opts)
        endpoint (str base-url "?id=" id)
        response (http/get endpoint headers)
        data (:body response)
        file-path (str download-to "/" id ".zip")]
    (save-zip-file data file-path)
    (println "Am descărcat" (str id ".zip") "pe calea" file-path)))

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
  (defn entries [zipfile]
    (enumeration-seq (.entries zipfile)))

  (defn walkzip [fileName]
    (with-open [z (ZipFile. fileName)]
      (doseq [e (entries z)]
        (println (.getName e)))))

  (is-file-in-dir? "3192491497.zip" "facturi-descarcate-zip")


  (descarca-factura "3182888140" "my-files/abc" {:endpoint :prod})

  (defn build-path [data-creare]
    (let [an (subs data-creare 0 4)
          luna (subs data-creare 4 6)]
      (str an "/" luna)))

  (defn download-zip-file [factura opts]
    (let [{:keys [id data_creare]} factura
          date-path (build-path data_creare)
          path (str "facturi/" date-path)]
      (descarca-factura id path opts)))

  (defn verifica-descarca-facturi [opts]
    (let [l (obtine-lista-facturi opts)
          facturi (:mesaje l)
          local-dir "facturi"]
      (doseq [f facturi]
        (let [zip-name (str (:id f) ".zip")]
          (if (is-file-in-dir? zip-name local-dir)
            (println "factura" zip-name "exista salvata local")
            (download-zip-file f opts))))))
  
  (verifica-descarca-facturi {:endpoint :prod})
  

  )
