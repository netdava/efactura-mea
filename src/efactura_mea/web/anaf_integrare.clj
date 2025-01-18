(ns efactura-mea.web.anaf-integrare
  "Integrare cu API ANAF pentru a genera / reîmprospăta token:
   
   Authorization Endpoint 
   https://logincert.anaf.ro/anaf-oauth2/v1/authorize
   Token Issuance Endpoint
   https://logincert.anaf.ro/anaf-oauth2/v1/token
   Token Revocation Endpoint
   https://logincert.anaf.ro/anaf-oauth2/v1/revoke

   "
  (:require
   [clojure.tools.logging :as log]
   [babashka.http-client :as http]
   [efactura-mea.db.facturi :as fdb]
   [efactura-mea.db.db-ops :as db-ops]
   [efactura-mea.util :as u]
   [efactura-mea.web.json :as wj]
   [jsonista.core :as json]
   [efactura-mea.web.layout :as layout]
   [efactura-mea.web.anaf.oauth2 :as o2a]
   [efactura-mea.web.ui.componente :as ui]
   [hiccup2.core :as h]
   [muuntaja.core :as m]
   [reitit.core :as r]
   [ring.util.response :as rur]
   [java-time.api :as jt])
  (:import
   [java.time.temporal ChronoUnit]))

;; todo: 
;; - funcționalitate de revocare a tokenului - în caz de compromitere
;; 

