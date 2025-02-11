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
   [reitit.core :as r]
   [reitit.openapi :as openapi]
   [reitit.ring :as ring]
   [reitit.ring.middleware.defaults :refer [defaults-middleware]]
   [reitit.ring.middleware.dev]
   [ring.adapter.jetty9 :refer [run-jetty stop-server]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.defaults :as rmd
    :refer [api-defaults secure-site-defaults site-defaults]]
   [ring.middleware.oauth2 :refer [wrap-oauth2]]
   [ring.middleware.session.memory :as memory]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-web]]
   [ring.middleware.webjars :refer [wrap-webjars]]
   [ring.util.response :as rur]))

(def session-data (atom {}))
;; single instance
(def session-store (memory/memory-store session-data))

(defn routes
  []
  [["/" {:handler #'home/handle-homepage}]
   ["/openapi.json" {:get {:handler (openapi/create-openapi-handler)
                           :openapi {:info {:title "my nice api" :version "0.0.1"}}
                           :no-doc true}}]
   ["/oauth2/main/done" {:handler (fn [_req] (rur/redirect "/"))}]
   ["/ui"
    ["/companii" (companii/routes)]
    ["/anaf" (anaf/routes)]]
   ["/api" {:defaults api-defaults}
    ["/v1/oauth/anaf-callback" #'anaf/make-authorization-token-handler]
    ["/alfa"
     ["/pornire-descarcare-automata" #'api/handler-descarcare-automata-facturi]
     ["/listare-sau-descarcare" #'api/handle-list-or-download]
     ["/transformare-xml-pdf" #'api/handler-descarca-factura-pdf]
     ["/descarca-arhiva" #'da/handler-descarca-arhiva]
     ["/sumar-descarcare-arhiva" #'da/handler-sumar-descarcare-arhiva]]]])

;; putem folosi handlerul pentru teste
;; https://cljdoc.org/d/metosin/reitit/0.7.2/doc/ring/ring-router#request-method-based-routing
(defstate main-handler
  :start
  (let [conf config/conf
        dev-mode? (:dev-mode? conf)
        download-dir (config/download-dir conf)
        public-path (get-in conf [:server :public-path])
        auth-profiles (get-in conf [:authentication :profiles])
        my-site-defaults (-> (if dev-mode? site-defaults secure-site-defaults)
                             (assoc-in [:session :store] session-store)
                                     ;; change session cookie ID - avoid disclosing information
                             (assoc-in [:session :cookie-name] "SID")
                                     ;; https://github.com/weavejester/ring-oauth2 
                                     ;; for redirect to auth service - lax cookie
                             (assoc-in [:session :cookie-attrs :same-site] :lax))
        dev-middleware [#_[wrap-lint]
                        [wrap-stacktrace-web]]
        ring-handler-middleware [wrap-webjars
                                 wrap-app-config
                                 [wrap-session (:session my-site-defaults false)]
                                 [wrap-params (get-in my-site-defaults [:params :urlencoded] false)]
                                 [wrap-oauth2 auth-profiles]]
        ring-handler-middleware (if dev-mode?
                                  (into [] (concat dev-middleware ring-handler-middleware))
                                  ring-handler-middleware)
        router-opts {;; difuri drăguțe
                    ;;  :reitit.middleware/transform reitit.ring.middleware.dev/print-request-diffs 
                     :data {:middleware defaults-middleware
                            :defaults my-site-defaults
                            ;; Muuntaja instance for content negotiation
                            :muuntaja muuntaja/instance
                            ;; Request and response coercion -- using Malli in this case.
                            :coercion reitit.coercion.malli/coercion}}]
    (log/info "Ensure directories" download-dir " " public-path)
    (api/create-dir-structure conf)
    (fs/create-dirs download-dir)
    (log/debug "Create ring handler")
    (ring/ring-handler
     (ring/router (routes) router-opts)
     (ring/routes
      (ring/create-resource-handler {:path "public"})
      (ring/create-resource-handler {:path "swagger-ui"})
      (ring/create-file-handler {:root download-dir :path "/data"})
      (ring/create-file-handler {:path "/"})
      (ring/create-default-handler))
     {:middleware ring-handler-middleware}))

  :stop nil)

(defstate server
  :start (run-jetty main-handler (:jetty config/conf))
  :stop (stop-server server))


(comment

  @session-data

  (reset! session-data {})

  (rur/redirect "/")

  (-> main-handler (ring/get-router) (r/compiled-routes))

  (into []
        (concat [[wrap-webjars]
                 [wrap-app-config]]
                defaults-middleware))

  (use 'clojure.tools.trace)
  (trace-ns ring.middleware.oauth2)

  (main-handler {:request-method :get, :uri "/"}) 
  (main-handler {:request-method :get, :uri "/css/style.css"})

  (main-handler {:request-method :get, :uri "/oauth2/main/login"})
  (main-handler {:request-method :get, :uri "/oauth2/main/done"})
  (main-handler {:request-method :get, :uri "/oauth2/main/don"})

  (main-handler {:request-method :get, :uri "/openapi.json"})
  (main-handler {:request-method :get, :uri "/swagger-ui"})
  (main-handler {:request-method :get, :uri "/swagger-ui/index.html"})

  (main-handler {:request-method :get, :uri "/favicon.ico"})
  (main-handler {:request-method :get, :uri "/api/get"})
  )
  
