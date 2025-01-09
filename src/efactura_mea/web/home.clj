(ns efactura-mea.web.home
  (:require 
   [efactura-mea.web.layout :as layout]))

(defn handle-homepage
  [req]
  (let [{:keys [headers]} req
        {:strs [hx-request]} headers
        content {:status 200
                 :body "Bine ai venit pe eFacturaMea"
                 :headers {"content-type" "text/html"}}
        sidebar (layout/sidebar-select-company)]
    (if hx-request
      content
      (layout/main-layout (:body content) sidebar))))