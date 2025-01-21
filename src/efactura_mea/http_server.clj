(ns efactura-mea.http-server
  (:require
   [babashka.fs :as fs]
   [efactura-mea.config :as config]
   [efactura-mea.web.api :as api]
   [efactura-mea.web.middleware :refer [wrap-app-config]]
   [efactura-mea.web.routes :refer [routes]]
   [mount.core :as mount :refer [defstate]]
   [muuntaja.middleware :as middleware]
   [reitit.ring :as reitit]
   [ring.adapter.jetty9 :refer [run-jetty stop-server]]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.defaults :as rmd]
   [ring.middleware.file :refer [wrap-file]]
   [ring.middleware.lint :refer [wrap-lint]]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-web]]
   [ring.middleware.webjars :refer [wrap-webjars]]))



(defn app
  [conf]
  (let [download-dir (config/download-dir conf)
        dev-mode? (:dev-mode? conf)
        site-defaults (if dev-mode?
                        rmd/site-defaults
                        rmd/secure-site-defaults)
        site-defaults (-> site-defaults
                      ;; change session cookie ID - avoid disclosing information
                          (assoc-in [:session :cookie-name] "SID"))]
    (api/create-dir-structure conf)
    (fs/create-dirs download-dir)
    ;; TODO: să mutăm middleware în ring / declarative
    (cond-> (reitit/router (routes))
      true (wrap-app-config)
      true (middleware/wrap-format)
      true (rmd/wrap-defaults site-defaults)
      true (wrap-file download-dir)
      true (wrap-file (get-in conf [:server :public-path]))
      dev-mode? (wrap-lint)
      dev-mode? (wrap-stacktrace-web)
      true (wrap-webjars)
      true (wrap-content-type))))

(defstate server
  :start (run-jetty (app config/conf) (:jetty config/conf))
  :stop (stop-server server))
