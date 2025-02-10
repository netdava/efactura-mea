(ns efactura-mea.web.layout
  (:require
   [efactura-mea.web.ui.componente :as ui]
   [hiccup.page :refer [html5]]))

(defn sidebar-select-company
  []
  [:div.p-3
   [:div#menu-wrapper.menu-wrapper
    [:aside.menu {:_ "on click take .is-active from .menu-item for the event's target"}
     [:ul.menu-list
      [:p.menu-label "Acasa"]
      [:li [:a.menu-item
            {:href "/"}
            "Companii"]]]]]])

(def tabulator-cdn (list
                [:link {:href "https://unpkg.com/tabulator-tables/dist/css/tabulator_bulma.min.css"
                        :rel "stylesheet"}]
                [:script {:type "text/javascript"
                          :src "https://unpkg.com/tabulator-tables/dist/js/tabulator.min.js"}]))

(defn main-layout
  ([content]
   (main-layout content sidebar-select-company "User Admin"))
  ([content sidebar]
   (main-layout content sidebar "User Admin"))
  ([content sidebar header]
   (html5
    {:lang "en"}
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
     tabulator-cdn
     [:link {:rel "stylesheet"
             :href "https://cdn.jsdelivr.net/npm/bulma@1.0.0/css/bulma.min.css"}]
     [:link {:rel "stylesheet"
             :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}]
     [:link {:rel "stylesheet"
             :href "/css/style.css"}]
     [:link {:rel "stylesheet"
             :href "/css/bulma-switch.min.css"}]
     [:link {:rel "stylesheet"
             :href "/assets/vanillajs-datepicker/dist/css/datepicker-bulma.min.css"}]
     [:script {:type "text/javascript"
               :src "/assets/htmx.org/dist/htmx.min.js"}]
     [:script {:type "text/javascript"
               :src "/assets/hyperscript.org/dist/_hyperscript.min.js"}]
     [:script {:type "module"
               :src "/assets/vanillajs-datepicker/js/Datepicker.js"}]
     [:script {:type "module"
               :src "/js/vanillajs-datepicker-constructor.js"}]
     [:body
      (ui/navbar header)
      [:section.container.is-fluid.mt-5
       [:div.columns
        [:div.column.is-one-fifth
         sidebar]
        [:div#main-content.column.main-content
         content]]]]])))

