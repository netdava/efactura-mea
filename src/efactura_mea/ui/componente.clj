(ns efactura-mea.ui.componente
  (:require [hiccup2.core :as h]))

(defn navbar [user-name]
  [:nav.navbar.is-white.top-nav
   [:div.container
    [:div.navbar-brand
     [:a.navbar-item.has-text-weight-bold.has-text-black {:href "/"} "eFacturaMea"]
     [:div.navbar-burger
      [:span]
      [:span]
      [:span]
      [:span]]]
    [:div.navbar-menu
     [:div.navbar-start
      #_[:a.navbar-item {:href "/"} "Home"]]
     [:div.navbar-end
      [:div.navbar-item.has-dropdown
       [:a.navbar-link {:href "#"} user-name]
       [:div.navbar-dropdown.is-boxed
        [:a.navbar-item {:href "#"} "Profile"]
        [:a.navbar-item {:href "#"} "Settings"]
        [:hr.navbar-divider]
        [:a.navbar-item.is-selected {:href "#"} "Logout"]]]]]]])



(defn sidebar []
  [:div.p-3
   [:div.menu-wrapper
    [:aside.menu
     [:p.menu-label "Facturi"]
     [:ul.menu-list
      [:li [:a {:href "/"} "Descărcate"]]
      [:li [:a {:href "/facturi-spv"} "Spațiul Public Virtual"]]]]]])

(defn title [title-text & args]
  (h/html
   [:div
    [:p.title.is-4 (str title-text (apply str args))]
    [:hr.title-hr]]))

(defn facturi-descarcate []
  (h/html
   [:div#main-container.block
    (title "Facturi descărcate local")
    [:div#facturi-descarcate {:hx-get "/facturile-mele"
                              :hx-target "#facturi-descarcate"
                              :hx-trigger "load"}]
    ]))

(defn facturi-spv []
  (let [days (range 1 61)
        days-select-vals (for [n days]
                           [:option {:value n} n])]
    (h/html
     [:div#main-container.block
      (title "Aici poți vizualiza și descărca facturile din SPV:")
      [:form
       [:div.field
        [:label.label "CIF:"]
        [:input.input {:type "text"
                 :id "cif-number"
                 :list "cif"
                 :name "cif"}]
        [:datalist {:id "cif"} [:option "35586426"]]]
       [:div.field
        [:label.label "Număr de zile pentru vizualizare/descărcare facturi anaf:"]
        [:div.select [:select
                      days-select-vals]]]
       [:div.buttons
        [:button.button.is-small.is-link {:type "submit" :name "action" :value "listare"} "vezi facturi"]
        [:button.button.is-small.is-link {:type "submit" :name "action" :value "descarcare"} "descarca facturi"]]]])))

(defn table-header-facturi-anaf []
  (h/html
   [:tr
    [:th "dată răspuns"]
    [:th "tip"]
    [:th "număr înregistrare"]
    [:th "detalii"]
    [:th "id descărcare"]]))

(defn table-header-facturi-descarcate []
  (h/html
   [:tr
    [:th "nume fișier"]
    [:th "data creare"]
    [:th "detalii"]
    [:th "tip"]
    [:th "număr înregistrare"]]))

(defn row-factura-anaf [data ora tip-factura id_solicitare detalii id]
  (h/html
   [:tr
    [:td.is-size-7 data [:br] ora]
    [:td.is-size-7 tip-factura]
    [:td.is-size-7 id_solicitare]
    [:td.is-size-7 detalii]
    [:td.is-size-7 id]]))

(defn row-factura-descarcata [href name creation-date detalii tip id_solicitare]
  (h/html
   [:tr
    [:td.is-size-7 [:a {:href href :target "_blank"} name]]
    [:td.is-size-7 creation-date]
    [:td.is-size-7 detalii]
    [:td.is-size-7 tip]
    [:td.is-size-7 id_solicitare]]))

(defn tabel-facturi-descarcate [rows]
  (h/html
   [:table.table.is-hoverable.is-fullwidth
    (table-header-facturi-descarcate)
    (for [r rows]
      r)]))

(defn validation-message [err-days err-cif]
  (h/html 
   [:ul.err-msg
    (when err-days [:li err-days])
    (when err-cif [:li err-cif])]))

(defn lista-mesaje [r]
  {:status 200
   :body (str (h/html
               [:div.facturi
                [:h4 "Facturi disponibile pentru descărcat:"]
                [:div {:style {"width" "1000px"
                               "word-wrap" "break-word"}}
                 [:table
                  r]]]))
   :headers {"content-type" "text/html"}})
