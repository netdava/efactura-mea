(ns efactura-mea.web.companii
  (:require
   [efactura-mea.db.db-ops :as db-ops]
   [efactura-mea.db.facturi :as facturi]
   [efactura-mea.web.companii.facturi :as wfacturi]
   [efactura-mea.web.companii.profil :as profil]
   [efactura-mea.web.descarca-exporta :as de]
   [efactura-mea.web.layout :as layout]
   [efactura-mea.web.logs :as logs]
   [efactura-mea.web.middleware :refer [pagination-params-middleware]]
   [efactura-mea.web.ui.componente :as ui]
   [efactura-mea.web.utils :as wu]
   [hiccup2.core :as h]
   [reitit.core :as r]
   [ring.util.response :as rur]))

(defn select-a-company
  [router companies]
  (let [registration-url (wu/route-name->url router ::register)]
    [:nav.panel
     [:p.panel-heading "Companii"]
     [:div.panel-block
      [:div.control.has-icons-left
       [:input.input {:type "text" :placeholder "Search"}]
       [:span.icon.is-left
        [:i.fa.fa-search {:aria-hidden "true"}]]]
      [:div.pannel-block
       [:button.button {:href "/companii/search"} "Caută"]]
      [:a.panel-block {:href registration-url}
       [:p.control
        [:button.button.is-link "Înregistrează companie"]]]]
     (for [c companies]
       (let [{:keys [cif name]} c
             url (wu/route-name->url router ::facturi-companie {:cif cif})]
         [:a.panel-block
          {:href url}
          [:span.panel-icon
           [:i.fa.fa-bar-chart  {:aria-hidden "true"}]]
          (str name " -- " cif)]))]))

(defn formular-inregistrare-companie
  [router]
  (let [t (str "Înregistrare companie în eFactura")
        registration-url (wu/route-name->url router ::register)]
    (h/html
     [:div#main-container.block
      (ui/title t)
      [:form.block {:hx-post registration-url
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
  (try
    (let [{:keys [cif name website address]} params
          website (or website nil)
          address (or address nil)
          inregistrata? (db-ops/test-companie-inregistrata ds cif)
          _ (if (not inregistrata?)
              (do
                (facturi/insert-company ds {:cif cif :name name :website website :address address})
                (db-ops/init-automated-download ds))
              (throw (Exception. (str "compania cu cif " cif " figurează ca fiind deja înregistrată."))))
          m (str "Compania \"" name "\" cu cif \"" cif "\" a fost înregistrată cu succes.")
          detailed-msg (str (h/html
                             [:article.message.is-success
                              [:div.message-header
                               [:p "Felicitări"]]
                              [:div.message-body
                               m]]))]
      (-> (rur/response detailed-msg)
          (rur/content-type "text/html")))
    (catch
     Exception e
      (let [err-msg (.getMessage e)
            detailed-msg (str (h/html
                               [:article.message.is-warning
                                [:div.message-header
                                 [:p "Nu s-a putut inregistra compania:"]]
                                [:div.message-body
                                 err-msg]]))]
        (-> (rur/response detailed-msg)
            (rur/content-type "text/html"))))))

(defn afisare-companii-inregistrate
  [router ds]
  (let [companii (db-ops/get-companies-data ds)]
    (str (h/html (select-a-company router companii)))))

(comment

  (afisare-companii-inregistrate {} efactura-mea.db.ds/ds))

(defn handle-companies-list
  [req]
  (let [{:keys [headers ds ::r/router]} req
        {:strs [hx-request]} headers
        content (afisare-companii-inregistrate router ds)
        sidebar (layout/sidebar-select-company)]
    (if hx-request
      (-> (afisare-companii-inregistrate router ds)
          (rur/response)
          (rur/content-type "text/html"))
      (layout/main-layout content sidebar))))

(defn register-new-company
  [req]
  (let [content (formular-inregistrare-companie (:router req))
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
        sidebar (ui/sidebar-company-data {:cif cif :router router})]
    (layout/main-layout content sidebar)))

(defn desc_aut_status_on? [status]
  (case status
    "on" true
    "off" false
    false))

(defn handler-formular-descarcare-automata
  [req]
  (let [{:keys [path-params ds]} req
        {:keys [cif]} path-params
        c-data (db-ops/get-company-data ds cif)
        my-selected-val (fn [n]
                          (let [opts {:value n}]
                            (when (= n 5) (assoc opts :selected true))))
        days (range 1 60)
        days-select-vals (for [n days]
                           [:option (my-selected-val n) n])
        {:keys [desc_aut_status]} c-data
        status (desc_aut_status_on? desc_aut_status)
        status-msg (if status
                     "Serviciul descărcare automată pornit"
                     "Serviciul descărcare automată oprit")
        body [:form.block {:hx-get "/pornire-descarcare-automata"
                           :hx-target "#status"
                           :hx-swap "innerHTML"}
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
               [:label.label "Număr de zile pentru descărcare automată facturi anaf:"]
               [:div.select [:select {:id "zile" :name "zile"}
                             days-select-vals]]]
              [:div.field
               [:input {:id "descarcare-automata"
                        :type "checkbox"
                        :checked status
                        :name "descarcare-automata"
                        :class "switch is-rounded is-info"}]
               [:label {:for "descarcare-automata"} "Activează descarcarea automată"]]
              [:div.field.content.is-small
               [:span.icon-text
                [:span.icon.has-text-info
                 [:i.fa.fa-info-circle]]
                [:p#status status-msg]]]
              [:button.button.is-small.is-link {:type "submit"} "Setează"]]]
    (-> (rur/response (str (h/html body)))
        (rur/content-type "text/html"))))

(defn routes
  []
  [["" #'handle-companies-list]
   ["/inregistreaza" {:name ::register
                      :get #'register-company
                      :post #'register-new-company}]
   ["/:cif"
    ["/facturi" {:name ::facturi-companie
                 :get #'wfacturi/handler-afisare-facturi-descarcate
                 :middleware [pagination-params-middleware]}]
    ["/profil" {:name ::profil
                :handler #'handle-company-profile}]
    ["/facturile-mele" {:name ::facturile-mele
                        :get #'wfacturi/handler-lista-mesaje-spv
                        :middleware [pagination-params-middleware]}]
    ["/export-facturi" {:name ::export-facturi
                        :get #'de/handler-descarca-exporta}]
    ["/jurnal-spv" {:name ::jurnal-spv
                    :get #'logs/handler-logs
                    :middleware [pagination-params-middleware]}]
    ["/facturi-spv" {:name ::facturi-spv
                     :get #'wfacturi/handler-facturi-spv
                     :middleware [pagination-params-middleware]}]
    ["/descarcare-automata" {:name ::descarcare-automata
                             :handler #'handler-formular-descarcare-automata}]]])

(comment

  (desc_aut_status_on? "on"))