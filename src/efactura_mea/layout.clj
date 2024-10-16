(ns efactura-mea.layout
  (:require [hiccup.page :refer [html5]]
            [hiccup2.core :as h]
            [efactura-mea.ui.componente :as ui]))

(defn main-layout
  ([content]
   (main-layout content (ui/sidebar-company-data) "User Admin"))
  ([content sidebar]
   (main-layout content sidebar "User Admin"))
  ([content sidebar header]
   {:status 200
    :headers {"content-type" "text/html"}
    :body (str (html5 {:lang "en"}
                      [:head
                       [:meta {:charset "utf-8"}]
                       [:meta {:name "viewport"
                               :content "width=device-width"
                               :initial-scale "1"}]
                       #_[:link {:rel "manifest"
                               :href "manifest.json"}]
                       #_[:link {:rel "icon"
                               :type "image/x-icon"
                               :href "/images/favicon-32x32.png"}]
                       [:title "eFacturaMea"]
                       [:link {:rel "stylesheet"
                               :href "https://cdn.jsdelivr.net/npm/bulma@1.0.0/css/bulma.min.css"}]
                       [:link {:rel "stylesheet"
                               :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}]
                       [:link {:rel "stylesheet"
                               :href "/css/style.css"}]
                       [:link {:rel "stylesheet"
                               :href "/css/bulma-switch.min.css"}]
                       [:script {:type "text/javascript"
                                 :src "/assets/htmx.org/dist/htmx.min.js"}]
                       #_[:script {:type "text/javascript"
                                 :src "/js/dropdown.js"}]


                       [:body
                        (ui/navbar header)
                        [:section.container.mt-5
                         [:div.columns
                          [:div.column.is-one-fifth
                           sidebar]
                          [:div#main-content.column.main-content
                           content]]]]]))}))

