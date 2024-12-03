(ns efactura-mea.main
  (:require [clj-commons.byte-streams :as bs]
            [cprop.core :refer [load-config]]
            [efactura-mea.db.db-ops :as db]
            [efactura-mea.db.next-jdbc-adapter :as adapter]
            [efactura-mea.layout :as layout]
            [efactura-mea.ui.componente :as ui]
            [efactura-mea.util :as u]
            [efactura-mea.web.api :as api]
            [efactura-mea.web.json :as wj]
            [efactura-mea.web.oauth2-anaf :as o2a]
            [efactura-mea.db.facturi :as f]
            [efactura-mea.web.descarca-exporta :as de]
            [efactura-mea.config :as config]
            [mount-up.core :as mu]
            [mount.core :as mount :refer [defstate]]
            [muuntaja.core :as m]
            [muuntaja.middleware :as middleware]
            [next.jdbc :as jdbc]
            [reitit.ring :as reitit]
            [ring.adapter.jetty9 :refer [run-jetty stop-server]]
            [ring.middleware.defaults :as rmd]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.webjars :refer [wrap-webjars]]
            [ring.middleware.content-type :refer [wrap-content-type]])
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
  (let [{:keys [params ds conf]} req
        {:keys [id_descarcare]} params
        detalii-fact (db/detalii-factura-anaf ds id_descarcare)
        {:keys [cif]} detalii-fact
        a-token (db/fetch-access-token ds cif)
        xml-data (api/zip-file->xml-data conf detalii-fact)]
    (api/transformare-xml-to-pdf a-token xml-data detalii-fact conf)))

(defn handle-facturi
  [content-fn req]
  (let [{:keys [path-params query-params ds uri headers]} req
        {:strs [page per-page]} query-params
        {:strs [hx-request]} headers
        cif (:cif path-params)
        opts {:cif cif :page page :per-page per-page :uri uri}
        content (content-fn opts ds)
        sidebar (ui/sidebar-company-data opts)]
    (if (= hx-request "true")
      content
      (layout/main-layout (:body content) sidebar))))

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
    (let [updated-request (assoc request :conf conf :ds ds)]
      (handler updated-request))))

(defn add-pagination-params-middleware [handler]
  (fn [request]
    (let [{:keys [query-params]} request
          {:strs [page per-page]} query-params
          page (or (some-> page Integer/parseInt) 1)
          per-page (or (some-> per-page Integer/parseInt) 20)
          new-params (merge query-params {"page" page "per-page" per-page})]
      ;; Apelăm handler-ul cu request-ul modificat
      (handler (assoc request :query-params new-params)))))

(defn add-path-for-download
  "Primeste lista de mesaje descarcate pentru afisare in UI
   Genereaza pentru fiecare mesaj calea unde a fost descarcat,
   pentru identificare si extragere meta-date."
  [conf mesaje-cerute]
  (reduce (fn [acc mesaj]
            (let [download-dir (config/download-dir conf)
                  {:keys [data_creare cif id_descarcare]} mesaj
                  p (str download-dir "/" cif "/")
                  date-path (u/build-path data_creare)
                  download-to (str p date-path "/" id_descarcare ".zip")
                  updated-path (merge mesaj {:download-path download-to})]
              (conj acc updated-path)))
          []
          mesaje-cerute))

