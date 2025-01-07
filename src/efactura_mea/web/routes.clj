(ns efactura-mea.web.routes
  (:require
   [efactura-mea.web.middleware :refer [pagination-params-middleware]]
   [efactura-mea.web.anaf-integrare :as anaf]
   [efactura-mea.web.companii :as companii]
   [efactura-mea.web.descarca-arhiva :as da]
   [efactura-mea.web.descarca-exporta :as de]
   [efactura-mea.web.facturi :as facturi]
   [efactura-mea.web.home :as home]
   [efactura-mea.web.logs :as logs]
   [efactura-mea.web.api :as api]))

(defn routes
  [anaf-conf]
  [["/" home/handle-homepage]
   ["/companii" companii/routes]
   ["/facturi/:cif"
    ["" {:get
         {:handler facturi/handler-afisare-facturi-descarcate
          :middleware [pagination-params-middleware]}}]
    ["/facturile-mele" {:get
                        {:handler facturi/handler-lista-mesaje-spv
                         :middleware [pagination-params-middleware]}}]]
   ["/facturi-spv/:cif" facturi/handler-facturi-spv]
   ["/anaf" (anaf/routes anaf-conf)]
   ["/login" api/handler-login]
   ["/api/v1/oauth/anaf-callback" (anaf/make-authorization-token-handler
                                   (anaf-conf :client-id)
                                   (anaf-conf :client-secret)
                                   (anaf-conf :redirect-uri))]
   ["/listare-sau-descarcare" api/handle-list-or-download]
   ["/transformare-xml-pdf" api/handler-descarca-factura-pdf]
   ["/logs/:cif"
    ["" {:get
         {:handler logs/handler-logs
          :middleware [pagination-params-middleware]}}]]
   ["/descarcare-automata/:cif" api/handler-afisare-formular-descarcare-automata]
   ["/pornire-descarcare-automata" api/handler-descarcare-automata-facturi]
   ["/formular-descarcare-automata/:cif" api/handler-formular-descarcare-automata]
   ["/descarcare-exportare/:cif" de/handler-descarca-exporta]
   ["/descarca-arhiva" da/handler-descarca-arhiva]
   ["/sumar-descarcare-arhiva" da/handler-sumar-descarcare-arhiva]])