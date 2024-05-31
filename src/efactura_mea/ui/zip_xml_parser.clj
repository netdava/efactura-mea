(ns efactura-mea.ui.zip-xml-parser
  (:require [efactura-mea.util :refer [list-files-from-dir]]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [clojure.walk :refer [postwalk]])
  (:import (java.util.zip ZipFile ZipInputStream)
           (java.io InputStreamReader FileInputStream )))



(defn entries [zipfile]
  (enumeration-seq (.entries zipfile)))

(defn zipfile->factura-xml [file]
  (let [a (atom nil)]
    (with-open [z (ZipFile. file)]
    (doseq [e (entries z)]
      (let [entry-name (.getName e)]
        (when
         (not (.contains entry-name "semnatura_"))
          (let [entry (.getEntry z entry-name)]
            (with-open [input-stream (.getInputStream z entry)]
              (let [data-xml (xml/parse input-stream)]
                (println data-xml))))))))
    @a))

(comment
  (let [path "/home/nas/proiecte/efactura-mea/data/date/35586426/2024/04/3370038663.zip"]
    (zipfile->factura-xml path))
  (concat nil [1 2])
  0)

(defn parse-xml [zip-file entry-name]
  (with-open [input-stream (.getInputStream (.getEntry zip-file entry-name))]

    (println "Parsing" entry-name)))


#_(defn zipfile->factura-xml [file]
  (with-open [z (ZipFile. file)]
    (for [e (doall (entries z))]
      (let [entry-name (.getName e)]
        (when
         (not (.contains entry-name "semnatura_"))
          (let [entry (.getEntry z entry-name)
                input-stream (.getInputStream z entry)
                data-xml (xml/parse input-stream)]
            data-xml))))))







