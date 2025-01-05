(ns efactura-mea.web.companii
  (:require
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.db.facturi :as facturi]
   [efactura-mea.web.layout :as layout]
   [efactura-mea.web.ui.componente :as ui]
   [efactura-mea.web.companii.profil :as profil]
   [hiccup2.core :as h]
   [reitit.core :as r]))

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
                 url (str "/companii/profil/" cif)]
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
      [:form.block {:hx-get "/companii/inregistreaza-companie"
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

(defn inregistrare-noua-companie
  [ds params]
  (try (let [{:keys [cif name website address]} params
             website (or website nil)
             address (or address nil)
             inregistrata? (db/test-companie-inregistrata ds cif)
             _ (if (not inregistrata?)
                 (do
                   (facturi/insert-company ds {:cif cif :name name :website website :address address})
                   (db/init-automated-download ds))
                 (throw (Exception. (str "compania cu cif " cif " figurează ca fiind deja înregistrată."))))
             m (str "Compania \"" name "\" cu cif \"" cif "\" a fost înregistrată cu succes.")
             detailed-msg (str (h/html
                                [:article.message.is-success
                                 [:div.message-header
                                  [:p "Felicitări"]]
                                 [:div.message-body
                                  m]]))]
         {:status 200
          :body detailed-msg
          :headers {"content-type" "text/html"}})
       (catch
        Exception e
         (let [err-msg (.getMessage e)
               detailed-msg (str (h/html
                                  [:article.message.is-warning
                                   [:div.message-header
                                    [:p "Nu s-a putut inregistra compania:"]]
                                   [:div.message-body
                                    err-msg]]))]
           {:status 200
            :body detailed-msg
            :headers {"content-type" "text/html"}}))))

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

(defn handle-register-new-company
  [_]
  (let [content (formular-inregistrare-companie)
        sidebar (ui/sidebar-select-company)]
    (layout/main-layout content sidebar)))

(defn register-company
  [req]
  (let [{:keys [params ds]} req]
    (inregistrare-noua-companie ds params)))

(defn handle-company-profile
  [req]
  (let [{:keys [path-params ::r/router]} req
        {:keys [cif]} path-params
        content (profil/afisare-profil-companie req)
        sidebar (layout/sidebar-company-data {:cif cif :router router})]
    (layout/main-layout content sidebar)))


(def routes
  [["" handle-companies-list]
   ["/inregistrare-noua-companie" handle-register-new-company]
   ["/inregistreaza-companie" register-company]
   ["/profil/:cif" handle-company-profile]])