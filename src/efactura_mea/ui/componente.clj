(ns efactura-mea.ui.componente
  (:require [hiccup2.core :as h]
            [efactura-mea.db.db-ops :as db]
            [efactura-mea.util :as u]
            [java-time.api :as jt]
            [efactura-mea.ui.pagination :as pag]))

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
  ;; TODO de pus conditia ca meniul sa fie disponibil doar daca compania a fost inregistrata corect cu token si tot ce trebuie
  (let [{:keys [cif page per-page]} opts
        qp (when (and page per-page)
             (str "?page=" page "&per-page=" per-page))
        link-facturi-descarcate (str "/facturi/" cif)
        link-facturi-spv (str "/facturi-spv/" cif)
        link-logs (str "/logs/" cif qp)
        link-descarcare-automata (str "/descarcare-automata/" cif)
        link-profil (str "/profil/" cif)
        link-integrare (str "/integrare/" cif)
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
                  :hx-target "#main-content"} "Jurnal actiuni"]]]
       [:p.menu-label "Administrare"]
       [:ul.menu-list
        [:li [:a {:href link-descarcare-automata} "Descărcare automată facturi"]]
        [:li [:a {:href link-integrare} "Integrare automată"]]
        [:li [:a {:hx-get link-descarcare-exportare
                  :hx-target "#main-content"
                  :hx-push-url "true"} "Descarcă/Exportă"]]]]]]))

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

(defn select-a-company [companies]
  (str (h/html
        [:nav.panel.is-primary
         [:p.panel-heading "Companii înregistrate"]
         [:div.panel-block
          [:p.control.has-icons-left
           [:input.input {:type "text" :placeholder "Search"}]
           [:span.icon.is-left
            [:i.fa.fa-search {:aria-hidden "true"}]]]]
         #_[:p.panel-tabs
            [:a.is-active "All"]
            [:a "Public"]
            [:a "Private"]]
         [:div.panel-block
          [:a {:href "/inregistrare-noua-companie"}
           [:p.control
            [:button.button.is-link.is-small "Înregistrează companie"]]]]
         (for [c companies]
           (let [{:keys [cif name]} c
                 url (str "/profil/" cif)]
             [:a.panel-block
              {:href url}
              [:span.panel-icon
               [:i.fa.fa-bar-chart  {:aria-hidden "true"}]]
              (str name " -- " cif)]))])))

(defn facturi-descarcate [table-with-pagination]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (str (h/html
               [:div#main-container.block
                (title "Facturi descărcate local")
                table-with-pagination]))})

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

(defn row-log-api-call
  [{:keys [id data_apelare url tip status_code]}]
  (h/html
   [:tr
    [:td.is-size-7 id]
    [:td.is-size-7 data_apelare]
    [:td.is-size-7 url]
    [:td.is-size-7 tip]
    [:td.is-size-7 status_code]]))

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



(defn tabel-facturi-descarcate [rows]
  (h/html
   [:table.table.is-hoverable
    (table-header-facturi-descarcate)
    (for [r rows]
      r)]))

(defn validation-message [err-days err-cif]
  (h/html
   [:ul.err-msg
    (when err-days [:li err-days])
    (when err-cif [:li err-cif])]))

(defn details-table
  "Primeste un map
   Genereaza un tabel pe baza tuturor perechilor k-v"
  [details-map]
  (h/html
   [:div.column
    [:table.table
     [:tbody
      (for [[k v] details-map]
        [:tr
         [:th k]
         [:td.has-text-right v]])]]]))


(defn lista-mesaje [r]
  {:status 200
   :body (str (h/html
               [:div.content.block
                [:h2 "Facturi disponibile pentru descărcat:"]
                [:table.table
                 r]]))
   :headers {"content-type" "text/html"}})

(defn logs-list
  [ds cif page per-page]
  (let [uri (str "/logs/" cif)
        count-logs (db/count-apeluri-anaf-logs ds cif)
        api-call-logs (db/fetch-apeluri-anaf-logs ds cif page per-page)
        total-pages (pag/calculate-pages-number count-logs per-page)
        logs (for [c api-call-logs]
               (row-log-api-call c))]
    (h/html
     [:table.table.is-hoverable
      logs]
     (pag/make-pagination total-pages page per-page uri))))

(defn logs-api-calls
  [ds opts]
  (let [{:keys [page per-page cif]} opts
        t (str "Istoric apeluri api Anaf - cif " cif)]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (str (h/html
                 [:div#main-container.block
                  (title t)
                  [:div#logs-table
                   (logs-list ds cif page per-page)]]))}))




^:rct/test
(comment
  (facturi-descarcate {:path-params {:cif "12345678"}})
  ;;=> {:status 200,
  ;;    :headers {"content-type" "text/html"},
  ;;    :body
  ;;    "<div class=\"block\" id=\"main-container\"><div><p class=\"title is-4\">Facturi descărcate local</p><hr class=\"title-hr\" /></div>{:path-params {:cif &quot;12345678&quot;}}</div>"}
  
  (facturi-descarcate {:path-params {:cif nil}})
  ;;=> {:status 200,
  ;;    :headers {"content-type" "text/html"},
  ;;    :body
  ;;    "<div class=\"block\" id=\"main-container\"><div><p class=\"title is-4\">Facturi descărcate local</p><hr class=\"title-hr\" /></div>{:path-params {:cif nil}}</div>"}
  
  (facturi-descarcate {:path-params {:cif nil}})
  ;;=> {:status 200,
  ;;    :headers {"content-type" "text/html"},
  ;;    :body
  ;;    "<div class=\"block\" id=\"main-container\"><div><p class=\"title is-4\">Facturi descărcate local</p><hr class=\"title-hr\" /></div>{:path-params {:cif nil}}</div>"}


  0)