(defn routes
  [anaf-conf]
  [["/" (fn [req]
          (let [{:keys [headers]} req
                {:strs [hx-request]} headers
                content {:status 200 :body "Bine ai venit pe eFacturaMea" :headers {"content-type" "text/html"}}
                sidebar (ui/sidebar-select-company)]
            (if hx-request
              content
              (layout/main-layout (:body content) sidebar))))]
   ["/companii" (fn [req]
                  (let [{:keys [headers ds]} req
                        {:strs [hx-request]} headers
                        content (api/afisare-companii-inregistrate ds)
                        sidebar (ui/sidebar-select-company)]
                    (if hx-request
                      content
                      (layout/main-layout (:body content) sidebar))))]
   ["/inregistrare-noua-companie" (fn [_]
                                    (let [content (api/formular-inregistrare-companie)
                                          sidebar (ui/sidebar-select-company)]
                                      (layout/main-layout content sidebar)))]
   ["/inregistreaza-companie" (fn [req]
                                (let [{:keys [params ds]} req]
                                  (api/inregistrare-noua-companie ds params)))]
   ["/profil/:cif" (fn [req]
                     (let [{:keys [path-params]} req
                           {:keys [cif]} path-params
                           content (api/afisare-profil-companie req)
                           sidebar (ui/sidebar-company-data {:cif cif})]
                       (layout/main-layout content sidebar)))]
   ["/facturi/:cif"
    ["" {:get
         {:handler (fn [req]
                     (let [{:keys [path-params query-params ds conf uri headers]} req
                           {:strs [page per-page]} query-params
                           {:strs [hx-request]} headers
                           cif (:cif path-params)
                           opts {:cif cif :page page :per-page per-page :uri uri}
                           mesaje-cerute (db/fetch-mesaje ds cif page per-page)
                           mesaje (api/gather-invoices-data (add-path-for-download conf mesaje-cerute))
                           table-with-pagination (api/afisare-facturile-mele mesaje ds opts)
                           content (ui/facturi-descarcate table-with-pagination)
                           sidebar (ui/sidebar-company-data opts)]
                       (if (= hx-request "true")
                         content
                         (layout/main-layout (:body content) sidebar))))
          :middleware [add-pagination-params-middleware]}}]
    ["/facturile-mele" {:get
                        {:handler (fn [req]
                                    (let [{:keys [path-params query-params headers uri ds]} req
                                          {:keys [cif]} path-params
                                          {:strs [page per-page]} query-params
                                          {:strs [hx-request]} headers
                                          opts {:cif cif :page page :per-page per-page}
                                          mesaje-cerute (db/fetch-mesaje ds cif page per-page)
                                          mesaje (api/gather-invoices-data (add-path-for-download conf mesaje-cerute))
                                          content (api/afisare-facturile-mele mesaje ds opts)
                                          sidebar (ui/sidebar-company-data opts)]
                                      (if (= hx-request "true")
                                        content
                                        (layout/main-layout (:body content) sidebar))))
                         :middleware [add-pagination-params-middleware]}}]]
   ["/facturi-spv/:cif" (fn [req]
                          (handle-facturi ui/facturi-spv req))]
   ["/login" (fn [req]
               (layout/login req))]
   ["/login-anaf" (o2a/make-anaf-login-handler
                   (anaf-conf :client-id)
                   (anaf-conf :redirect-uri))]
   ["/api/v1/oauth/anaf-callback" (o2a/make-authorization-token-handler
                                   (anaf-conf :client-id)
                                   (anaf-conf :client-secret)
                                   (anaf-conf :redirect-uri))]
   ["/listare-sau-descarcare" (fn [req]
                                (let [{:keys [params ds conf]} req]
                                  (api/handle-list-or-download params ds conf)))]
   ["/transformare-xml-pdf" descarca-factura-pdf]
   ["/logs/:cif"
    ["" {:get
         {:handler (fn [req]
                     (let [{:keys [path-params query-params ds uri headers]} req
                           {:strs [page per-page]} query-params
                           {:strs [hx-request]} headers
                           cif (:cif path-params)
                           opts {:page page :per-page per-page :uri uri :cif cif}
                           content (ui/logs-api-calls ds opts)
                           sidebar (ui/sidebar-company-data opts)]
                       (if (= hx-request "true")
                         content
                         (layout/main-layout (:body content) sidebar))))
          :middleware [add-pagination-params-middleware]}}]]
   ["/descarcare-automata/:cif" (fn [req]
                                  (let [{:keys [path-params headers]} req
                                        {:strs [hx-request]} headers
                                        cif (:cif path-params)
                                        opts {:cif cif}
                                        sidebar (ui/sidebar-company-data opts)

                                        content (api/set-descarcare-automata cif)]
                                    #_(if (= hx-request "true")
                                        content
                                        (layout/main-layout content sidebar))
                                    (layout/main-layout content sidebar)))]
   ["/pornire-descarcare-automata" (fn [req]
                                     (let [{:keys [params ds conf]} req]
                                       (api/descarcare-automata-facturi params ds conf)))]

   ["/get-sda-form/:cif" (fn [req]
                           (let [{:keys [path-params ds]} req
                                 {:keys [cif]} path-params
                                 c-data (db/get-company-data ds cif)]
                             (api/sda-form c-data cif)))]
   ["/integrare/:cif" (fn [req]
                        (let [{:keys [path-params]} req
                              {:keys [cif]} path-params
                              content (api/afisare-integrare-efactura req)
                              sidebar (ui/sidebar-company-data {:cif cif})]
                          (layout/main-layout content sidebar)))]
   ["/autorizeaza-acces-fara-certificat/:cif" (fn [req]
                                                (let [{:keys [path-params]} req
                                                      {:keys [cif]} path-params
                                                      content (api/modala-link-autorizare cif)]
                                                  {:status 200
                                                   :body content
                                                   :headers {"content-type" "text/html"}}))]
   ["/descarcare-exportare/:cif" (fn [req]
                                   (let [{:keys [path-params headers]} req
                                         {:strs [hx-request]} headers
                                         {:keys [cif]} path-params
                                         content (api/afisare-descarcare-exportare cif)
                                         opts {:cif cif}
                                         sidebar (ui/sidebar-company-data opts)]
                                     (if (= hx-request "true")
                                       content
                                       (layout/main-layout (:body content) sidebar))))]
   ["/descarca-arhiva" (fn [req]
                         (let [{:keys [query-params ds]} req
                               {:strs [file_type_pdf file_type_zip]} query-params
                               content (de/descarca-lista-mesaje ds conf query-params)
                               {:keys [archive-content archive-name]} content
                               content-disposition (str "attachment; filename=" archive-name)]
                           (if (and (not file_type_pdf) (not file_type_zip))
                             {:status 200
                              :body "Trebuie sa selectezi cel puțin un fip de fișier"
                              :headers {"Content-Type" "text/html"}}
                             (if (nil? archive-content)
                               {:status 200
                                :body "În perioada selectata, nu au fost identificate facturi pentru descarcare"
                                :headers {"Content-Type" "text/html"}}
                               {:status 200
                                :body archive-content
                                :headers {"Content-Type" "application/zip, application/octet-stream"
                                          "Content-Disposition" content-disposition}}))))]
   ["/sumar-descarcare-arhiva" (fn [req]
                                 (let [{:keys [query-params ds conf]} req
                                       locale "ro-RO"]
                                   (de/sumar-descarcare-arhiva ds conf query-params locale)))]])


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
      (wrap-file (config/download-dir conf))
      (wrap-file (get-in conf [:server :public-path]))
      (wrap-webjars)
      (wrap-content-type)))

(defstate server
  :start (run-jetty (app conf) (:jetty conf))
  :stop (stop-server server))

(defn -main [& args]
  (println "Starting Application Server")
  (mount/start)
  (api/init-db ds)
  (api/pornire-serviciu-descarcare-automata ds conf))

(comment
  (-main)
  (mount/start)
  (mount/stop)
  (db/get-company-data ds "35586426")
  (f/update-automated-download-status ds {:id 1 :status ""})
  
  (jdbc/execute!
   ds
   ["SELECT timediff('2024-11-11 09:49:31', '2024-11-11 09:30:57')"])

  0
  )