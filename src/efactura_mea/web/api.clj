(ns efactura-mea.web.api
  (:require [babashka.http-client :as http]
            [clojure.java.io :as io]
            [jsonista.core :as j]
            [efactura-mea.web.oauth2-anaf :refer [make-query-string]]
            [efactura-mea.db.facturi :as facturi]
            [efactura-mea.config :as c]))

(defn fetch-cif [db id]
  (:cif (facturi/select-company-cif db {:id id})))

(defn fetch-access-token [db cif]
  (:access_token (facturi/select-access-token db {:cif cif})))

(defn create-sql-tables
  [ds]
  (facturi/create-facturi-anaf-table ds)
  (facturi/create-company-table ds)
  (facturi/create-tokens-table ds))

(defn build-url
  "Build a url from a base and a target {:endpoint <type>}
   - <type> can be :prod or :test;"
  [url-base target]
  (let [type (:endpoint target)]
    (format url-base (name type))))

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
  ([target ds zile cif]
   (let [a-token (fetch-access-token ds cif)
         headers {:headers {"Authorization" (str "Bearer " a-token)}}
         format-url "https://api.anaf.ro/%s/FCTEL/rest/listaMesajeFactura"
         base-url (build-url format-url target)
         q-str {"zile" zile
                "cif" cif}
         endpoint (str base-url "?" (make-query-string q-str))
         r (http/get endpoint headers)
         body (:body r)
         object-mapper (j/object-mapper {:decode-key-fn true})
         lista-facturi (j/read-value body object-mapper)]
     lista-facturi)))

(defn scrie-factura->db [factura ds]
  (let [{:keys [id data_creare tip cif id_solicitare detalii]} factura]
    (facturi/insert-row-factura ds {:id id
                            :data_creare data_creare
                            :tip tip
                            :cif cif
                            :id_solicitare id_solicitare :detalii detalii})))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod;
   - target un map {:endpoint <type>}, <type> poate fi :prod sau :test ."
  [id path target a-token]
  (let [headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
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
        a-token (fetch-access-token ds cif)
        {:keys [id data_creare]} factura
        date-path (build-path data_creare)
        path (str download-to "/" cif "/" date-path)]
    (descarca-factura id path target a-token)))

(defn verifica-descarca-facturi [cfg ds zile]
  (let [target (:target cfg)
        download-to (c/download-dir cfg)
        l (obtine-lista-facturi target ds zile)
        facturi (:mesaje l)]
    (doseq [f facturi]
      (let [id (:id f)
            zip-name (str id ".zip")
            test-file-exist (facturi/test-factura-descarcata? ds {:id id})]
        (if (empty? test-file-exist)
          (do (download-zip-file f target ds download-to)
              (scrie-factura->db f ds))
          (println "factura" zip-name "exista salvata local"))))))


