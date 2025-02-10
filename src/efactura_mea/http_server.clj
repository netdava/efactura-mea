(ns efactura-mea.http-server
  (:require
   [babashka.fs :as fs]
   [clojure.tools.logging :as log]
   [efactura-mea.config :as config]
   [efactura-mea.web.anaf-integrare :as anaf]
   [efactura-mea.web.api :as api]
   [efactura-mea.web.companii :as companii]
   [efactura-mea.web.descarca-arhiva :as da]
   [efactura-mea.web.home :as home]
   [efactura-mea.web.middleware :refer [wrap-app-config]]
   [mount.core :as mount :refer [defstate]]
   [muuntaja.core :as muuntaja]
   [reitit.coercion.malli]
   [reitit.openapi :as openapi]
   [reitit.ring :as ring]
   [reitit.ring.middleware.defaults :refer [defaults-middleware]]
   [reitit.ring.middleware.dev]
   [ring.middleware.session.memory :as memory]
   [ring.adapter.jetty9 :refer [run-jetty stop-server]]
   [ring.middleware.defaults :as rmd :refer [api-defaults site-defaults secure-site-defaults]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-web]]
   [ring.middleware.webjars :refer [wrap-webjars]]))

#_(def handler-ouath
    (wrap-oauth2
     routes
     {:github
      {:authorize-uri    "https://github.com/login/oauth/authorize"
       :access-token-uri "https://github.com/login/oauth/access_token"
       :client-id        "abcabcabc"
       :client-secret    "xyzxyzxyzxyzxyz"
       :scopes           ["user:email"]
       :launch-uri       "/oauth2/github"
       :redirect-uri     "/oauth2/github/callback"
       :landing-uri      "/"}}))


(def session-data (atom {}))
;; single instance
(def session-store (memory/memory-store session-data))

(defn routes
  [opts]
  (let [{:keys [my-site-defaults my-api-defaults]} opts]
    [["/" {:middleware defaults-middleware
           :defaults my-site-defaults
           ;; Muuntaja instance for content negotiation
           :muuntaja muuntaja/instance
           ;; Request and response coercion -- using Malli in this case.
           :coercion reitit.coercion.malli/coercion
           :handler #'home/handle-homepage}]
     ["/openapi.json" {:get {:handler (openapi/create-openapi-handler)
                             :openapi {:info {:title "my nice api" :version "0.0.1"}}
                             :no-doc true}}]
     ["/ui" {:middleware defaults-middleware
             :defaults   my-site-defaults
             ;; Muuntaja instance for content negotiation
             :muuntaja muuntaja/instance
             ;; Request and response coercion -- using Malli in this case.
             :coercion reitit.coercion.malli/coercion}
      ["/companii" (companii/routes)]
      ["/anaf" (anaf/routes)]]
     ["/api" {:middleware defaults-middleware
              :defaults   my-api-defaults
              ;; Muuntaja instance for content negotiation
              :muuntaja muuntaja/instance
              ;; Request and response coercion -- using Malli in this case.
              :coercion reitit.coercion.malli/coercion}
      ["/v1/oauth/anaf-callback" #'anaf/make-authorization-token-handler]
      ["/alfa"
       ["/pornire-descarcare-automata" #'api/handler-descarcare-automata-facturi]
       ["/listare-sau-descarcare" #'api/handle-list-or-download]
       ["/listare-mesaje" {:name ::listare-mesaje
                           :get #'api/handle-list-messages}]
       ["/transformare-xml-pdf" #'api/handler-descarca-factura-pdf]
       ["/descarca-arhiva" #'da/handler-descarca-arhiva]
       ["/sumar-descarcare-arhiva" #'da/handler-sumar-descarcare-arhiva]]]]))

;; putem folosi handlerul pentru teste
;; https://cljdoc.org/d/metosin/reitit/0.7.2/doc/ring/ring-router#request-method-based-routing
(defstate main-handler
  :start
  (let [conf config/conf
        dev-mode? (:dev-mode? conf)
        download-dir (config/download-dir conf)
        public-path (get-in conf [:server :public-path])
        dev-middleware [#_[wrap-lint]
                        [wrap-stacktrace-web]]
        middleware [[wrap-webjars]
                    [wrap-app-config]]
        middleware (if dev-mode?
                     (into [] (concat dev-middleware middleware))
                     middleware)
        router-opts {;; difuri drăguțe
                    ;;  :reitit.middleware/transform reitit.ring.middleware.dev/print-request-diffs
                     ;; 
                     }
        my-site-defaults (-> (if dev-mode? site-defaults secure-site-defaults)
                             (assoc-in [:session :store] session-store)
                               ;; change session cookie ID - avoid disclosing information
                             (assoc-in [:session :cookie-name] "SID"))
        my-api-defaults (-> api-defaults
                            ;; Enable exception middleware.  You can also add custom
                            ;; handlers in the [:exception handlers] key and they
                            ;; will be passed to create-exception-middleware.
                            (assoc :exception true))
        main-routes (routes {:my-api-defaults my-api-defaults :my-site-defaults my-site-defaults})]
    (log/info "Ensure directories" download-dir " " public-path)
    (api/create-dir-structure conf)
    (fs/create-dirs download-dir)
    (log/debug "Create ring handler")
    (ring/ring-handler
     (ring/router main-routes router-opts)
     (ring/routes
      (ring/create-resource-handler {:path "public"})
      (ring/create-resource-handler {:path "swagger-ui"})
      (ring/create-file-handler {:root download-dir :path "/data"})
      (ring/create-file-handler {:path "/"})
      (ring/create-default-handler))
     {:middleware middleware}))

  :stop nil)

(defstate server
  :start (run-jetty main-handler (:jetty config/conf))
  :stop (stop-server server))


(comment

  @session-data

  (main-handler {:request-method :get, :uri "/"})
  (main-handler {:request-method :get, :uri "/css/style.css"})

  (main-handler {:request-method :get, :uri "/openapi.json"})
  (main-handler {:request-method :get, :uri "/swagger-ui"})
  (main-handler {:request-method :get, :uri "/swagger-ui/index.html"})

  (main-handler {:request-method :get, :uri "/favicon.ico"})
  (main-handler {:request-method :get, :uri "/api/get"})
  )
