(ns efactura-mea.web.companii
  (:require
   [efactura-mea.layout :as layout]
   [efactura-mea.ui.componente :as ui]
   [efactura-mea.db.db-ops :as db]
   [hiccup2.core :as h]))

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
          [:a {:href "/companii/inregistrare-noua-companie"}
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

(defn formular-inregistrare-companie
  []
  (let [t (str "Înregistrare companie în eFactura")]
    (h/html
     [:div#main-container.block
      (ui/title t)
      [:form.block {:hx-get "/inregistreaza-companie"
                    :hx-target "#main-container"
                    :hx-swap "innerHTML swap:1s"}
       [:div.field
        [:label.label "CIF:"]
        [:input.input {:type "text"
                       :id "cif"
                       :name "cif"
                       :placeholder "cif companie"}]]
       [:div.field
        [:label.label "Denumire companie"]
        [:input.input {:type "text"
                       :id "name"
                       :name "name"}]]
       [:div.field
        [:label.label "Website"]
        [:input.input {:type "text"
                       :id "website"
                       :name "website"}]]
       [:div.field
        [:label.label "Physical Address"]
        [:input.input {:type "text"
                       :id "address"
                       :name "address"}]]
       [:button.button.is-small.is-link {:type "submit"} "Adaugă companie"]]])))

(defn afisare-companii-inregistrate [ds]
  (let [companii (db/get-companies-data ds)]
    {:status 200
     :body (select-a-company companii)
     :headers {"content-type" "text/html"}}))

(defn handle-companies-list
  [req]
  (let [{:keys [headers ds]} req
        {:strs [hx-request]} headers
        content (afisare-companii-inregistrate ds)
        sidebar (layout/sidebar-select-company)]
    (if hx-request
      content
      (layout/main-layout (:body content) sidebar))))

(defn handle-register-company
  [_]
  (let [content (formular-inregistrare-companie)
        sidebar (ui/sidebar-select-company)]
    (layout/main-layout content sidebar)))