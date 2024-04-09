(ns efactura-mea.util
  (:require [clojure.java.io :as io]
            [jsonista.core :as j])
  (:import (java.util.zip ZipFile)))

(defn list-files-from-dir [dir]
  (let [directory (io/file dir)
        dir? #(.isDirectory %)]
    (map #(.getPath %)
         (filter (comp not dir?)
                 (tree-seq dir? #(.listFiles %) directory)))))

(defn is-file-in-dir? [file dir]
  (let [files (list-files-from-dir dir)]
    (some #(= file %) files)))
(defn entries [zipfile]
  (enumeration-seq (.entries zipfile)))

(defn walkzip [fileName]
  (with-open [z (ZipFile. fileName)]
    (doseq [e (entries z)]
      (println (.getName e)))))

(defn build-url
  "Build a url from a base and a target {:endpoint <type>}
   - <type> can be :prod or :test;"
  [url-base target]
  (let [type (:endpoint target)]
    (format url-base (name type))))

(defn build-path [data-creare]
  (let [an (subs data-creare 0 4)
        luna (subs data-creare 4 6)]
    (str an "/" luna)))

(defn parse-date [date]
  (let [an (subs date 0 4)
        luna (subs date 4 6)
        zi (subs date 6 8)
        ora (subs date 8 10)
        min (subs date 10 12)
        data-creare (str zi "." luna "." an)
        ora-creare (str ora ":" min)]
    {:data_c data-creare
     :ora_c ora-creare}))

(defn json-encode->edn [json-str]
  (-> json-str
      (j/write-value-as-bytes j/default-object-mapper)
      (j/read-value j/keyword-keys-object-mapper)))

(defn encode-body-json->edn [body]
  (let [object-mapper (j/object-mapper {:decode-key-fn true})
        response (j/read-value body object-mapper)]
    response))

(defn encode-request-params->edn [req]
  (let [q (:query-params req)
        edn-q-params (json-encode->edn q)]
    edn-q-params))
