(ns efactura-mea.web.api
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [clojure.data.json :as json]
   [clojure.tools.logging :as log]
   [efactura-mea.config :as c]
   [efactura-mea.db.db-ops :as db-ops]
   [efactura-mea.db.facturi :as facturi]
   [efactura-mea.job-scheduler :as scheduler]
   [efactura-mea.util :as u]
   [efactura-mea.web.anaf.oauth2 :refer [make-query-string]]
   [efactura-mea.web.ui.componente :as ui]
   [efactura-mea.web.ui.input-validation :as v]
   [hiccup2.core :as h]
   [hiccup.util :refer [raw-string]]
   [java-time.api :as jt]
   [ring.util.response :as rur]
   [reitit.core :as r]
   [efactura-mea.web.utils :as wu])
  (:import
   (java.time LocalDate)
   (java.time.format DateTimeFormatter)
   (java.util.concurrent TimeUnit)))

(defn company_desc_aut_status [db cif]
  (let [c (db-ops/get-company-data db cif)]
    (:desc_aut_status c)))

(defn filter-companies-status-on [companies]
  (filter
   (fn [c]
     (let [s (:desc_aut_status c)]
       (= "on" s))) companies))

(defn create-dir-structure
  [conf]
  (let [dir-name (:data-dir conf)]
    (fs/create-dirs dir-name)))

(defn save-zip-file [data file-path]
  (let [f (io/file file-path)
        _ (doto (-> (.getParentFile f)
                    (.mkdirs)))]
    (with-open [output-stream (io/output-stream f)]
      (io/copy (io/input-stream data) output-stream))))

(defn no-token-found-err-msg
  [cif]
  {:status 403
   :body (str "Access-token for cif " cif " not found.")})

(defn obtine-lista-facturi
  "Obtine lista de facturi pe o perioada de 60 zile din urmă;
   - apeleaza mediul de :test din oficiu;
   - primeste app-state si {:endpoint <type>}, <type> poate fi :prod sau :test ."
  [zile cif tip-apel ds conf]
  (try
    (let [target (:target conf)
          a-token (db-ops/fetch-access-token ds cif)]
      (if (nil? a-token)
        (no-token-found-err-msg cif)
        (let [headers {:headers {"Authorization" (str "Bearer " a-token)}}
              format-url "https://api.anaf.ro/%s/FCTEL/rest/listaMesajeFactura"
              base-url (u/build-url format-url target)
              q-str {"zile" zile
                     "cif" cif}
              endpoint (str base-url "?" (make-query-string q-str))
              r (http/get endpoint headers)
              {:keys [body status]} r
              _ (db-ops/log-api-calls ds cif r tip-apel)
              response (u/encode-body-json->edn body)]
          {:status status
           :body response})))
    (catch Exception e
      (log/error e (str "Eroare apel listaMesajeFactura " (.getMessage e))))))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod;
   - target un map {:endpoint <type>}, <type> poate fi :prod sau :test ."
  [cif id path ds conf]
  (let [a-token (db-ops/fetch-access-token ds cif)
        target (:target conf)
        headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
        format-url "https://api.anaf.ro/%s/FCTEL/rest/descarcare"
        base-url (u/build-url format-url target)
        endpoint (str base-url "?id=" id)
        response (http/get endpoint headers)
        tip-apel "descarcare"
        _ (db-ops/log-api-calls ds cif response tip-apel)
        data (:body response)
        app-dir (System/getProperty "user.dir")
        file-path (str app-dir "/" path "/" id ".zip")]
    (save-zip-file data file-path)
    (log/info "Am descărcat" (str id ".zip") "pe calea" file-path)))

(defn download-zip-file [factura conf ds cif]
  (let [download-to (c/download-dir conf)
        {:keys [id data_creare]} factura
        date-path (u/build-path data_creare)
        path (str download-to "/" cif "/" date-path)]
    (descarca-factura cif id path ds conf)))

