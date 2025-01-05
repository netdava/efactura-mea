(ns efactura-mea.web.api
  (:require
   [babashka.fs :as fs]
   [babashka.http-client :as http]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [efactura-mea.config :as c]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.db.facturi :as facturi]
   [efactura-mea.job-scheduler :as scheduler]
   [efactura-mea.layout :as layout]
   [efactura-mea.ui.componente :as ui]
   [efactura-mea.ui.input-validation :as v]
   [efactura-mea.util :as u]
   [efactura-mea.web.login :as login]
   [efactura-mea.web.oauth2-anaf :refer [make-query-string]]
   [hiccup2.core :as h]
   [reitit.core :as r]
   [java-time.api :as jt])
  (:import
   (java.time LocalDate)
   (java.time.format DateTimeFormatter)
   (java.util.concurrent TimeUnit)))

(defn company_desc_aut_status [db cif]
  (let [c (db/get-company-data db cif)]
    (:desc_aut_status c)))

(defn desc_aut_status_on? [status]
  (case status
    "on" true
    "off" false
    false))

(defn filter-companies-status-on [companies]
  (filter
   (fn [c]
     (let [s (:desc_aut_status c)]
       (= "on" s))) companies))

(defn create-dir-structure
  [conf]
  (let [dir-name (:data-dir conf)]
    (fs/create-dirs dir-name)))

(defn init-db [ds]
  (println "Initialising database")
  (println "* Creating SQL tables")
  (db/create-sql-tables ds))

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
          a-token (db/fetch-access-token ds cif)]
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
              _ (db/log-api-calls ds cif r tip-apel)
              response (u/encode-body-json->edn body)]
          {:status status
           :body response})))
    (catch Exception e (println (.getMessage e)))))

(defn descarca-factura
  "Descarcă factura în format zip pe baza id-ului.
   - funcționează doar cu endpointurile de test/prod;
   - target un map {:endpoint <type>}, <type> poate fi :prod sau :test ."
  [cif id path ds conf]
  (let [a-token (db/fetch-access-token ds cif)
        target (:target conf)
        headers {:headers {"Authorization" (str "Bearer " a-token)} :as :stream}
        format-url "https://api.anaf.ro/%s/FCTEL/rest/descarcare"
        base-url (u/build-url format-url target)
        endpoint (str base-url "?id=" id)
        response (http/get endpoint headers)
        tip-apel "descarcare"
        _ (db/log-api-calls ds cif response tip-apel)
        data (:body response)
        app-dir (System/getProperty "user.dir")
        file-path (str app-dir "/" path "/" id ".zip")]
    (save-zip-file data file-path)
    (println "Am descărcat" (str id ".zip") "pe calea" file-path)))

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
  (println mesaje)
  (if mesaje
    (let [parsed-messages (for [m mesaje]
                            (parse-message m))
          theader (ui/table-header-facturi-anaf)
          table-rows (cons theader parsed-messages)]
      table-rows)
    eroare))

(defn add-status-downloaded?
  [ids-set mesaje]
  (map (fn [mesaj]
         (let [{:keys [id]} mesaj]
           (if (ids-set id)
             (assoc mesaj :descarcata true)
             (assoc mesaj :descarcata false))))
       mesaje))

(defn call-for-lista-facturi [zile cif ds conf]
  (let [tip-apel "lista-mesaje"
        apel-lista-mesaje (obtine-lista-facturi zile cif tip-apel ds conf)
        {:keys [status body]} apel-lista-mesaje
        {:keys [mesaje eroare]} body
        ids-mesaje-disponibile (mapv :id mesaje)
        mesaje-disponibile-descarcate (db/fetch-ids-mesaje-descarcate ds ids-mesaje-disponibile)
        ids-map->set (set (map :id_descarcare mesaje-disponibile-descarcate))
        mesaje-marcate (add-status-downloaded? ids-map->set mesaje)]
    (if
     (= 200 status)
      (afisare-lista-mesaje mesaje-marcate eroare)
      body)))

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
           (println "incep sa downloadez")
           (download-zip-file f conf ds cif)
           (db/scrie-factura->db f ds)
           (conj acc (str "am descarcat mesajul " zip-name)))
         (conj acc (str "factura " zip-name " exista salvata local")))))
   [] mesaje))