(defn content-integrare-efactura
  [req]
  (let [{:keys [path-params ::r/router]} req
        {:keys [cif]} path-params
        url-autorizare (-> router
                           (r/match-by-name ::autorizare {:cif cif})
                           :path)]
    (h/html
     (ui/title "Integrare E-factura")
     [:div.content
      [:p "Activează integrarea automata cu E-factura "]
      [:div.columns
       [:div.column
        [:div.notification
         [:p "Dacă " [:span.has-text-weight-bold "ai permisiunea "]
          "de autentificare în S.P.V." [:span.has-text-weight-bold " cu certificatul digital:"]]
         [:a.button.is-link {:href url-autorizare}
          (str "Autorizează accesul pentru CUI:" cif)]]]
       [:div.column
        [:div.notification
         [:p [:span.has-text-weight-bold "Fără permisiunea "]
          "de autentificare în S.P.V." [:span.has-text-weight-bold " cu certificatul digital:"]] 
         [:button.button.is-link {:disabled true
                                  :hx-get url-autorizare
                                  :hx-target "#modal-wrapper"
                                  :hx-swap "innerHTML"}
          (str "Autorizează accesul pentru CUI:" cif)]
         [:p.is-small "Nu este implementat încă."]]]]
      [:div#modal-wrapper]])))

(defn page-anaf-integrare
  [req]
  (let [{:keys [path-params ::r/router]} req
        {:keys [cif]} path-params
        content (content-integrare-efactura req)
        sidebar (layout/sidebar-company-data {:cif cif :router router})]
    (layout/main-layout content sidebar)))

(defn modala-link-autorizare
  [_]
  (str (h/html [:div.modal.is-active
                {:_ "on closeModal remove .is-active then remove me"}
                [:div.modal-background]
                [:div.modal-content
                 [:div.box [:div
                            [:p.title.is-5 "Obține accesul prin delegat (persoană cu acces în S.P.V.)"]
                            [:hr.title-hr]
                            [:p "Copiază și trimite link-ul de mai jos pentru autorizarea accesului eFacturaMea în e-Factura contabilului tău, respectiv persoanei care poate accesa S.P.V. ANAF cu certificat digital pentru firma ta."]
                            [:div.block
                             [:textarea.textarea "ceva text in textarea"]]
                            [:div.buttons
                             [:button.button.is-small "Copiază"]
                             [:button.button.is-small "Trimite pe mail"]]]]]
                [:button.modal-close.is-large {:aria-label "close"
                                               :_ "on click trigger closeModal"}]])))

(defn handler-autorizare
  [req]
  (let [{:keys [path-params session conf]} req
        {:keys [cif]} path-params
        ;; content (modala-link-autorizare cif)
        session (assoc session ::anaf-autorizare-cif cif)
        {:keys [client-id redirect-uri]} (:anaf conf)
        url (o2a/authorization-url client-id redirect-uri)]
    (-> {:status 303
         :headers {"content-type" "text/html"
                   "Location" url}}
        (assoc :session session))))


(defn make-authorization-token-handler
  "Crează un handler ring pentru procesul oauth2 de conversie cod autorizare
   în jeton autentificare și jeton de împrospătare.

   Handlerul primește o cerere HTTP cu codul de autorizare.
   Face un apel POST către serverul de autorizare cu codul primit
   și secretul înregistrat.

   Întoarce jetoanele de autentificare sau o structură de eroare.
  "
  [client-id client-secret redirect-uri]
  (fn [req]
    (let [{:keys [query-params session ds]} req
          cif (:efactura-mea.web.anaf-integrare/anaf-autorizare-cif session)
          code (get query-params "code")]
      (log/info (str "Get authorization token " cif))
      (try
        (let [res (o2a/authorization-code->tokens! client-id client-secret redirect-uri code)
              {:keys [status body]} res]
          (if (= status 200)
            (let [data (m/decode wj/m "application/json" body)
                  now (u/date-time-now-utc)
                  expires-in (:expires_in data)
                  expiration-date (u/expiration-date now expires-in)
                  data (assoc data
                              :cif cif
                              :updated now
                              :expiration_date expiration-date)]
              (log/info "Received " data)
              (fdb/insert-company-tokens ds data)
              (-> (rur/response (m/encode wj/m "application/json" data))
                  (rur/content-type "application/json")))
            (throw (ex-info "Error getting token" {:response res}))))
        (catch Exception e
          (log/info e (str "Exception" (ex-cause e)))
          (throw e))))))


(defn save-refreshed-token
  [ds opts]
  (println "incep cu functia SAVE_REFRESHED_TOKEN")
  (let [{:keys [body cif]} opts
        b (json/read-value body)
        access-token (get b "access_token")
        refresh-token (get b "refresh_token")
        expires-in (get b "expires_in")
        now (u/date-time-now-utc)
        expiration-date (u/expiration-date now expires-in)
        save-opts {:cif cif
                   :access_token access-token
                   :refresh_token refresh-token
                   :expiration_date expiration-date
                   :expires_in expires-in
                   :_updated now}
        _ (db-ops/save-refreshed-token-data! ds save-opts)]
    {:status 200
     :body "Token refreshed"
     :headers {"content-type" "text/html"}}))


(defn handler-refresh-token
  [client-id client-secret]
  (fn [req]
    (let [{:keys [path-params ds uri]} req
          {:keys [cif]} path-params
          token-data (db-ops/fetch-company-token-data ds cif)
          {:keys [refresh_token retries_count]} token-data
          refresh-token-anaf-uri "https://logincert.anaf.ro/anaf-oauth2/v1/token"
          opts {:basic-auth [client-id client-secret]
                :form-params {:grant_type "refresh_token"
                              :refresh_token refresh_token}}]
      (try
        (loop [retries retries_count]
          (if (<= retries 5)
            (let [response (http/post refresh-token-anaf-uri opts)
                  {:keys [status body]} response
                  opts {:body body :cif cif :uri uri}]
              (if (= 200 status)
                (do
                  (log/info "Refresh token reușit!")
                  (db-ops/update-retries-counter! ds cif 0)
                  (log/info "test dupa db action :"))
                (do
                  (log/info "Eroare obtinere refresh-token, încerc din nou...")
                  (db-ops/update-retries-counter! ds cif retries)
                  (recur (inc retries)))))
            (log/info "Am atins limita maximă de încercări!")))
        (catch Exception e
          (log/info e (str "Exception" (ex-cause e)))
          (throw e)))
      {:status 200
       :body "refreshed token"
       :headers {"Content-type" "text/html"}})))



(defn handler-revoke!-token
  [client-id client-secret]
  (fn [req]
    (let [{:keys [path-params ds]} req
          {:keys [cif]} path-params
          revoke-token (db-ops/fetch-company-refresh-token ds cif)
          revoke-token-anaf-uri "https://logincert.anaf.ro/anaf-oauth2/v1/revoke"
          opts {:basic-auth [client-id client-secret]
                :form-params {:token revoke-token}}]
      (try
        (let [response (http/post revoke-token-anaf-uri opts)
              {:keys [status]} response]
          (if (= 200 status)
            ;; TODO: de implementat mesaj de informare
            (log/info "Tokenul a fost revocat cu succes!")
            (throw (ex-info "Failed to refresh token" {:status status
                                                       :response response}))))
        (catch Exception e
          (log/info e (str "Exception" (ex-cause e)))
          (throw e))))))

(defn days-until-expiration [expiration-str]
  (let [now (jt/local-date-time)
        expiration (jt/zoned-date-time expiration-str)
        days-difference (.between ChronoUnit/DAYS now expiration)]
    days-difference))

(defn routes
  [anaf-conf]
  [["/login-anaf" (o2a/make-anaf-login-handler
                   (anaf-conf :client-id)
                   (anaf-conf :redirect-uri))]
   ["/integrare/:cif" {:name ::integrare
                       :get page-anaf-integrare}]
   ["/autorizeaza-acces/:cif" {:name ::autorizare
                               :get handler-autorizare}]
   ["/refresh-access-token/:cif" (handler-refresh-token
                                  (anaf-conf :client-id)
                                  (anaf-conf :client-secret))]
   ["/revoke-token/:cif" (handler-revoke!-token
                                  (anaf-conf :client-id)
                                  (anaf-conf :client-secret))]])


(comment

  (->
   (r/router (routes {}))
   (r/match-by-name ::integrare {:cif "1234"})
   :path)
  ;;=> "/integrare/1234"
  
  (-> (r/router (routes {}))
      (r/match-by-name ::autorizare {:cif "123"})
      :path)
  ;;=> "/autorizeaza-acces/123" 
  )