(defn parse-message [m]
  (let [{:keys [data_creare tip id_solicitare detalii id descarcata]} m
        downloaded?-mark (when descarcata [:div.icon-text
                                           [:span.icon.has-text-success
                                            [:i.fa.fa-check-square-o]]])
        data-creare-mesaj (u/parse-date data_creare)
        d (:data_c data-creare-mesaj)
        h (:ora_c data-creare-mesaj)
        parsed-tip (s/lower-case tip)]
    (ui/row-factura-anaf d h parsed-tip id_solicitare detalii id downloaded?-mark)))

(defn afisare-lista-mesaje [mesaje eroare]
  (if mesaje
    (let [parsed-messages (for [m mesaje]
                            (parse-message m))
          theader (ui/table-header-facturi-anaf)
          table-rows (cons theader parsed-messages)]
      table-rows)
    eroare))

(defn add-status-downloaded?
  [ids-set mesaje]
  (mapv (fn [mesaj]
          (let [{:keys [id]} mesaj]
            (if (ids-set id)
              (assoc mesaj :descarcata true)
              (assoc mesaj :descarcata false))))
        mesaje))

#_(defn call-for-lista-facturi [opts]
    (let [{:keys [zile cif ds conf]} opts
          tip-apel "lista-mesaje"
          apel-lista-mesaje (obtine-lista-facturi zile cif tip-apel ds conf)
          {:keys [status body]} apel-lista-mesaje
          {:keys [mesaje eroare]} body
        ;; TODO: de vazut ce status are raspunsul cu eroare: este tot 200 dar are eroare?
          ids-mesaje-disponibile (mapv :id mesaje)
          mesaje-disponibile-descarcate (db-ops/fetch-ids-mesaje-descarcate ds ids-mesaje-disponibile)
          ids-map->set (set (map :id_descarcare mesaje-disponibile-descarcate))
          mesaje-marcate (add-status-downloaded? ids-map->set mesaje)]
      (if
       (= 200 status)
        (afisare-lista-mesaje mesaje-marcate eroare)
        body)))

(defn call-for-lista-facturi [opts]
  (let [{:keys [zile cif ds conf]} opts
        tip-apel "lista-mesaje"
        apel-lista-mesaje (obtine-lista-facturi zile cif tip-apel ds conf)
        {:keys [status body]} apel-lista-mesaje
        {:keys [mesaje eroare]} body
        ;; TODO: de vazut ce status are raspunsul cu eroare: este tot 200 dar are eroare?
        ids-mesaje-disponibile (mapv :id mesaje)
        mesaje-disponibile-descarcate (db-ops/fetch-ids-mesaje-descarcate ds ids-mesaje-disponibile)
        ids-map->set (set (map :id_descarcare mesaje-disponibile-descarcate))
        mesaje-marcate (add-status-downloaded? ids-map->set mesaje)]
    mesaje-marcate))

(defn error-message-invalid-result [validation-result]
  (let [{:keys [cif zile]} validation-result
        valid-cif? (first cif)
        valid-zile? (first zile)]
    (ui/validation-message valid-zile? valid-cif?)))

(defn verifica-descarca-facturi [mesaje conf ds]
  (reduce
   (fn [acc f]
     (let [{:keys [id cif]} f
           zip-name (str id ".zip")
           test-file-exist (facturi/test-factura-descarcata? ds {:id id})]
       (if (empty? test-file-exist)
         (do
           (log/info "incep sa downloadez")
           (download-zip-file f conf ds cif)
           (db-ops/scrie-factura->db f ds)
           (conj acc (str "am descarcat mesajul " zip-name)))
         (conj acc (str "factura " zip-name " exista salvata local")))))
   [] mesaje))

(defn fetch-lista-mesaje [opts]
  (let [{:keys [zile cif ds conf]} opts
        tip-apel "lista-mesaje-descarcare"
        apel-lista-mesaje (obtine-lista-facturi zile cif tip-apel ds conf)
        {:keys [status body]} apel-lista-mesaje
        {:keys [eroare mesaje]} body]
    (if (= 200 status)
      (if mesaje
        (let [_ (db-ops/track-descarcare-mesaje ds mesaje)
              queue-lista-mesaje (facturi/select-queue-lista-mesaje ds)
              {:keys [id lista_mesaje]} queue-lista-mesaje
              lista-mesaje (edn/read-string lista_mesaje)
              raport-descarcare-facturi (verifica-descarca-facturi lista-mesaje conf ds)
              raport-descarcare-facturi->ui [:article.message.is-info
                                             [:div.message-body
                                              [:ul
                                               (for [item raport-descarcare-facturi]
                                                 [:li item])]]]]
          (facturi/clear-download-queue ds {:id id})
          raport-descarcare-facturi->ui)
        eroare)
      body)))

