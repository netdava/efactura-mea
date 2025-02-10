(ns efactura-mea.web.ui.componente
  (:require
   [efactura-mea.web.utils :as wu]))

(defn hiccup-bold-span
  [text]
  [:span.has-text-weight-bold text])

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
  (let [{:keys [cif router]} opts
        path-params {:cif cif}
        link-facturi-descarcate (wu/route-name->url
                                 router :efactura-mea.web.companii/facturi-companie path-params)
        link-facturi-spv (wu/route-name->url
                          router :efactura-mea.web.companii/facturi-spv path-params)
        link-logs (wu/route-name->url
                   router :efactura-mea.web.companii/jurnal-spv path-params)
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
        [:li [:a {:href link-logs} "Jurnal actiuni"]]
        [:li [:a {::href link-descarcare-exportare} "Descarcă/Exportă"]]]
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

(defn title
  [title-text & args]
  [:div
   [:p.title.is-4 (str title-text (apply str args))]
   [:hr.title-hr]])

(defn facturi-spv [opts _]
  (let [{:keys [cif router]} opts
        days (range 1 60)
        days-select-vals (for [n days]
                           [:option {:value n} n])]
    [:div#main-container.block
     (title "Aici poți vizualiza și descărca facturile din SPV:")
     [:form.block {:hx-get "/api/alfa/listare-sau-descarcare"
                   :hx-target "#facturi-anaf-table-wrapper"}
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
     [:div#facturi-anaf-table-wrapper]]))

(defn tag-tip-factura [tip]
  (case tip
    "primita" "is-info"
    "trimisa" "is-success"
    "eroare" "is-danger"
    "is-warning"))

(defn validation-message
  [err-days err-cif]
  [:ul.err-msg
   (when err-days [:li err-days])
   (when err-cif [:li err-cif])])

(defn details-table
  "Primeste un map
   Genereaza un tabel pe baza tuturor perechilor k-v"
  [details-map]
  [:div.column
   [:table.table.is-fullwidth
    [:tbody
     (for [[k v] details-map]
       [:tr
        [:th k]
        [:td.has-text-right (or v "N/A")]])]]]
  [:div.column
   [:table.table.is-fullwidth
    [:tbody
     (for [[k v] details-map]
       [:tr
        [:th k]
        [:td.has-text-right (or v "N/A")]])]]])