(defn fetch-lista-mesaje [zile cif ds conf]
  (println "Entering fetch-lista-mesaje with zile:" zile ", cif:" cif "si conf " conf)
  (let [tip-apel "lista-mesaje-descarcare"
        apel-lista-mesaje (obtine-lista-facturi zile cif tip-apel ds conf)
        {:keys [status body]} apel-lista-mesaje
        {:keys [eroare mesaje]} body]
    (if (= 200 status)
      (if mesaje
        (let [_ (db/track-descarcare-mesaje ds mesaje)
              queue-lista-mesaje (facturi/select-queue-lista-mesaje ds)
              {:keys [id lista_mesaje]} queue-lista-mesaje
              lista-mesaje (edn/read-string lista_mesaje)
              raport-descarcare-facturi (verifica-descarca-facturi lista-mesaje conf ds)
              raport-descarcare-facturi->ui (h/html [:article.message.is-info
                                                     [:div.message-body
                                                      [:ul
                                                       (for [item raport-descarcare-facturi]
                                                         [:li item])]]])]
          (facturi/clear-download-queue ds {:id id})
          (h/html raport-descarcare-facturi->ui))
        eroare)
      body)))

(defn handler-login
  [_]
  (let [content (login/login-form)]
    {:status 200
     :headers {"content-type" "text/html"}
     :body content}))

(defn descarca-mesaje-automat
  [zile cif ds conf]
  (println "PARAMS din descarca-mesaje-automat " zile " / " cif "/" conf)
  (let [validation-result (v/validate-input-data zile cif)]
    (if (nil? validation-result)
      (do
        (println "Pornesc descarcarea automata a facturilor pentru cif: " cif " la " zile " zile")
        (println "o sa apelez (fetch-lista-mesaje) cu ")
        (fetch-lista-mesaje zile cif ds conf)
        (println "Am terminat descarcarea automata a facturilor pentru cif: " cif))
      (error-message-invalid-result validation-result))))

(defn handle-mesaje
  [opts ds conf fetch-fn]
  (let [{:keys [cif zile validation-result]} opts
        r (if (nil? validation-result)
            (fetch-fn zile cif ds conf)
            (error-message-invalid-result validation-result))]
    (ui/lista-mesaje r)))

(defn handle-list-or-download [req]
  (let [{:keys [params ds conf]} req
        {:keys [action cif zile]} params
        zile (or (some-> zile Integer/parseInt) nil)
        vr (v/validate-input-data zile cif)
        opts {:cif cif :zile zile :validation-result vr}]
    (case action
      "listare" (handle-mesaje opts ds conf call-for-lista-facturi)
      "descarcare" (handle-mesaje opts ds conf fetch-lista-mesaje))))

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

