(ns efactura-mea.web.home
  (:require
   [clojure.tools.logging :as log]
   [efactura-mea.web.companii :as companii]
   [efactura-mea.web.layout :as layout]
   [ring.util.response :as rur]
   [hiccup2.core :as h]))

(defn handle-homepage
  [req]
  (let [{:keys [headers ds :reitit.core/router]} req
        {:strs [hx-request]} headers
        content (companii/afisare-companii-inregistrate router ds)
        sidebar (layout/sidebar-select-company)
        body (if hx-request
               (str (h/html content))
               (layout/main-layout content sidebar))]
    ;; (log/info "Home page 3")
    (-> (rur/response body)
        (rur/content-type "text/html"))))