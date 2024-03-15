(ns efactura-mea.oauth2-anaf
  (:require [ring.util.codec :refer [url-encode]]
            [babashka.http-client :as http]
            [clojure.tools.logging :as log]))

(def default-anaf-url-logincert "https://logincert.anaf.ro")

(def default-anaf-api-version "v1")

(defn make-query-string
  "Build a query string from a map of keys + values.
   Values are url-encoded."
  [m]
  (->> (for [[k v] m]
         (str (url-encode k) "=" (url-encode v)))
       (interpose "&")
       (apply str)))

(defn build-url
  "Build a url from a base and a query map."
  [url-base query-map]
  (str url-base "?" (make-query-string query-map)))

(defn base-authorization-url
  [logingert-url api-version]
  (str logingert-url "/anaf-oauth2/" api-version "/authorize"))

(defn authorization-url
  [client-id redirect-uri & {:as _opts
                             :keys [anaf-url-logincert
                                    anaf-api-version params]
                             :or {anaf-url-logincert default-anaf-url-logincert
                                  anaf-api-version default-anaf-api-version}}]
  (build-url (base-authorization-url anaf-url-logincert anaf-api-version)
             (merge {"response_type" "code"
                     "client_id" client-id
                     "redirect_uri" redirect-uri
                     "token_content_type" "jwt"
                     "approval_prompt" "auto"} params)))


(defn token-url
  ([]
   (token-url default-anaf-url-logincert default-anaf-api-version))
  ([logingert-url api-version]
   (str logingert-url "/anaf-oauth2/" api-version "/token")))

^:rct/test
(comment

  "authorization url builds with params"
  (authorization-url "1" "https://localhost/callback")
  ;=> "https://logincert.anaf.ro/anaf-oauth2/v1/authorize?response_type=code&client_id=1&redirect_uri=https%3A%2F%2Flocalhost%2Fcallback&token_content_type=jwt&approval_prompt=auto"

  "token url"

  (token-url)
  ;=> "https://logincert.anaf.ro/anaf-oauth2/v1/token"
  (token-url default-anaf-url-logincert default-anaf-api-version)
  ;=> "https://logincert.anaf.ro/anaf-oauth2/v1/token"

  )


(defn make-anaf-login-handler
  "Cează un handler ring folosit pentru pornirea procesului de obținere
   token oauth2.

   Handler-ul face următoarele:
   Creează un URL pe care clientul trebuie să îl deschidă în navigator web.
   Trimite către utilizator un răspuns HTTP redirect 302.
   Pornește procesul de autentificare ANAF pentru obținere token oauth2.
   Utilizatorul va trebui să se autentifice cu certificat digital."
  [client-id redirect-uri ]
  (fn [_req]
    (let [resp (authorization-url client-id redirect-uri)]
      {:status 302
       :body nil
       :headers {"location" resp}})))

(defn authorization-code->tokens!
  "Apel HTTP POST pentru conversia codul de autorizare oauth2 în
   jetoane JWT pentru autentificare și împrospătare."
  [client-id client-secret redirect-uri code]
  (try
    (http/post (token-url)
               {:headers {"content-type" "application/x-www-form-urlencoded"}
                :form-params {"grant_type" "authorization_code"
                              "code" code
                              "redirect_uri" redirect-uri
                              "token_content_type" "jwt"
                              "client_id" client-id
                              "client_secret" client-secret}})
    (catch Exception e
      (log/warn e "Exception")
      (ex-data e))))

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
    (log/info "Get authorization token")
    (let [{:keys [query-params]} req
          code (get query-params "code")
          res (authorization-code->tokens! client-id client-secret redirect-uri code)]
      {:status 200
       :body "" ;;(res->str res)
       :headers {"content-type" "application/json"}})))

