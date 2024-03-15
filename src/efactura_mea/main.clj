(ns efactura-mea.main
  (:require [babashka.http-client :as http]
            [clj-commons.byte-streams :as bs]
            [cprop.core :refer [load-config]]
            [hiccup2.core :as h]
            [jsonista.core :as j]
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
            [efactura-mea.api :as api]
            [efactura-mea.next-jdbc-adapter :as adapter]
            [efactura-mea.oauth2-anaf :as o2a])
  (:gen-class))

(mu/on-upndown :info mu/log :before)

(defstate conf
  :start (load-config))

(defstate ds :start
  (let [db-spec (:db-spec conf)]
    (adapter/set-next-jdbc-adapter)
    (jdbc/get-datasource db-spec)))

(def object-mapper (j/object-mapper {:pretty true}))

(def m (m/create (assoc m/default-options
                        :return :input-stream
                        :mapper object-mapper)))
(defn html-handler
  [_]
  {:status 200, :body "ok"})

(defn req->str
  [req]
  (let [body (:body req)
        bis (m/encode m
                      "application/json"
                      (-> req
                          (assoc :body (bs/to-string body))
                          (dissoc :reitit.core/router
                                  :reitit.core/match)))]
    (bs/to-string bis)))

(defn res->str
  [res]
  (let [body (:body res)
        bis (m/encode m
                      "application/json"
                      (-> res
                          (assoc :body (bs/to-string body))
                          (dissoc :body
                                  :reitit.core/router
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
  [["/" html-handler]
   ["/login-anaf" (o2a/make-anaf-login-handler
                   (anaf-conf :client-id)
                   (anaf-conf :client-secret))]
   ["/xui/echo" {:get echo-request}]
   ["/api/v1/oauth/anaf-callback" (o2a/make-authorization-token-handler
                                   (anaf-conf :client-id)
                                   (anaf-conf :client-secret)
                                   (anaf-conf :redirect-uri))]])

(defn handler
  [conf]
  (reitit/ring-handler
   (reitit/router (routes (get-in conf [:anaf])))))

(defn app
  [conf]
  (-> (handler conf)
      (middleware/wrap-format)
      (rmd/wrap-defaults rmd/site-defaults)
      (wrap-file (get-in conf [:server :public-path]))
      (wrap-webjars)))

(defstate server
  :start (run-jetty (app conf) (:jetty conf))
  :stop (stop-server server))

(defn -main [& args]
  (mount/start)
  (api/create-sql-tables ds)
  (api/obtine-lista-facturi {:endpoint :test} ds))

(comment
  (-main)
  (mount/start)
  (mount/stop)

  (println conf)


  (bs/to-string (m/encode m "application/json" {"a" 123}))

  (http/get "https://www.ieugen.ro/")


  0)
