(ns ro.ieugen.sqlite-import
  (:require [next.jdbc :as jdbc]))


(def db-spec {:dbtype "sqlite"
              :dbname "facturi-anaf.db"})

(def ds (jdbc/get-datasource db-spec))


(defn create-table-facturi-anaf []
  (jdbc/execute! ds
                 ["CREATE TABLE facturi_anaf (
                   id INTEGER PRIMARY KEY,
                   abstract_id TEXT,
                   data_creare TEXT,
                   tip TEXT,
                   cif TEXT,
                   id_solicitare TEXT,
                   detalii TEXT
                   )"]))

(comment
  (create-table-facturi-anaf))