(defn descarca-mesaje-automat
  [zile cif ds conf]
  (let [opts {:zile zile :cif cif :ds ds :conf conf}
        validation-result (v/validate-input-data zile cif)]
    (if (nil? validation-result)
      (do
        (log/debug "Pornesc descarcarea automata a facturilor pentru cif: " cif " la " zile " zile")
        (log/debug "o sa apelez (fetch-lista-mesaje) cu ")
        (fetch-lista-mesaje opts)
        (log/debug "Am terminat descarcarea automata a facturilor pentru cif: " cif))
      (error-message-invalid-result validation-result))))

(defn handle-mesaje
  [opts fetch-fn]
  (let [{:keys [cif zile validation-result ds conf]} opts
        facturi-pretabile-descarcat (if (nil? validation-result)
                                      (json/write-str (fetch-fn opts))
                                      (error-message-invalid-result validation-result))]
    ;; TODO: access_token nu era valid, nu a dat nicio eroare in UI
    facturi-pretabile-descarcat))

(defn facturi-anaf-table-config [opts]
  (let [{:keys [url]} opts]
    {:locale true
     :ajaxURL url
     :columns [{:title "Descărcată?" :field "descarcata" :formatter "tickCross" :width 60}
               {:title "ID mesaj" :field "id" :width 150}
               {:title "Dată creare" :field "data_creare" :width 80 :headerSort false}
               {:title "Cif" :field "cif" :width 120}
               {:title "ID solicitare" :field "id_solicitare" :width 120}
               {:title "Tip"  :field "tip"}
               {:title "Detalii" :field "detalii"}]
     :layout "fitColumns"}))

