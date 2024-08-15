(ns efactura-mea.ui.componente
  (:require [hiccup2.core :as h]
            [efactura-mea.db.db-ops :as db]
            [efactura-mea.util :as u]
            [java-time :as jt]))

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
      [:li [:a {:href "/facturi/35586426"} "Descărcate"]]
      [:li [:a {:href "/facturi-spv/35586426"} "Spațiul Public Virtual"]]
      [:li [:a {:href "/logs/35586426"} "Jurnal actiuni"]]]
     [:p.menu-label "Administrare"]
     [:ul.menu-list
      [:li [:a {:href "/descarcare-automata/35586426"} "Descărcare automată facturi"]]]]]])

(defn title [title-text & args]
  (h/html
   [:div
    [:p.title.is-4 (str title-text (apply str args))]
    [:hr.title-hr]]))

(defn facturi-descarcate [{:keys [path-params]}]
  (let [cif (:cif path-params)
        get-url (str "/facturile-mele/" cif)]
    (h/html
     [:div#main-container.block
      (title "Facturi descărcate local")
      [:div#facturi-descarcate {:hx-get get-url
                                :hx-target "#facturi-descarcate"
                                :hx-trigger "load"}]])))

(defn facturi-spv [{:keys [path-params]}]
  (let [cif (:cif path-params)
        days (range 1 60)
        days-select-vals (for [n days]
                           [:option {:value n} n])]
    (h/html
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

(defn table-header-api-calls []
  [:tr
   [:th]
   [:th "data"]
   [:th "url"]
   [:th "tip"]
   [:th "status"]])

(defn row-log-api-call
  [{:keys [id data_apelare url tip status_code]}]
  (let [zoned-time (jt/zoned-date-time data_apelare)
        f-time (jt/format "H:mm - MMMM dd, yyyy" zoned-time)]
    (h/html
     [:tr
      [:td.is-size-7 id]
      [:td.is-size-7 f-time]
      [:td.is-size-7 url]
      [:td.is-size-7 tip]
      [:td.is-size-7 status_code]])))

(defn tag-tip-factura [tip]
  (case tip
    "primita" "is-info"
    "trimisa" "is-success"
    "eroare" "is-danger"
    "is-warning"))

(defn row-factura-descarcata-detalii 
  [{:keys [data_creare client id_descarcare id_solicitare tip furnizor valuta total data_scadenta data_emitere serie_numar href cif]}]
  (let [dc (u/parse-date data_creare)
        parsed_date (str (:data_c dc) "-" (:ora_c dc))
        path (u/build-path data_creare)
        zip-file-name (str id_descarcare ".zip")
        final-path (str "date/" cif "/" path "/" zip-file-name)
        app-dir (System/getProperty "user.dir")
        full-path (str app-dir "/" href)
        xml-name (str id_solicitare ".xml")
        #_xml-content #_(u/read-file-from-zip full-path xml-name)
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
            [:button.button.is-ghost {:type "submit"} "pdf"]]]]]]]])))



(defn tabel-facturi-descarcate [rows]
  (h/html
   [:table.table.is-hoverable
    (table-header-facturi-descarcate)
    (for [r rows]
      r)]))

(defn tabel-logs-api-calls [rows]
  (h/html
   [:table.table.is-hoverable
    (table-header-api-calls)
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

(defn logs-api-calls
  [cif ds]
  (let [t (str "Istoric apeluri api Anaf - cif " cif)
        calls (db/fetch-apeluri-anaf-logs ds cif)]
    (h/html
     [:div#main-container.block
      (title t)
      [:table.table.is-hoverable
       (for [c calls]
         (row-log-api-call c))]])))




