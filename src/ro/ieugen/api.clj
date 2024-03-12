(ns ro.ieugen.api
  (:require [babashka.http-client :as http]
            [clojure.java.io :as io]
            [jsonista.core :as j]
            [ro.ieugen.oauth2-anaf :refer [make-query-string]]
            [next.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [hugsql.adapter.next-jdbc :as next-adapter]))

(def config {:cif "35586426"
             :write-to "facturi"
             :target {:endpoint :test}
             :db-spec {:dbtype "sqlite"
                       :dbname "facturi-anaf.db"}})

(defn fetch-cif [db id]
  (:cif (select-company-cif db {:id id})))

(defn insert-company->db [db cif name]
  (insert-company db {:cif cif :name name}))

(hugsql/def-db-fns "ro/ieugen/facturi.sql" {:adapter (next-adapter/hugsql-adapter-next-jdbc)})

(comment
  (insert-company->db (:db-spec config) "35586426" "Netdava International")
  (create-facturi-anaf-table (:db-spec config))
  (create-company-table (:db-spec config))
  (create-tokens-table (:db-spec config))
  )

(defn get-access-token [name]
  (let [env (System/getenv)
        access-token (get env name)]
    access-token))

(defn build-url
  "Build a url from a base and a target {:endpoint <type>}
   - <type> can be :prod or :test;"
  [url-base target]
  (let [type (:endpoint target)]
    (format url-base (name type))))

(comment
  (build-url "https://api.anaf.ro/%s/FCTEL/rest/" {:endpoint :test}))

(defn save-zip-file [data file-path]
  (let [f (io/file file-path)
        _ (doto (-> (.getParentFile f)
                    (.mkdirs)))]
    (with-open [output-stream (io/output-stream f)]
      (io/copy (io/input-stream data) output-stream))))

(defn obtine-lista-facturi
  "Obtine lista de facturi pe o perioada de 60 zile din urmă;
   - apeleaza mediul de :test din oficiu;
   - primeste app-state si {:endpoint <type>}, <type> poate fi :prod sau :test ."
  ([target ds]
   (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
         headers {:headers {"Authorization" (str "Bearer " a-token)}}
         format-url "https://api.anaf.ro/%s/FCTEL/rest/listaMesajeFactura"
         base-url (build-url format-url target)
         cif (fetch-cif ds 1)
         q-str {"zile" "60"
                "cif" cif}
         endpoint (str base-url "?" (make-query-string q-str))
         r (http/get endpoint headers)
         body (:body r)
         object-mapper (j/object-mapper {:decode-key-fn true})
         lista-facturi (j/read-value body object-mapper)]
     lista-facturi)))

(comment 
  (let [app-state {:target {:endpoint :test}
                   :db-spec {:dbtype "sqlite"
                             :dbname "facturi-anaf.db"}}
        target (:target app-state)
        db-spec (:db-spec app-state)
        ds (jdbc/get-datasource db-spec)]
   (obtine-lista-facturi target ds)))

(defn scrie-factura->db [factura ds]
  (let [{:keys [id data_creare tip cif id_solicitare detalii]} factura]
    (insert-row-factura ds {:id id
                                           :data_creare data_creare
                                           :tip tip
                                           :cif cif
                                           :id_solicitare id_solicitare :detalii detalii})))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod;
   - target un map {:endpoint <type>}, <type> poate fi :prod sau :test ."
  [id path target]
  (let [a-token (get-access-token "EFACTURA_ACCESS_TOKEN")
        headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
        format-url "https://api.anaf.ro/%s/FCTEL/rest/descarcare"
        base-url (build-url format-url target)
        endpoint (str base-url "?id=" id)
        response (http/get endpoint headers)
        data (:body response)
        app-dir (System/getProperty "user.dir")
        file-path (str app-dir "/" path "/" id ".zip")]
    (save-zip-file data file-path)
    (println "Am descărcat" (str id ".zip") "pe calea" file-path)))

(defn build-path [data-creare]
  (let [an (subs data-creare 0 4)
        luna (subs data-creare 4 6)]
    (str an "/" luna)))

(defn download-zip-file [factura target ds download-to]
  (let [cif (fetch-cif ds 1)
        {:keys [id data_creare]} factura
        date-path (build-path data_creare)
        path (str download-to "/" cif "/" date-path)]
    (descarca-factura id path target)))

(defn verifica-descarca-facturi [app-state]
  (let [target (:target app-state)
        db-spec (:db-spec app-state)
        ds (jdbc/get-datasource db-spec)
        download-to (get-in app-state [:server :download-dir])
        l (obtine-lista-facturi target ds)
        facturi (:mesaje l)]
    (doseq [f facturi]
      (let [id (:id f)
            zip-name (str id ".zip")
            test-file-exist (test-factura-descarcata? ds {:id id})]
        (if (empty? test-file-exist)
          (do (download-zip-file f target ds download-to)
              (scrie-factura->db f ds))
          (println "factura" zip-name "exista salvata local"))))))

(comment
  (verifica-descarca-facturi {:target {:endpoint :test}
                              :db-spec {:dbtype "sqlite"
                                        :dbname "facturi-anaf.db"}
                              :anaf {:client-id "replace-me"
                                     :client-secret "replace-me"
                                     :redirect-uri "replace-me"}
                              :server {:public-path "public"
                                       :templates-path "templates"
                                       :download-dir "DATA_DIR/"}})
  
  (let [ds (jdbc/get-datasource {:dbtype "sqlite"
                                 :dbname "facturi-anaf.db"})]
    (fetch-cif ds 1))
  )
