(ns efactura-mea.util
  (:require [clojure.java.io :as io])
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

(comment
  (list-files-from-dir "facturi")
  (let [z-files (list-files-from-dir "facturi")]
    (doseq [z z-files]
      (println (walkzip z)))))