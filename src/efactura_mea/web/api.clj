(ns efactura-mea.web.api
  (:require [babashka.http-client :as http]
            [babashka.fs :as fs]
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
            [efactura-mea.ui.pagination :as pag]
            [hiccup2.core :as h]
            [java-time :as jt]
            [efactura-mea.job-scheduler :as scheduler])
  (:import (java.util.concurrent TimeUnit)))

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

(defn formular-inregistrare-companie
  []
  (let [t (str "Înregistrare companie în eFactura")]
    (h/html
     [:div#main-container.block
      (ui-comp/title t)
      [:form.block {:hx-get "/inregistreaza-companie"
                    :hx-target "#main-container"
                    :hx-swap "innerHTML swap:1s"}
       [:div.field
        [:label.label "CIF:"]
        [:input.input {:type "text"
                       :id "cif"
                       :name "cif"
                       :placeholder "cif companie"}]]
       [:div.field
        [:label.label "Denumire companie"]
        [:input.input {:type "text"
                       :id "name"
                       :name "name"}]]
       [:button.button.is-small.is-link {:type "submit"} "Adaugă companie"]]])))

(defn inregistrare-noua-companie
  [ds params]
  (try (let [{:keys [cif name]} params
             inregistrata? (db/test-companie-inregistrata ds cif)
             _ (if (not inregistrata?)
                 (facturi/insert-company ds {:cif cif :name name})
                 (throw (Exception. (str "compania cu cif " cif " figurează ca fiind deja înregistrată."))))
             m (str "Compania \"" name "\" cu cif \"" cif "\" a fost înregistrată cu succesito.")
             detailed-msg (str (h/html
                                [:article.message.is-success
                                 [:div.message-header
                                  [:p "Felicitări"]]
                                 [:div.message-body
                                  m]]))]
         {:status 200
          :body detailed-msg
          :headers {"content-type" "text/html"}})
       (catch
        Exception e 
         (let [err-msg (.getMessage e)
               detailed-msg (str (h/html
                                  [:article.message.is-warning
                                   [:div.message-header
                                    [:p "Nu s-a putut inregistra compania:"]]
                                   [:div.message-body
                                    err-msg]]))]
           {:status 200
            :body detailed-msg
            :headers {"content-type" "text/html"}}))))

(defn afisare-companii-inregistrate [ds]
  (let [companii (db/get-companies-data ds)]
    (ui-comp/select-a-company companii)))

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
         :body response}))))

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
  (let [{:keys [data_creare tip id_solicitare detalii id]} m
        data-creare-mesaj (u/parse-date data_creare)
        d (:data_c data-creare-mesaj)
        h (:ora_c data-creare-mesaj)
        parsed-tip (s/lower-case tip)]
    (ui-comp/row-factura-anaf d h parsed-tip id_solicitare detalii id)))

(defn afisare-lista-mesaje [mesaje eroare]
  (if mesaje
    (let [parsed-messages (for [m mesaje]
                            (parse-message m))
          theader (ui-comp/table-header-facturi-anaf)
          table-rows (cons theader parsed-messages)]
      table-rows)
    eroare))

(defn call-for-lista-facturi [zile cif ds conf]
  (let [tip-apel "lista-mesaje"
        apel-lista-mesaje (obtine-lista-facturi zile cif tip-apel ds conf)
        {:keys [status body]} apel-lista-mesaje
        {:keys [mesaje eroare]} body]
    (if
     (= 200 status)
      (afisare-lista-mesaje mesaje eroare)
      body)))

(defn error-message-invalid-result [validation-result]
  (let [{:keys [cif zile]} validation-result
        valid-cif? (first cif)
        valid-zile? (first zile)]
    (ui-comp/validation-message valid-zile? valid-cif?)))

(defn listeaza-mesaje
  [params ds conf]
  (let [{:keys [cif zile]} params
        zile (if zile (parse-long zile) nil)
        validation-result  (v/validate-input-data zile cif)
        r (if (nil? validation-result)
            (call-for-lista-facturi zile cif ds conf)
            (error-message-invalid-result validation-result))]
    (ui-comp/lista-mesaje r)))

