(ns efactura-mea.ui.componente
  (:require [hiccup2.core :as h]
            [efactura-mea.db.db-ops :as db]
            [efactura-mea.util :as u]
            [babashka.http-client :as http]))

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
      [:div.navbar-item.has-dropdown.is-hoverable
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
                              :hx-trigger "load"}]]))

(defn facturi-spv []
  (let [days (range 1 61)
        days-select-vals (for [n days]
                           [:option {:value n} n])]
    (h/html
     [:div#main-container.block
      (title "Aici poți vizualiza și descărca facturile din SPV:")
      [:form.block {:hx-get "/listare-sau-descarcare"
                    :hx-target "#facturi-anaf"}
       [:div.field
        [:label.label "CIF:"]
        [:input.input {:type "text"
                 :id "cif-number"
                 :list "cif"
                 :name "cif"}]
        [:datalist {:id "cif"} [:option "35586426"]]]
       [:div.field
        [:label.label "Număr de zile pentru vizualizare/descărcare facturi anaf:"]
        [:div.select [:select {:id "zile" :name "zile"}
                      days-select-vals]]]
       [:div.buttons
        [:button.button.is-small.is-link {:type "submit"
                                          :name "action"
                                          :value "listare"} "vezi facturi"]
        [:button.button.is-small.is-link {:type "submit" :name "action" :value "descarcare"} "descarca facturi"]]]
      [:div#facturi-anaf]])))

(defn table-header-facturi-anaf []
  (h/html
   [:tr
    [:th "dată răspuns"]
    [:th "tip factură"]
    [:th "id solicitare"]
    [:th "detalii"]
    [:th "id factură"]]))

(defn row-factura-anaf 
  [data ora tip-factura id_solicitare detalii id]
  (h/html
   [:tr
    [:td.is-size-7 data [:br] ora]
    [:td.is-size-7 tip-factura]
    [:td.is-size-7 id_solicitare]
    [:td.is-size-7 detalii]
    [:td.is-size-7 id]]))

(defn table-header-facturi-descarcate []
  (h/html
   [:tr
    [:th "serie/număr"]
    [:th "data urcare SPV"]
    [:th "data emiterii"]
    [:th "data scadenței"]
    [:th "furnizor"]
    [:th "client"]
    [:th "valoare"]
    [:th "moneda"]
    [:th "tip"]
    [:th "download"]]))

(defn tag-tip-factura [tip]
  (case tip
    "primita" "is-info"
    "trimisa" "is-success"
    "eroare" "is-danger"
    "is-warning"))

(defn row-factura-descarcata-detalii 
  [ds {:keys [data_creare client id_descarcare id_solicitare tip furnizor valuta total data_scadenta data_emitere serie_numar href cif]}]
  (let [dc (u/parse-date data_creare)
        parsed_date (str (:data_c dc) "-" (:ora_c dc))
        path (u/build-path data_creare)
        zip-file-name (str id_descarcare ".zip")
        final-path (str "date/" cif "/" path "/" zip-file-name)
        app-dir (System/getProperty "user.dir")
        full-path (str app-dir "/" href)
        xml-name (str id_solicitare ".xml")
        xml-content (u/read-file-from-zip full-path xml-name)
        type (tag-tip-factura tip)
        tag-opts (update {:class "tag is-normal "} :class str type)
        link-opts {:href final-path :target "_blank"}]
    (h/html
     [:tr
      [:td.is-size-7 serie_numar]
      [:td.is-size-7 parsed_date]
      [:td.is-size-7 data_emitere]
      [:td.is-size-7 data_scadenta]
      [:td.is-size-7 furnizor]
      [:td.is-size-7 client]
      [:td.is-size-7 total]
      [:td.is-size-7 valuta]
      [:td.is-size-7 [:span tag-opts tip]]
      [:td.is-size-7.has-text-centered
       [:div.dropdown.is-hoverable
        [:div.dropdown-trigger
         [:button.button.is-small {:aria-haspopup "true" :aria-controls "dropdown-menu3"}
          [:i {:class "fa fa-ellipsis-h "
               :aria-hidden true}]]]
        [:div.dropdown-menu {:id "dropdown-menu3" :role "menu"}
         [:div.dropdown-content
          [:a.dropdown-item link-opts zip-file-name]
          [:a.dropdown-item
           [:form {:hx-get "/transformare-xml-pdf"
                   :hx-swap "none"}
            [:input {:type "hidden" :name "id_descarcare" :value id_descarcare}]
            [:button.button {:type "submit"} "Pdf"]]]]]]]])))



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
               [:div.content.block
                [:h2 "Facturi disponibile pentru descărcat:"]
                [:table
                 r]]))
   :headers {"content-type" "text/html"}})
