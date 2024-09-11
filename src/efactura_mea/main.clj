(ns efactura-mea.main
  (:require [clj-commons.byte-streams :as bs]
            [efactura-mea.web.api :as api]
            [efactura-mea.web.json :as wj]
            [efactura-mea.web.oauth2-anaf :as o2a]
            [mount-up.core :as mu]
            [mount.core :as mount :refer [defstate]]
            [muuntaja.core :as m]
            [muuntaja.middleware :as middleware]
            [cprop.core :refer [load-config]]
            [reitit.ring :as reitit]
            [ring.adapter.jetty9 :refer [run-jetty stop-server]]
            [ring.middleware.defaults :as rmd]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [efactura-mea.layout :as layout]
            [efactura-mea.ui.componente :as ui]
            [efactura-mea.db.facturi :as f]
            [efactura-mea.db.db-ops :as db]
            [efactura-mea.db.next-jdbc-adapter :as adapter]
            [next.jdbc :as jdbc])
  (:gen-class))

(mu/on-upndown :info mu/log :before)

(defstate conf
  :start (load-config))

(defstate ds :start
  (let [db-spec (:db-spec conf)]
    (adapter/set-next-jdbc-adapter)
    (jdbc/get-datasource db-spec)))

(defn descarca-factura-pdf 
  [req]
  (let [{:keys [params ds config]} req
        {:keys [id_descarcare]} params
        detalii-fact (db/detalii-factura-anaf ds id_descarcare)
        {:keys [cif]} detalii-fact
        a-token (db/fetch-access-token ds cif)
        xml-data (api/zip-file->xml-data config detalii-fact)]
    (api/transformare-xml-to-pdf a-token xml-data id_descarcare)))

(defn handle-facturi
  [content-fn req]
  (let [{:keys [path-params]} req
        cif (:cif path-params)
        content (content-fn cif)
        sidebar (ui/sidebar-company-data cif)]
    (layout/main-layout content sidebar)))

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

(defn wrap-app-config [handler]
    (fn [request]
      (let [updated-request (assoc request :config conf :ds ds)]
        (handler updated-request))))

(defn routes
  [anaf-conf]
  [["/" (fn [_] (layout/main-layout "Bine ai venit în e-Factura, User Admin" (ui/sidebar-select-company)))]
   ["/companii" (fn [_]
                  (let [content (api/afisare-companii-inregistrate ds)
                        sidebar (ui/sidebar-select-company)]
                    (layout/main-layout content sidebar)))]
   ["/inregistrare-noua-companie" (fn [_]
                                    (let [content (api/formular-inregistrare-companie)
                                          sidebar (ui/sidebar-select-company)]
                                      (layout/main-layout content sidebar)))]
   ["/inregistreaza-companie" (fn [req]
                                (let [{:keys [params]} req]
                                  (api/inregistrare-noua-companie ds params)))]
   #_["/" (fn [_] (layout/main-layout "Hello, Admin!"))]
   ["/facturi/:cif" (fn [req]
                      (handle-facturi ui/facturi-descarcate req))]
   ["/facturi-spv/:cif" (fn [req]
                          (handle-facturi ui/facturi-spv req))]
   ["/login-anaf" (o2a/make-anaf-login-handler
                   (anaf-conf :client-id)
                   (anaf-conf :redirect-uri))]
   ["/api/v1/oauth/anaf-callback" (o2a/make-authorization-token-handler
                                   (anaf-conf :client-id)
                                   (anaf-conf :client-secret)
                                   (anaf-conf :redirect-uri))]
   ["/listare-sau-descarcare" (fn [req]
                                (let [{:keys [params]} req]
                                  (api/efactura-action-handler params ds conf)))]
   ["/facturile-mele/:cif" (fn [req]
                             (let [{:keys [path-params]} req]
                               (api/afisare-facturile-mele path-params conf ds)))]
   ["/transformare-xml-pdf" descarca-factura-pdf]
   ["/logs/:cif" (fn [req]
                   (let [{:keys [path-params ds]} req
                         cif (:cif path-params)
                         content (ui/logs-api-calls cif ds)
                         sidebar (ui/sidebar-company-data cif)]
                     (layout/main-layout content sidebar)))]
   ["/descarcare-automata/:cif" (fn [req]
                                  (let [{:keys [path-params]} req
                                        cif (:cif path-params)
                                        sidebar (ui/sidebar-company-data cif)
                                        c-data (db/get-company-data ds cif)
                                        content (api/set-descarcare-automata c-data cif)]
                                    (layout/main-layout content sidebar)))]
   ["/pornire-descarcare-automata" (fn [req]
                                     (let [params (:params req)]
                                       (api/descarcare-automata-facturi params ds)))]])



(defn handler
  [conf]
  (reitit/ring-handler
   (reitit/router (routes (get-in conf [:anaf])))))

(defn app
  [conf]
  (api/create-dir-structure conf)
  (-> (handler conf)
      (wrap-app-config)
      (middleware/wrap-format)
      (rmd/wrap-defaults rmd/site-defaults)
      (wrap-file "data")
      (wrap-file (get-in conf [:server :public-path]))
      (wrap-webjars)))

(defstate server
  :start (run-jetty (app conf) (:jetty conf))
  :stop (stop-server server))

(defn -main [& args]
  (mount/start)
  (api/init-db ds)
  (api/pornire-serviciu-descarcare-automata ds conf))

(comment
  (-main)
  (mount/start)
  (mount/stop)

  (f/select-detalii-factura-descarcata ds "3412523350")
  (db/test-companie-inregistrata ds "35586426")
  (seq [])
  0)
