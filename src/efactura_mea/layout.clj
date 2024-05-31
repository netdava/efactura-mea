(ns efactura-mea.layout
  (:require [hiccup.page :refer [html5]]
            [hiccup2.core :as h]
            [efactura-mea.ui.componente :as ui]))

(defn sidebar-menu []
  [:aside.menu.column.is-2.main.hero.is-fullheight
   [:ul.menu-list
    [:li [:a {:href "/"} "Facturi"]]
    [:li [:a {:href "/ca"} "Certificate Authority"]]
    [:li [:a {:href "/certificates"} "Hosts & Certificates"]]
    [:li [:a {:href "/network"} "Network"]]]])

(defn header []
  [:header.header
   [:div.header-content
    [:div.user "Adminn"]]])

(defn footer []
  [:footer.footer
   [:span "snm by netdava - 2024"]])

(defn main [content]
  [:main.main
   [:div.page-content
    [:div#nebula-cert-main-container.card
          ;;    {:class "card"
          ;;     :hx-get "/nebula-cert"
          ;;     :hx-trigger "load delay:100ms"
          ;;     :hx-target "#nebula-cert-main-container"
          ;;     :hx-swap  "innerHTML"}
     content]
    #_[:div#upload-form {:hx-get "/upload-form"
                         :hx-target "#upload-form"
                         :hx-trigger "load"}]
    #_[:div#upload-file-form {:hx-get "/upload-file"
                              :hx-target "#upload-file-form"
                              :hx-trigger "load"}]]])

(defn main-layout
  ([content]
   (main-layout content (ui/sidebar) "User Admin"))
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
                          [:div.column.main-content
                           content]]]]]))}))