(ns efactura-mea.web.routes
  (:require
   [efactura-mea.web.anaf-integrare :as anaf]
   [efactura-mea.web.api :as api]
   [efactura-mea.web.companii :as companii]
   [efactura-mea.web.descarca-arhiva :as da]
   [efactura-mea.web.home :as home]))

(defn routes
  []
  [["/" #'home/handle-homepage]
   ["/companii" (companii/routes)]
   ["/anaf" (anaf/routes)]
   ["/api/v1/oauth/anaf-callback" #'anaf/make-authorization-token-handler]
   ["/pornire-descarcare-automata" #'api/handler-descarcare-automata-facturi]
   ["/listare-sau-descarcare" #'api/handle-list-or-download]
   ["/transformare-xml-pdf" #'api/handler-descarca-factura-pdf]
   ["/descarca-arhiva" #'da/handler-descarca-arhiva]
   ["/sumar-descarcare-arhiva" #'da/handler-sumar-descarcare-arhiva]])