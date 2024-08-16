(ns efactura-mea.main
  (:require [clj-commons.byte-streams :as bs]
            [efactura-mea.web.api :as api]
            [efactura-mea.web.json :as wj]
            [efactura-mea.web.oauth2-anaf :as o2a]
            [mount-up.core :as mu]
            [mount.core :as mount :refer [defstate]]
            [muuntaja.core :as m]
            [muuntaja.middleware :as middleware]
            [reitit.ring :as reitit]
            [ring.adapter.jetty9 :refer [run-jetty stop-server]]
            [ring.middleware.defaults :as rmd]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [efactura-mea.layout :as layout]
            [efactura-mea.ui.componente :as ui])
  (:gen-class))

(mu/on-upndown :info mu/log :before)


(defn req->str
  [req]
  (let [body (:body req)
        bis (m/encode wj/m
                      "application/json"
                      (-> req
                          (assoc :body (bs/to-string body))
                          (dissoc :reitit.core/router
                                  :reitit.core/match)))]
    (bs/to-string bis)))

(defn routes
  [anaf-conf]
  [["/" (fn [_] (layout/main-layout "Hello, Admin!"))]
   ["/facturi/:cif" (fn [req] (layout/main-layout (ui/facturi-descarcate req)))]
   ["/facturi-spv/:cif" (fn [req] (layout/main-layout (ui/facturi-spv req)))]
   ["/login-anaf" (o2a/make-anaf-login-handler
                   (anaf-conf :client-id)
                   (anaf-conf :redirect-uri))]
   ["/api/v1/oauth/anaf-callback" (o2a/make-authorization-token-handler
                                   (anaf-conf :client-id)
                                   (anaf-conf :client-secret)
                                   (anaf-conf :redirect-uri))]
   ["/listare-sau-descarcare" (fn [request]
                                (api/efactura-action-handler request))]
   ["/facturile-mele/:cif" (fn [request] (api/afisare-facturile-mele request))]
   ["/transformare-xml-pdf" (fn [req] (api/transformare-xml-to-pdf req))]
   ["/logs/:cif" (fn [req] (layout/main-layout (api/log-api-calls req)))]
   ["/descarcare-automata/:cif" (fn [req] (layout/main-layout (api/set-descarcare-automata req)))]
   ["/pornire-descarcare-automata" (fn [req] (api/descarcare-automata-facturi req))]])

(defn handler
  [conf]
  (reitit/ring-handler
   (reitit/router (routes (get-in conf [:anaf])))))

(defn app
  [conf]
  (-> (handler conf)
      (middleware/wrap-format)
      (rmd/wrap-defaults rmd/site-defaults)
      (wrap-file "data")
      (wrap-file (get-in conf [:server :public-path]))
      (wrap-webjars)))

(defstate server
  :start (run-jetty (app api/conf) (:jetty api/conf))
  :stop (stop-server server))

(defn -main [& args]
  (mount/start)
  (api/init-db))

(comment
  (-main)
  (mount/start)
  (mount/stop)
  0)
