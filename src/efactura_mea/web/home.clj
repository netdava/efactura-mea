(ns efactura-mea.web.home
  (:require
   [clojure.tools.logging :as log]
   [efactura-mea.web.companii :as companii]
   [efactura-mea.web.layout :as layout]
   [ring.util.response :as rur]))

(defn handle-homepage
  [req]
  (let [{:keys [headers ds :reitit.core/router]} req
        {:strs [hx-request]} headers
        content (companii/afisare-companii-inregistrate router ds)
        sidebar (layout/sidebar-select-company)]
    ;; (log/info "Home page 3")
    (if hx-request
      (-> (rur/response content)
          (rur/content-type "text/html"))
      (layout/main-layout content sidebar))))