(defn verifica-descarca-facturi [mesaje conf ds cif]
  (reduce
   (fn [acc f]
     (let [id (:id f)
           ;; TODO de vazut daca cumva cif exista in fiecare mesaj, sa-l extrag si sa nu mai treb sa`l pasez
           zip-name (str id ".zip")
           test-file-exist (facturi/test-factura-descarcata? ds {:id id})]
       (if (empty? test-file-exist)
         (do (download-zip-file f conf ds cif)
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


(defn opis-facturi-descarcate [facturi ds]
  (for [f facturi]
    (when f (let [{:keys [tip id_descarcare]} f
                  is-downloaded? (> (count (facturi/test-factura-descarcata? ds {:id id_descarcare})) 0)
                  tip-factura (parse-tip-factura tip)
                  invoice-details (assoc f :tip tip-factura)
                  _ (when is-downloaded? (db/scrie-detalii-factura-anaf->db invoice-details ds))]
              (ui-comp/row-factura-descarcata-detalii invoice-details)))))

#_(defn planifica-descarca-mesaje
  [mesaje]
  (let [_ (db/track-descarcare-mesaje ds mesaje)
        queue-lista-mesaje (facturi/select-queue-lista-mesaje ds)
        {:keys [id lista_mesaje]} queue-lista-mesaje
        lista-mesaje (edn/read-string lista_mesaje)
        raport-descarcare-facturi (verifica-descarca-facturi lista-mesaje target )
        raport-descarcare-facturi->ui (h/html [:article.message.is-info
                                               [:div.message-body
                                                [:ul
                                                 (for [item raport-descarcare-facturi]
                                                   [:li item])]]])]
    (facturi/clear-download-queue ds {:id id})
    (h/html raport-descarcare-facturi->ui)))

(defn fetch-lista-mesaje [zile cif ds conf]
  (try (let [tip-apel "lista-mesaje-descarcare"
             apel-lista-mesaje (obtine-lista-facturi zile cif tip-apel ds conf)
             {:keys [status body]} apel-lista-mesaje
             {:keys [eroare mesaje]} body]
         (if (= 200 status)
           (if mesaje
             (let [_ (db/track-descarcare-mesaje ds mesaje)
                   queue-lista-mesaje (facturi/select-queue-lista-mesaje ds)
                   {:keys [id lista_mesaje]} queue-lista-mesaje
                   lista-mesaje (edn/read-string lista_mesaje)
                   raport-descarcare-facturi (verifica-descarca-facturi lista-mesaje conf ds cif)
                   raport-descarcare-facturi->ui (h/html [:article.message.is-info
                                                          [:div.message-body
                                                           [:ul
                                                            (for [item raport-descarcare-facturi]
                                                              [:li item])]]])]
               (facturi/clear-download-queue ds {:id id})
               (h/html raport-descarcare-facturi->ui))
             eroare)
           body))
       (catch Exception _ "OOps, nu am gasit date pt cif " cif)))

(defn combine-invoice-data [invoice-path ds]
  (let [id-mesaj-anaf (u/path->id-mesaj invoice-path)
        test-file-exist (facturi/test-factura-descarcata? ds {:id id-mesaj-anaf})
        ;; aici sa fac verificarea daca id mesaj-anaf exista in tabelul lista-mesaje
        ]
    (when (seq test-file-exist)
      (let [detalii-anaf (first (facturi/select-factura-descarcata ds {:id id-mesaj-anaf}))
            detalii-xml (u/get-invoice-data invoice-path)
            detalii-xml (assoc detalii-xml :href invoice-path)]
        (merge detalii-anaf detalii-xml)))))

(defn gather-invoices-data [paths-fact-desc-local ds]
  (reduce (fn [acc invoice-path]
            (merge acc (combine-invoice-data invoice-path ds)))
          []
          paths-fact-desc-local))

(defn sortare-facturi-data-creare
  [facturi]
  (sort #(compare (:data_creare %1) (:data_creare %2)) facturi))

(defn afisare-facturile-mele [params conf ds]
  (let [{:keys [cif]} params
        dd (c/download-dir conf)
        path-facturi (str dd "/" cif)
        paths-fact-desc-local (u/list-files-from-dir path-facturi)
        detalii-combinate-facturi (gather-invoices-data paths-fact-desc-local ds)
        facturi-sortate (sortare-facturi-data-creare detalii-combinate-facturi)
        detalii->table-rows (opis-facturi-descarcate facturi-sortate ds)
        r (h/html (ui-comp/tabel-facturi-descarcate detalii->table-rows))]
    {:status 200
     :body (str r)
     :headers {"content-type" "text/html"}}))

(defn descarca-mesaje
  [params ds conf]
  (let [{:keys [cif zile]} params
        zile-int (if zile (parse-long zile) nil)
        validation-result (v/validate-input-data zile-int cif)
        r (if (nil? validation-result)
            (fetch-lista-mesaje zile cif ds conf)
            (error-message-invalid-result validation-result))]
    (ui-comp/lista-mesaje r)))

(defn descarca-mesaje-automat
  [zile cif ds conf]
  (let [validation-result (v/validate-input-data zile cif)]
    (if (nil? validation-result)
      (do
        (println "Pornesc descarcarea automata a facturilor pentru cif: " cif)
        (fetch-lista-mesaje zile cif ds conf)
        (println "Am terminat descarcarea automata a facturilor pentru cif: " cif))
      (error-message-invalid-result validation-result))))

(defn efactura-action-handler [params ds conf]
  (let [a (:action params)]
    (case a
      "listare" (listeaza-mesaje params ds conf)
      "descarcare" (descarca-mesaje params ds conf))))

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

(defn transformare-xml-to-pdf [a-token xml-content id_descarcare]
  (let [;; TODO de vazut daca pot lua id_descarcare din detalii-fact sa nu mai pasez din main 
        app-dir (System/getProperty "user.dir")
        url "https://api.anaf.ro/prod/FCTEL/rest/transformare/FACT1"
        r (http/post url {:headers {"Authorization" (str "Bearer " a-token)
                                    "Content-Type" "text/plain"}
                          :body xml-content
                          :as :stream})
        body (:body r)
        _ (save-pdf app-dir (str id_descarcare ".pdf")  body)]
    {:status 200
     :body "ok"
     :headers {"content-type" "text/html"}}))

(defn set-descarcare-automata
  [c-data cif]
  (let [t (str "Activare descărcare automată facturi")
        days (range 1 60)
        days-select-vals (for [n days]
                           [:option {:value n} n])
        c-status (:desc_aut_status c-data)
        s (desc_aut_status_on? c-status)]
    (h/html
     [:div#main-container.block
      (ui-comp/title t)
      [:article.message
       [:div#status.message-body (if s
                                   (str " - descărcarea automată a facturilor a fost pornită")
                                   "to be continued")]]
      [:form.block {:hx-get "/pornire-descarcare-automata"
                    :hx-target "#status"
                    :hx-swap "innerHTML swap:1s"}
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
       [:div {:class "field"}
        [:input {:id "descarcare-automata"
                 :type "checkbox"
                 :name "descarcare-automata"
                 :class "switch is-rounded is-info"}]
        [:label {:for "descarcare-automata"} "Activează descarcarea automată"]]
       [:button.button.is-small.is-link {:type "submit"} "Setează"]]])))

#_(defn pornire-descarcare-automata [req]
    (let [now (java.time.LocalDateTime/now)
          formatter (jt/formatter "H:mm - MMMM dd, yyyy")
          f-date (jt/format formatter now)
          timer (Timer. true)
          timer-task (proxy [TimerTask] []
                       (run []
                         (try
                           (println (str "Task ||< descarcare-automata-facturi >|| started at: " (Date.)))
                           (descarca-mesaje req)
                           (println (str "Task finished at: " (Date.)))
                           (catch Exception e
                             (println (str "Task failed: " (.getMessage e)))
                             (.cancel timer))))) ;; Use the local timer to cancel the task if it fails
          interval-24h (* 24 60 60 1000)]
      (reset! timer-atom {:started f-date :status timer})
      (.scheduleAtFixedRate timer timer-task 0 interval-24h)))



(defn descarcare-automata-facturi [params ds]
  (let [{:keys [cif descarcare-automata]} params
        company-data (db/get-company-data ds cif)
        id (:id company-data)]
    (if descarcare-automata
      (try
        (db/update-company-desc-aut-status ds id "on")
        (println "updated for company-id: " id " status ON")
        #_(pornire-descarcare-automata req)
        {:status 200
         :body "Ai activat cu succes serviciul de descărcare automată"
         :headers {"content-type" "text/html"}}
        (catch Exception e
          (println (str "Task failed to start: " (.getMessage e)))))
      (do #_(oprire-descarcare-automata)
       (db/update-company-desc-aut-status ds id "off")
          (println "canceling timer, set for comp-id " id " status OFF")
          {:status 200
           :body "Ai dezactivat serviciul de descărcare automată"
           :headers {"content-type" "text/html"}}))))

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
    (submit-download-proc live-companies db conf {:interval-zile 5})))

(defn pornire-serviciu-descarcare-automata [db conf]
  (let [interval-executare 30
        initial-delay 0]
    (println "Initialising automatic download for every company with desc_aut_status \"on\", at every " interval-executare " minutes" )
    (.scheduleAtFixedRate scheduler/sched-pool
                          (fn [] (schedule-descarcare-automata-per-comp db conf))
                          initial-delay
                          interval-executare
                          TimeUnit/MINUTES)))

(comment
  
  #_(pornire-serviciu-descarcare-automata)
  (.shutdown scheduler/sched-pool)

  (defn some-job []
    (println "writing to DB and other stuff"))

  (company_desc_aut_status ds "35586426")
  (desc_aut_status_on? "on")



  0
  )
