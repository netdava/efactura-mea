(ns efactura-mea.layout
  (:require [hiccup.page :refer [html5]]
            [efactura-mea.ui.componente :as ui]
            [reitit.core :as r]))

(defn sidebar-select-company []
  [:div.p-3
   [:div#menu-wrapper.menu-wrapper
    [:aside.menu {:_ "on click take .is-active from .menu-item for the event's target"}
     [:ul.menu-list
      [:li [:a.menu-item
            {:hx-get "/"
             :hx-target "#main-content"
             :hx-change "innerHTML"
             :hx-push-url "true"}
            "Acasă"]]]
     [:p.menu-label "Portofoliu"]
     [:ul.menu-list
      [:li [:a.menu-item
            {:hx-get "/companii"
             :hx-target "#main-content"
             :hx-change "innerHTML"
             :hx-push-url "true"}
            "Companii"]]]]]])

(defn sidebar-company-data [opts]
  ;; TODO de pus conditia ca meniul sa fie disponibil doar daca compania a fost inregistrata corect cu token si tot ce trebuie
  (let [{:keys [cif page per-page router]} opts
        qp (when (and page per-page)
             (str "?page=" page "&per-page=" per-page))
        link-facturi-descarcate (str "/facturi/" cif)
        link-facturi-spv (str "/facturi-spv/" cif)
        link-logs (str "/logs/" cif qp)
        link-descarcare-automata (str "/descarcare-automata/" cif)
        link-profil (str "/companii/profil/" cif)
        link-integrare (-> router
                           (r/match-by-name :efactura-mea.web.anaf-integrare/integrare-cif
                                            {:cif cif})
                           :path)
        link-descarcare-exportare (str "/descarcare-exportare/" cif)]
    [:div.p-3
     [:div.menu-wrapper
      [:aside.menu
       [:ul.menu-list
        [:li [:a {:href "/"} "Acasă"]]
        [:li [:a {:href link-profil} "Profil"]]]
       [:p.menu-label "Facturi"]
       [:ul.menu-list
        [:li [:a {:href link-facturi-descarcate} "Descărcate"]]
        [:li [:a {:href link-facturi-spv} "Spațiul Public Virtual"]]
        [:li [:a {:hx-get link-logs
                  :hx-target "#main-content"
                  :hx-push-url "true"} "Jurnal actiuni"]]]
       [:p.menu-label "Administrare"]
       [:ul.menu-list
        [:li [:a {:href link-descarcare-automata} "Descărcare automată facturi"]]
        [:li [:a {:href link-integrare} "Integrare automată"]]
        [:li [:a {:hx-get link-descarcare-exportare
                  :hx-target "#main-content"
                  :hx-push-url "true"} "Descarcă/Exportă"]]]]]]))

(defn main-layout
  ([content]
   (main-layout content sidebar-select-company "User Admin"))
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
                        [:section.container.mt-5
                         [:div.columns
                          [:div.column.is-one-fifth
                           sidebar]
                          [:div#main-content.column.main-content
                           content]]]]]))}))

