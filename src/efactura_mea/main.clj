(ns efactura-mea.main
  (:require [clj-commons.byte-streams :as bs]
            [clojure.java.io :as io]
            [cprop.core :refer [load-config]]
            [babashka.http-client :as http]
            [efactura-mea.db.db-ops :as db]
            [efactura-mea.db.next-jdbc-adapter :as adapter]
            [efactura-mea.web.api :as api]
            [efactura-mea.web.json :as wj]
            [efactura-mea.web.oauth2-anaf :as o2a]
            [hiccup2.core :as h]
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
            [efactura-mea.layout :as layout]
            [efactura-mea.ui.componente :as ui]
            [efactura-mea.db.facturi :as f])
  (:gen-class))

(mu/on-upndown :info mu/log :before)

(defstate conf
  :start (load-config))

(defstate ds :start
  (let [db-spec (:db-spec conf)]
    (adapter/set-next-jdbc-adapter)
    (jdbc/get-datasource db-spec)))

(defn html-handler
  [_]
  {:status 200, :body "ok"})

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

(defn echo-request
  [req]
  ;; (tap> req)
  ;; (def reqqq res)
  {:status 200
   :body (str (h/html
               [:div.request
                [:h2 "Request"]
                [:div {:style {"width" "600px"
                               "word-wrap" "break-word"}}
                 (req->str req)]]))
   :headers {"content-type" "text/html"}})

(defn routes
  [anaf-conf]
  [["/" (fn [req] (layout/main-layout "Hello, Admin!"))]
   ["/facturi/:cif" (fn [req] (layout/main-layout (ui/facturi-descarcate req)))]
   ["/facturi-spv/:cif" (fn [req] (layout/main-layout (ui/facturi-spv req)))]
   ["/login-anaf" (o2a/make-anaf-login-handler
                   (anaf-conf :client-id)
                   (anaf-conf :redirect-uri))]
   ["/xui/echo" {:get echo-request}]
   ["/api/v1/oauth/anaf-callback" (o2a/make-authorization-token-handler
                                   (anaf-conf :client-id)
                                   (anaf-conf :client-secret)
                                   (anaf-conf :redirect-uri))]
   ["/listare-sau-descarcare" (fn [request]
                               (api/efactura-action-handler request conf ds))]
   
   ["/facturile-mele/:cif" (fn [request] (api/afisare-facturile-mele request ds conf))]
   ["/transformare-xml-pdf" (fn [req] (api/transformare-xml-to-pdf req ds conf))]
   ["/logs/:cif" (fn [req] (layout/main-layout (ui/logs-api-calls req ds)))]])

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
  :start (run-jetty (app conf) (:jetty conf))
  :stop (stop-server server))

(defn -main [& args]
  (mount/start)
  (db/create-sql-tables ds))

(comment
  (-main)
  (mount/start)
  (mount/stop)

  (bs/to-string (m/encode wj/m "application/json" {"a" 123}))


  (defn save-pdf [pdf-content]
    (io/copy pdf-content (io/file "my.pdf")))

  (defn upload-factura [ds cif]
    (let [a-token (db/fetch-access-token ds cif)
          f (slurp "facturi-anaf/pt-upload/3513634141/4313475697.xml")
          url "https://api.anaf.ro/prod/FCTEL/rest/transformare/FACT1"
          r (http/post url {:headers {"Authorization" (str "Bearer " a-token)
                                      "Content-Type" "text/plain"}
                            :body f
                            :as :stream})]
      (:body r)))

  (defn fetch-and-save-pdf [ds cif]
    (let [pdf-content (upload-factura ds cif)]
      (save-pdf pdf-content)))


  (upload-factura ds "35586426")
  (fetch-and-save-pdf ds "35586426")
  0
  )
