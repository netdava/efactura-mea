(ns ro.ieugen.efacturier
  (:require [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [mount.core :as mount :refer [defstate]]
            [mount-up.core :as mu]
            [reitit.ring :as reitit]
            ;; [muuntaja.middleware :as muuntaja]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.defaults :as rmd]
            [ring.adapter.jetty9 :refer [run-jetty stop-server]]))

(mu/on-upndown :info mu/log :before)

(defn load-config
  [file]
  (try
    (edn/read-string (slurp file))
    (catch Exception e
      (log/error e "Failed to load config."))))

(defstate config
  :start (load-config "test/resources/config.edn"))

(defn html-handler
  [_]
  {:status 200, :body "ok"})

(defn anaf-callback
  [req]
  {:status 200
   :body "anaf-callback"
   :headers {"content-type" "text/html"}})

(def routes
  [["/" {:get html-handler}]
   ["/api/v1/oauth/anaf-callback" {:get anaf-callback}]])

(def handler
  (reitit/ring-handler
   (reitit/router routes)))

(defn app
  []
  (-> #'handler
      (rmd/wrap-defaults rmd/site-defaults)
      (wrap-file (:public-path config))
      (wrap-file "node_modules")))

(defstate server
  :start (run-jetty (app) {:port 8080
                         :join? false
                         :h2c? true  ;; enable cleartext http/2
                         :h2? true   ;; enable http/2
                         :ssl? true  ;; ssl is required for http/2
                         :ssl-port 8123
                         :keystore "dev-resources/keystore.jks"
                         :key-password "dev123"
                         :keystore-type "jks"
                         :sni-host-check? false})
  :stop (stop-server server))

(defn -main [& args]
  (mount/start))

(comment

  (mount/start)
  (mount/stop)

  (println config)


  0
  )
