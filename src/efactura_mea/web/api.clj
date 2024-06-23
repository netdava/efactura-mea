(ns efactura-mea.web.api
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [efactura-mea.config :as c]
            [efactura-mea.db.db-ops :as db]
            [efactura-mea.db.facturi :as facturi]
            [efactura-mea.ui.componente :as ui-comp]
            [efactura-mea.ui.input-validation :as v]
            [efactura-mea.util :as u]
            [efactura-mea.web.api :as api]
            [efactura-mea.web.oauth2-anaf :refer [make-query-string]]
            [hiccup2.core :as h]))


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
  [target ds zile cif tip-apel]
  (let [a-token (db/fetch-access-token ds cif)]
    (if (nil? a-token)
      {:status 403
       :body (str "Access-token for cif " cif " not found.")}
      (let [headers {:headers {"Authorization" (str "Bearer " a-token)}}
            format-url "https://api.anaf.ro/%s/FCTEL/rest/listaMesajeFactura"
            base-url (u/build-url format-url target)
            q-str {"zile" zile
                   "cif" cif}
            endpoint (str base-url "?" (make-query-string q-str))
            r (http/get endpoint headers)
            body (:body r)
            status (:status r)
            _ (db/log-api-calls ds r tip-apel)
            response (u/encode-body-json->edn body)]
        {:status status
         :body response}))))



(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod;
   - target un map {:endpoint <type>}, <type> poate fi :prod sau :test ."
  [ds id path target a-token]
  (let [headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
        format-url "https://api.anaf.ro/%s/FCTEL/rest/descarcare"
        base-url (u/build-url format-url target)
        endpoint (str base-url "?id=" id)
        response (http/get endpoint headers)
        tip-apel "descarcare"
        _ (db/log-api-calls ds response tip-apel)
        data (:body response)
        app-dir (System/getProperty "user.dir")
        file-path (str app-dir "/" path "/" id ".zip")]
    (save-zip-file data file-path)
    (println "Am descărcat" (str id ".zip") "pe calea" file-path)))

(defn download-zip-file [factura target ds download-to]
  (let [cif (db/fetch-cif ds 1)
        a-token (db/fetch-access-token ds cif)
        {:keys [id data_creare]} factura
        date-path (u/build-path data_creare)
        path (str download-to "/" cif "/" date-path)]
    (descarca-factura ds id path target a-token)))

(defn parse-message [m]
  (let [{:keys [data_creare tip id_solicitare detalii id]} m
        data-creare-mesaj (u/parse-date data_creare)
        d (:data_c data-creare-mesaj)
        h (:ora_c data-creare-mesaj)
        parsed-tip (s/lower-case tip)]
    (ui-comp/row-factura-anaf d h parsed-tip id_solicitare detalii id)))

(defn call-for-lista-facturi [target ds zile cif]
  (let [tip-apel "lista-mesaje"
        apel-lista-mesaje (obtine-lista-facturi target ds zile cif tip-apel)
        status (:status apel-lista-mesaje)
        body (:body apel-lista-mesaje)
        err (:eroare body)
        mesaje (:mesaje body)]
    (if (= 200 status)
      (if mesaje
        (let [parsed-messages (for [m mesaje]
                                (parse-message m))
              theader (ui-comp/table-header-facturi-anaf)
              table-rows (cons theader parsed-messages)]
          table-rows)
        err)
      body)))

(defn parse-validation-result [validation-result]
  (let [err validation-result
        valid-zile? (first (:zile err))
        valid-cif? (first (:cif err))]
    (ui-comp/validation-message valid-zile? valid-cif?)))

(defn parse-to-int-when-present [q z]
  (if (contains? q :zile)
    (try (Integer/parseInt z)
         (catch Exception _ z))
    nil))

(defn listeaza-mesaje
  [req conf ds]
  (let [target (:target conf)
        querry-params (u/encode-request-params->edn req)
        zile (:zile querry-params)
        zile-int (parse-to-int-when-present querry-params zile)
        cif (:cif querry-params)
        validation-result  (v/validate-input-data zile-int cif)
        r (if (nil? validation-result)
            (call-for-lista-facturi target ds zile cif)
            (parse-validation-result validation-result))]
    (ui-comp/lista-mesaje r)))

(defn verifica-descarca-facturi [target ds mesaje path]
  (reduce
   (fn [acc f]
     (let [id (:id f)
           zip-name (str id ".zip")
           test-file-exist (facturi/test-factura-descarcata? ds {:id id})]
       (if (empty? test-file-exist)
         (do (download-zip-file f target ds path)
             (db/scrie-factura->db f ds)
             (conj acc (str "am descarcat mesajul " zip-name)))
         (conj acc (str "factura " zip-name " exista salvata local")))))
   [] mesaje))

(defn parse-tip-factura [tip-factura]
  (let [tip (s/lower-case tip-factura)]
    (case tip
      "factura primita" "primita"
      "factura trimisa" "trimisa"
      "erori factura" "eroare"
      tip)))

