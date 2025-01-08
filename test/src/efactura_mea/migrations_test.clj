(ns efactura-mea.migrations-test
  (:require
   [babashka.fs :as fs]
   [clojure.test :refer [deftest is run-tests testing]]
   [clojure.tools.logging :as log]
   [efactura-mea.config :as config]
   [efactura-mea.db.ds :as ds]
   [efactura-mea.db.migrations :as m]
   [mount-up.core :as mu]
   [mount.core :as mount]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

;; Log mount service up / down
(mu/on-upndown :info mu/log :before)

(defn clean-temporary-db
  [db-path]
  (log/info "Cleanup db " db-path)
  (fs/delete-if-exists db-path))

(defn get-tables
  [ds]
  (jdbc/execute!
   ds
   ["SELECT name FROM sqlite_schema WHERE type ='table' AND  name NOT LIKE 'sqlite_%';"]
   {:builder-fn rs/as-unqualified-kebab-maps}))

(deftest test-database-migrations
  (testing "We can bring migrations up successfully"
    (let [temp-db (fs/create-temp-file {:prefix "facturi-test" :suffix ".db"})
          test-config {:db-spec {:dbtype "sqlite"
                                 :dbname (str temp-db)}
                       :migratus {:store :database
                                  :db {:dbtype "sqlite"
                                       :dbname (str temp-db)}}}
          new-config {:start (fn []
                               (log/info "Use db " (str temp-db))
                               test-config)
                      :stop #(clean-temporary-db temp-db)}
          _ (-> (mount/only #{#'efactura-mea.db.ds/ds
                              #'efactura-mea.config/conf})
                (mount/swap-states {#'efactura-mea.config/conf new-config})
                mount/start)
          tables (into #{} (get-tables ds/ds))
          expected-tables #{{:name "lista_mesaje"}
                            {:name "descarcare_lista_mesaje"}
                            {:name "company"}
                            {:name "schema_migrations"}
                            {:name "tokens"}
                            {:name "apeluri_api_anaf"}
                            {:name "company_automated_proc"}
                            {:name "detalii_facturi_anaf"}}]
      (log/info "Tables " tables)
      (is (= tables expected-tables))
      ;; reset db - down + up
      (m/reset (:migratus test-config))
      ;; check tables again
      (is (= (into #{} (get-tables ds/ds)) expected-tables))
      (mount/stop))))


(comment

  (str (fs/create-temp-file {:prefix "efactura-test-" :suffix ".db"}))
  ;;=> "/tmp/efactura-test-6489126065789595152.db"

  (run-tests)
  
  
  )