(defn handler-formular-descarcare-automata
  [req]
  (let [{:keys [path-params ds]} req
        {:keys [cif]} path-params
        c-data (db/get-company-data ds cif)
        my-selected-val (fn [n]
                          (let [opts {:value n}]
                            (when (= n 5) (assoc opts :selected true))))
        days (range 1 60)
        days-select-vals (for [n days]
                           [:option (my-selected-val n) n])
        {:keys [desc_aut_status]} c-data
        status (desc_aut_status_on? desc_aut_status)
        status-msg (if status
                     "Serviciul descărcare automată pornit"
                     "Serviciul descărcare automată oprit")]
    {:status 200
     :body (str
            (h/html
             [:form.block {:hx-get "/pornire-descarcare-automata"
                           :hx-target "#status"
                           :hx-swap "innerHTML"}
              [:div.field
               [:label.label "CIF:"]
               [:input.input {:readonly "readonly"
                              :type "text"
                              :id "cif-number"
                              :list "cif"
                              :name "cif"
                              :value cif
                              :placeholder cif}]]
              [:div.field
               [:label.label "Număr de zile pentru descărcare automată facturi anaf:"]
               [:div.select [:select {:id "zile" :name "zile"}
                             days-select-vals]]]
              [:div.field
               [:input {:id "descarcare-automata"
                        :type "checkbox"
                        :checked status
                        :name "descarcare-automata"
                        :class "switch is-rounded is-info"}]
               [:label {:for "descarcare-automata"} "Activează descarcarea automată"]]
              [:div.field.content.is-small
               [:span.icon-text
                [:span.icon.has-text-info
                 [:i.fa.fa-info-circle]]
                [:p#status status-msg]]]
              [:button.button.is-small.is-link {:type "submit"} "Setează"]]))
     :headers {"content-type" "text/html"}}))

(defn set-descarcare-automata
  [cif]
  (let [t (str "Activare descărcare automată facturi")
        url (str "/formular-descarcare-automata/" cif)]
    (h/html
     [:div#main-container.block
      (ui/title t)
      [:div#sda-form
       {:hx-get url
        :hx-trigger "load"}]
      [:div#modal-wrapper]])))

(defn handler-afisare-formular-descarcare-automata
  [req]
  (let [{:keys [path-params ::r/router]} req
        cif (:cif path-params)
        opts {:cif cif :router router}
        sidebar (layout/sidebar-company-data opts)
        content (set-descarcare-automata cif)]
    (layout/main-layout content sidebar)))


(defn submit-download-proc
  [live-companies ds conf & {:keys [interval-zile]
                             :or {interval-zile 5}}]
  (doseq [c live-companies]
    (let [cif (:cif c)]
      (println "Submitting download for cif : " cif)
      (.submit scheduler/descarcari-pool
               (fn [] (descarca-mesaje-automat interval-zile cif ds conf))))))

(defn schedule-descarcare-automata-per-comp [db conf]
  (let [c (db/companies-with-status db)
        live-companies (filter-companies-status-on c)]
    (submit-download-proc live-companies db conf {:interval-zile 10})))

(defn handler-descarca-factura-pdf
  [req]
  (let [{:keys [params ds conf]} req
        {:keys [id_descarcare]} params
        detalii-fact (db/detalii-factura-anaf ds id_descarcare)
        {:keys [cif]} detalii-fact
        a-token (db/fetch-access-token ds cif)
        xml-data (zip-file->xml-data conf detalii-fact)]
    (transformare-xml-to-pdf a-token xml-data detalii-fact conf)))

(defn pornire-serviciu-descarcare-automata [db conf]
  (println "la pornire AUTOMATA la setare conf este: " conf " in rest este " conf)
  (let [interval-executare 12
        initial-delay 0]
    (println "Initialising automatic download for every company with desc_aut_status \"on\", at every " interval-executare " hours")
    (.scheduleAtFixedRate scheduler/sched-pool
                          (fn [] (schedule-descarcare-automata-per-comp db conf))
                          initial-delay
                          interval-executare
                          TimeUnit/HOURS)))

(defn handler-descarcare-automata-facturi [req]
  (let [{:keys [params ds conf]} req
        {:keys [cif descarcare-automata]} params
        company-data (db/get-company-data ds cif)
        {:keys [id]} company-data
        now (u/formatted-date-now)
        opts {:id id :date_modified now :status "on"}]
    (if descarcare-automata
      (try
        (do
          (db/update-company-desc-aut-status ds opts)
          (pornire-serviciu-descarcare-automata ds conf)
          (println "updated for company-id: " id " status ON")
          {:status 200
           :body "Serviciul descărcare automată pornit"
           :headers {"content-type" "text/html"}})
        (catch Exception e
          (let [msg (.getMessage e)]
            [:div.notification.is-danger
             msg])))
      (try
        (let [opts {:id id :date_modified now :status "off"}]
          #_(oprire-descarcare-automata)
          (db/update-company-desc-aut-status ds opts)
          (println "canceling timer, set for comp-id " id " status OFF")
          {:status 200
           :body "Serviciul descărcare automată oprit"
           :headers {"content-type" "text/html"}})
        (catch Exception e
          (let [msg (.getMessage e)]
            [:div.notification.is-danger
             msg]))))))

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
        now (u/date-now)
        r (str (h/html
                [:div#main-container.block
                 (ui/title t)
                 [:div.columns
                  [:div.column
                   [:form {:action "/descarca-arhiva"
                           :method "get"}
                    [:div.field
                     [:label.label
                      {:for "perioada"}
                      "Listă mesaje pentru: "]
                     [:div.select.is-fullwidth
                      [:select {:hx-get "/sumar-descarcare-arhiva"
                                :hx-include "[name='cif'], [name='date_first']"
                                :hx-trigger "change"
                                :hx-target "#status"
                                :name "perioada"}
                       [:option {:value "luna"} "lună"]
                       [:option {:value "saptamana"} "săptamână"]]]]
                    [:div.field
                     [:label.label {:for "date_first"} "Alege perioada:"]
                     [:input.input {:hx-get "/sumar-descarcare-arhiva"
                                    :hx-include "[name='cif'], [name='perioada']"
                                    :hx-trigger "changeDate"
                                    :hx-target "#status"
                                    :type "text"
                                    :value now
                                    :name "date_first"
                                    :id "date_first"}]]
                    [:div#status.field.content.is-small]
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

                    #_[:div.field
                       [:label.label "Alege perioada:"]
                       [:div#date-range-picker
                        [:input {:type "text"
                                 :name "date_start"
                                 :id "date_start"}]
                        [:input {:type "text"
                                 :name "date_end"
                                 :id "date_end"}]]]
                    [:button.button.is-small.is-link {:type "submit"} "Descarcă arhiva"]
                    [:div#validation-err-container]]]
                  [:div.column]]]))]
    {:status 200
     :body r
     :headers {"content-type" "text/html"}}))


(comment

  #_(pornire-serviciu-descarcare-automata)
  (.shutdown scheduler/sched-pool)

  (defn some-job []
    (println "writing to DB and other stuff"))

  (company_desc_aut_status ds "35586426")
  (desc_aut_status_on? "on")

  0)
