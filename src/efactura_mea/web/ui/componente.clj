(ns efactura-mea.web.ui.componente
  (:require
   [efactura-mea.util :as u]
   [efactura-mea.web.utils :as wu]
   [hiccup2.core :as h]))

(defn hiccup-bold-span
  [text]
  (h/html [:span.has-text-weight-bold text]))

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
     [:div.navbar-start]
     [:div.navbar-end
      [:div.navbar-item.has-dropdown.is-hoverable
       [:a.navbar-link {:href "#"} user-name]
       [:div.navbar-dropdown.is-boxed
        [:a.navbar-item {:href "#"} "Profile"]
        [:a.navbar-item {:href "#"} "Settings"]
        [:hr.navbar-divider]
        [:a.navbar-item.is-selected {:href "#"} "Logout"]]]]]]])

(defn sidebar-company-data [opts]
  ;; TODO de pus conditia ca meniul sa fie disponibil doar daca compania a 
  ;; fost inregistrata corect cu token si tot ce trebuie
  (let [{:keys [cif page per-page router]} opts
        query-params {:page page
                      :per-page per-page}
        path-params {:cif cif}
        link-facturi-descarcate (wu/route-name->url
                                 router :efactura-mea.web.companii/facturi-companie path-params)
        link-facturi-spv (wu/route-name->url
                          router :efactura-mea.web.companii/facturi-spv path-params)
        link-logs (wu/route-name->url
                   router :efactura-mea.web.companii/jurnal-spv path-params query-params)
        link-profil (wu/route-name->url
                     router :efactura-mea.web.companii/profil path-params)
        link-integrare (wu/route-name->url
                        router :efactura-mea.web.anaf-integrare/integrare path-params)
        link-descarcare-exportare (wu/route-name->url
                                   router :efactura-mea.web.companii/export-facturi path-params)]
    [:div.p-3
     [:div.menu-wrapper
      [:aside.menu
       [:ul.menu-list
        [:li [:a {:href "/"} "Acasă"]]]
       [:p.menu-label "Facturi"]
       [:ul.menu-list
        [:li [:a {:href link-facturi-descarcate} "Descărcate"]]
        [:li [:a {:href link-facturi-spv} "Spațiul Public Virtual"]]
        [:li [:a {:hx-get link-logs
                  :hx-target "#main-content"} "Jurnal actiuni"]]
        [:li [:a {:hx-get link-descarcare-exportare
                  :hx-target "#main-content"
                  :hx-push-url "true"} "Descarcă/Exportă"]]]
       [:p.menu-label "Administrare"]
       [:ul.menu-list
        [:li [:a {:href link-profil} "Profil"]]
        [:li [:a {:href link-integrare} "Integrare eFactura ANAF"]]]]]]))

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

(defn title [title-text & args]
  (h/html
   [:div
    [:p.title.is-4 (str title-text (apply str args))]
    [:hr.title-hr]]))

(defn facturi-spv [opts _]
  (let [{:keys [cif]} opts
        days (range 1 60)
        days-select-vals (for [n days]
                           [:option {:value n} n])]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (str (h/html
                 [:div#main-container.block
                  (title "Aici poți vizualiza și descărca facturile din SPV:")
                  [:form.block {:hx-get "/listare-sau-descarcare"
                                :hx-target "#facturi-anaf"}
                   [:div.field
                    [:label.label "CIF:"]
                    [:input.input {:readonly "readonly"
                                   :type "text"
                                   :id "cif-number"
                                   :list "cif"
                                   :name "cif"
                                   :value cif
                                   :placeholder cif}]]
                   [:div.field
                    [:label.label "Număr de zile pentru vizualizare/descărcare facturi anaf:"]
                    [:div.select [:select {:id "zile" :name "zile"}
                                  days-select-vals]]]
                   [:div.buttons
                    [:button.button.is-small.is-link {:type "submit"
                                                      :name "action"
                                                      :value "listare"} "vezi facturi"]
                    [:button.button.is-small.is-link {:type "submit" :name "action" :value "descarcare"} "descarca facturi"]]]
                  [:div#facturi-anaf]]))}))

(defn table-header-facturi-anaf []
  (h/html
   [:tr
    [:th]
    [:th "dată răspuns"]
    [:th "tip factură"]
    [:th "id solicitare"]
    [:th "detalii"]
    [:th "id factură"]]))

(defn row-factura-anaf
  [data ora tip-factura id_solicitare detalii id downloaded?-mark]
  (h/html
   [:tr
    [:td downloaded?-mark]
    [:td.is-size-7 data [:br] ora]
    [:td.is-size-7 tip-factura]
    [:td.is-size-7 id_solicitare]
    [:td.is-size-7 detalii]
    [:td.is-size-7 id]]))

(defn table-header-facturi-descarcate []
  (h/html
   [:tr
    [:th "id descărcare"]
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
  [{:keys [data_creare client id_descarcare tip furnizor valuta total data_scadenta data_emitere serie_numar cif]}]
  (let [dc (u/parse-date data_creare)
        parsed_date (str (:data_c dc) "-" (:ora_c dc))
        path (u/build-path data_creare)
        zip-file-name (str id_descarcare ".zip")
        final-path (str "/" cif "/" path "/" zip-file-name)
        pdf-file-name (str id_descarcare ".pdf")
        type (tag-tip-factura tip)
        tag-opts (update {:class "tag is-normal "} :class str type)
        link-opts {:href final-path :target "_blank"}
        pdf-download-query-params (str "?id_descarcare=" id_descarcare)
        pdf-download-url (str "/transformare-xml-pdf" pdf-download-query-params)]
    (h/html
     [:tr
      [:td.is-size-7 id_descarcare]
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
           {:href pdf-download-url
            :target "_blank"} pdf-file-name]]]]]])))

(defn tabel-facturi-descarcate
  [rows]
  [:table.table.is-hoverable
   (table-header-facturi-descarcate)
   (for [r rows] r)])

(defn validation-message
  [err-days err-cif]
  [:ul.err-msg
   (when err-days [:li err-days])
   (when err-cif [:li err-cif])])

(defn details-table
  "Primeste un map
   Genereaza un tabel pe baza tuturor perechilor k-v"
  [details-map]
  [[:div.column
    [:table.table.is-fullwidth
     [:tbody
      (for [[k v] details-map]
        [:tr
         [:th k]
         [:td.has-text-right v]])]]]
   [:div.column
    [:table.table.is-fullwidth
     [:tbody
      (for [[k v] details-map]
        [:tr
         [:th k]
         [:td.has-text-right v]])]]]])

(defn lista-mesaje
  [r]
  [:div.content.block
   [:h2 "Facturi disponibile pentru descărcat:"]
   [:table.table r]])