(defn facturi-table-js-logic [opts]
  (let [{:keys [params router]} opts
        {:keys [action cif zile]} params
        url (wu/route-name->url router :efactura-mea.web.routes/listare-mesaje {} {:cif cif :zile zile :action action})
        cfg (json/write-str (facturi-anaf-table-config {:url url}))]
    [:script
     (raw-string (str "document.body.addEventListener('htmx:afterSettle', function(evt) {
         if (evt.target.querySelector('#ceva')) {
           console.log('Initializing Tabulator on #ceva');
           var tableConfig = " cfg ";
           var table = new Tabulator('#ceva', tableConfig);
         }
       });"))]))

(defn handle-list-or-download
  [req]
  (let [{:keys [params ds conf ::r/router]} req
        {:keys [action cif zile]} params
        zile (or (some-> zile Integer/parseInt) nil)
        vr (v/validate-input-data zile cif)
        opts {:params params :validation-result vr :router router}
        result (case action
                 "listare" (let [r [:div
                                    [:p "iata ce facturi poti descarca"]
                                    [:div#ceva]
                                    (facturi-table-js-logic opts)]]
                             (-> (rur/response (str (h/html r)))
                                 (rur/content-type "text/html")))
                 "descarcare" (let [r (handle-mesaje opts fetch-lista-mesaje)]
                                (-> (rur/response (str (h/html r)))
                                    (rur/content-type "text/html"))))]
    result))


(defn handle-list-messages
  [req]
  (let [{:keys [params ds conf]} req
        {:keys [cif zile]} params
        zile (or (some-> zile Integer/parseInt) nil)
        vr (v/validate-input-data zile cif)
        opts {:cif cif :zile zile :validation-result vr :ds ds :conf conf}
        result (handle-mesaje opts call-for-lista-facturi)]
    (-> (rur/response result)
        (rur/content-type "application/json"))))


(defn save-pdf [path pdf-name pdf-content]
  (let [save-to (str path "/" pdf-name)]
    (io/copy pdf-content (io/file save-to))))

(defn zip-file->xml-data
  [conf detalii-fact]
  (let [{:keys [cif data_creare id_descarcare id_solicitare]} detalii-fact
        download-dir (c/download-dir conf)
        app-dir (System/getProperty "user.dir")
        date->path (u/build-path data_creare)
        save-to-path (str app-dir "/" download-dir "/" cif "/" date->path)
        zip-file-name (str id_descarcare ".zip")
        zip-path (str save-to-path "/" zip-file-name)
        xml-name (str id_solicitare ".xml")
        xml-data (u/read-file-from-zip zip-path xml-name)]
    xml-data))

(defn transformare-xml-to-pdf [a-token xml-content detalii-fact conf]
  (let [{:keys [cif data_creare id_descarcare]} detalii-fact
        download-dir (c/download-dir conf)
        app-dir (System/getProperty "user.dir")
        date->path (u/build-path data_creare)
        save-to-path (str app-dir "/" download-dir "/" cif "/" date->path)
        pdf-location (str cif "/" date->path)
        pdf-name (str id_descarcare ".pdf")
        location (str pdf-location "/" pdf-name)
        url "https://api.anaf.ro/prod/FCTEL/rest/transformare/FACT1"
        r (http/post url {:headers {"Authorization" (str "Bearer " a-token)
                                    "Content-Type" "text/plain"}
                          :body xml-content
                          :as :stream})
        pdf-content (:body r)
        _ (save-pdf save-to-path (str id_descarcare ".pdf")  pdf-content)
        content-disposition (str "attachement;filename=" pdf-name)]
    {:status 302
     :headers {"Content-Type" "application/pdf"
               "Content-Disposition" content-disposition
               "Location" location}}))

(defn submit-download-proc
  [live-companies ds conf & {:keys [interval-zile]
                             :or {interval-zile 5}}]
  (doseq [c live-companies]
    (let [cif (:cif c)]
      (log/info "Submitting download for cif : " cif)
      (.submit scheduler/executor-service
               (fn [] (descarca-mesaje-automat interval-zile cif ds conf))))))

(defn schedule-descarcare-automata-per-comp [db conf]
  (let [c (db-ops/companies-with-status db)
        live-companies (filter-companies-status-on c)]
    (submit-download-proc live-companies db conf {:interval-zile 10})))

(defn schedule-token-refresh
  "Scanăm baza de date pentru access_tokens care mai au x zile până la expirare (7 zile?!).
   Și care au și numărul de încercări <= 5.
   Pentru fiecare token access încercăm re-împrospătarea cu refresh_token.
   Dacă nu reușește, creștem numărul de încercări până la 5+.
   Conturile care au mai mult de 5 încercări - sunt marcate cu un mesaj.
   Utilizatorul va trebui să re-împrospăteze tokenul manual."
  [db conf]
  ;; TODO: implementare
  (log/info "Un ciot care așteaptă implementarea."))

(defn handler-descarca-factura-pdf
  [req]
  (let [{:keys [params ds conf]} req
        {:keys [id_descarcare]} params
        detalii-fact (db-ops/detalii-factura-anaf ds id_descarcare)
        {:keys [cif]} detalii-fact
        a-token (db-ops/fetch-access-token ds cif)
        xml-data (zip-file->xml-data conf detalii-fact)]
    (transformare-xml-to-pdf a-token xml-data detalii-fact conf)))

(defn pornire-serviciu-descarcare-automata [db conf]
  (log/info "la pornire AUTOMATA la setare conf este: " conf " in rest este " conf)
  (let [interval-executare 12
        initial-delay 0]
    (log/info "Initialising automatic download for every company with desc_aut_status \"on\", at every " interval-executare " hours")
    (.scheduleAtFixedRate scheduler/job-scheduler-pool
                          (fn [] (schedule-descarcare-automata-per-comp db conf))
                          initial-delay
                          interval-executare
                          TimeUnit/HOURS)
    (.scheduleAtFixedRate scheduler/job-scheduler-pool
                          (fn [] (schedule-token-refresh db conf))
                          initial-delay
                          interval-executare
                          TimeUnit/HOURS)))

^:rct/test
(comment
  (defn get-day-of-week [date-str]
    (let [formatter (DateTimeFormatter/ofPattern "dd/MM/yyyy")
          date (LocalDate/parse date-str formatter)]
      (.getDayOfWeek date)))

  (try
    (jt/local-date "yyyy-MM-dd" "2024-13-10")
    (catch Exception e
      (ex-message e)))
  ;;=> "Conversion failed"
  )

(defn afisare-descarcare-exportare
  [cif]
  (let [t (str "Descarcă lista mesaje ANAF pentru cif:" cif)
        now (u/date-now)]
    [:div#main-container.block
     (ui/title t)
     [:div.columns
      [:div.column
       [:form {:action "/api/alfa/descarca-arhiva"
               :method "get"}
        [:div.field
         [:label.label
          {:for "perioada"}
          "Listă mesaje pentru: "]
         [:div.select.is-fullwidth
          [:select {:hx-get "/api/alfa/sumar-descarcare-arhiva"
                    :hx-include "[name='cif'], [name='date_first']"
                    :hx-trigger "change"
                    :hx-target "#status"
                    :name "perioada"}
           [:option {:value "luna"} "lună"]
           [:option {:value "saptamana"} "săptamână"]]]]
        [:div.field
         [:label.label {:for "date_first"} "Alege perioada:"]
         [:input.input {:hx-get "/api/alfa/sumar-descarcare-arhiva"
                        :hx-include "[name='cif'], [name='perioada']"
                        :hx-trigger "changeDate"
                        :hx-target "#status"
                        :type "text"
                        :value now
                        :name "date_first"
                        :id "date_first"}]]
        [:div#status.field.content]
        [:input {:name "cif"
                 :value cif
                 :type "hidden"}]
        [:label.label {:for "file_type"} "Tipul fișierului:"]
        [:div.checkboxes
         [:label.checkbox [:input {:type "checkbox"
                                   :name "file_type_zip"} "ZIP"]]
         [:label.checkbox [:input {:type "checkbox"
                                   :name "file_type_pdf"} "PDF"]]]

        [:div#validation-err-container {:style "display:none;"}
         [:article.message.is-warning
          [:div.message-body "Trebuie să selectezi cel puțin un tip de fișier"]]]
        [:button.button.is-link {:type "submit"} "Descarcă arhiva"]
        [:div#validation-err-container]]]
      [:div.column]]]))

(defn handler-descarcare-automata-facturi
  [req]
  (let [{:keys [params ds]} req
        {:keys [cif descarcare-automata]} params
        company-data (db-ops/get-company-data ds cif)
        {:keys [id]} company-data
        now (u/date-time-now-utc)
        opts {:id id :date_modified now :status "on"}
        status-on-info-text "Serviciul descărcare automată pornit"
        status-off-info-text "Serviciul descărcare automată oprit"]
    (if descarcare-automata
      (try
        (db-ops/update-company-desc-aut-status ds opts)
        (log/info "updated for company-id: " id " status ON")
        (-> (rur/response status-on-info-text)
            (rur/content-type "text/html"))
        (catch Exception e
          (let [msg (.getMessage e)]
            [:div.notification.is-danger
             msg])))
      (try
        (let [opts {:id id :date_modified now :status "off"}]
          #_(oprire-descarcare-automata)
          (db-ops/update-company-desc-aut-status ds opts)
          (log/info "canceling timer, set for comp-id " id " status OFF")
          (-> (rur/response status-off-info-text)
              (rur/content-type "text/html")))
        (catch Exception e
          (let [msg (.getMessage e)]
            [:div.notification.is-danger
             msg]))))))



(comment

  #_(pornire-serviciu-descarcare-automata)
  (.shutdown scheduler/sched-pool)

  (defn some-job []
    (log/info "writing to DB and other stuff"))

  (company_desc_aut_status efactura-mea.db.ds/ds "35586426")

  0)