(defn opis-facturi-descarcate [ds facturi]
  (for [f facturi]
    (let [{:keys [tip id_descarcare]} f
          is-downloaded? (not (> (count (facturi/test-factura-descarcata? ds {:id id_descarcare})) 0))
          tip-factura (parse-tip-factura tip)
          invoice-details (assoc f :tip tip-factura)
          _ (when is-downloaded? (db/scrie-detalii-factura-anaf->db invoice-details ds))]
      (ui-comp/row-factura-descarcata-detalii ds invoice-details))))


(defn fetch-lista-mesaje [target ds zile cif conf]
  (let [tip-apel "lista-mesaje-descarcare"
        apel-lista-mesaje (obtine-lista-facturi target ds zile cif tip-apel)
        status (:status apel-lista-mesaje)
        body (:body apel-lista-mesaje)
        err (:eroare body)
        mesaje (:mesaje body)]
    (if (= 200 status)
      (if mesaje
        (let [_ (db/track-descarcare-mesaje ds mesaje)
              queue-lista-mesaje (facturi/select-queue-lista-mesaje ds)
              queue-id (:id queue-lista-mesaje)
              lm-str (:lista_mesaje queue-lista-mesaje)
              lista-mesaje (edn/read-string lm-str)
              download-to (c/download-dir conf)
              raport-descarcare-facturi (verifica-descarca-facturi target ds lista-mesaje download-to)
              raport-descarcare-facturi->ui (h/html [:article.message.is-info
                                                     [:div.message-body
                                                      [:ul
                                                       (for [item raport-descarcare-facturi]
                                                         [:li item])]]])]
          (facturi/delete-row-download-queue ds {:id queue-id})
          (h/html raport-descarcare-facturi->ui))
        err)
      body)))

(defn combine-invoice-data [ds invoice-path]
  (let [id-mesaj-anaf (u/path->id-mesaj invoice-path)
        detalii-anaf (first (facturi/select-factura-descarcata ds {:id id-mesaj-anaf}))
        detalii-xml (u/get-invoice-data invoice-path)
        detalii-xml (assoc detalii-xml :href invoice-path)]
    (merge detalii-anaf detalii-xml)))


(defn gather-invoices-data [ds paths-fact-desc-local]
  (reduce (fn [acc invoice-path]
            (merge acc (combine-invoice-data ds invoice-path)))
          []
          paths-fact-desc-local))

(defn afisare-facturile-mele [_ ds conf]
  (let [dd (c/download-dir conf)
        paths-fact-desc-local (u/list-files-from-dir dd)
        detalii-combinate-facturi (gather-invoices-data ds paths-fact-desc-local)
        detalii-sortate (sort #(compare (:data_creare %1) (:data_creare %2)) detalii-combinate-facturi)
        detalii->table-rows (opis-facturi-descarcate ds detalii-sortate)
        r (h/html (ui-comp/tabel-facturi-descarcate detalii->table-rows))]
    {:status 200
     :body (str r)
     :headers {"content-type" "text/html"}}))

(defn descarca-mesaje
  [req conf ds]
  (let [target (:target conf)
        querry-params (u/encode-request-params->edn req)
        zile (:zile querry-params)
        zile-int (if (contains? querry-params :zile)
                   (try (Integer/parseInt zile)
                        (catch Exception _ zile))
                   nil)
        cif (:cif querry-params)
        validation-result  (v/validate-input-data zile-int cif)
        r (if (nil? validation-result)
            (fetch-lista-mesaje target ds zile cif conf)
            (parse-validation-result validation-result))]
    (ui-comp/lista-mesaje r)))

(defn efactura-action-handler [req conf ds]
  (let [params (:params req)]
    (case (:action params)
      "listare" (listeaza-mesaje req conf ds)
      "descarcare" (descarca-mesaje req conf ds))))

(defn save-pdf [path pdf-name pdf-content]
  (let [save-to (str path "/" pdf-name)]
    (io/copy pdf-content (io/file save-to))))

(defn transformare-xml-to-pdf [req ds conf]
  (let [p (:params req)
        id-descarcare (:id_descarcare p)
        detalii-fact (first (facturi/select-detalii-factura-descarcata ds {:id id-descarcare}))
        {:keys [cif data_creare id_solicitare]} detalii-fact
        download-dir (c/download-dir conf)
        date->path (u/build-path data_creare)
        app-dir (System/getProperty "user.dir")
        zip-file-name (str id-descarcare ".zip")
        final-path (str app-dir "/" download-dir "/" cif "/" date->path)
        zip-path (str final-path "/" zip-file-name)
        xml-name (str id_solicitare ".xml")
        xml-content (u/read-file-from-zip zip-path xml-name)
        a-token (db/fetch-access-token ds cif)
        url "https://api.anaf.ro/prod/FCTEL/rest/transformare/FACT1"
        r (http/post url {:headers {"Authorization" (str "Bearer " a-token)
                                    "Content-Type" "text/plain"}
                          :body xml-content
                          :as :stream})
        body (:body r)
        _ (save-pdf app-dir (str id-descarcare ".pdf")  body)]
    {:status 200
     :body "ok"
     :headers {"content-type" "text/html"}}))

