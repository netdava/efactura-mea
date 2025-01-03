(ns efactura-mea.web.anaf-integrare
  "Integrare cu API ANAF pentru a genera / reîmprospăta token"
  (:require
   [efactura-mea.layout :as layout]
   [efactura-mea.ui.componente :as ui]
   [efactura-mea.web.oauth2-anaf :as o2a]
   [hiccup2.core :as h]))

(defn afisare-integrare-efactura
  [req]
  (let [{:keys [path-params]} req
        {:keys [cif]} path-params
        url-autorizare (str "/autorizeaza-acces-fara-certificat/" cif)]
    (h/html
     (ui/title "Integrare E-factura")
     [:div.content
      [:p "Activează integrarea automata cu E-factura "]
      [:div.columns
       [:div.column
        [:div.notification
         [:p.is-size-7 "Dacă " [:span.has-text-weight-bold "ai permisiunea "] "de autentificare în S.P.V." [:span.has-text-weight-bold " cu certificatul digital:"]]
         [:button.button.is-small.is-link {:hx-get "/autorizeaza-acces-cu-certificat"}
          (str "Autorizează accesul pentru CUI:" cif)]]]
       [:div.column
        [:div.notification
         [:p.is-size-7 [:span.has-text-weight-bold "Fără permisiunea "] "de autentificare în S.P.V." [:span.has-text-weight-bold " cu certificatul digital:"]]
         [:button.button.is-small.is-link {:hx-get url-autorizare
                                           :hx-target "#modal-wrapper"
                                           :hx-swap "innerHTML"}
          (str "Autorizează accesul pentru CUI:" cif)]]]]
      [:div#modal-wrapper]])))

(defn handler-integrare
  [req]
  (let [{:keys [path-params]} req
        {:keys [cif]} path-params
        content (afisare-integrare-efactura req)
        sidebar (layout/sidebar-company-data {:cif cif})]
    (layout/main-layout content sidebar)))

(defn modala-link-autorizare
  [_]
  (str (h/html [:div.modal.is-active
                {:_ "on closeModal remove .is-active then remove me"}
                [:div.modal-background]
                [:div.modal-content
                 [:div.box [:div
                            [:p.title.is-5 "Obține accesul prin delegat (persoană cu acces în S.P.V.)"]
                            [:hr.title-hr]
                            [:p "Copiază și trimite link-ul de mai jos pentru autorizarea accesului eFacturaMea în e-Factura contabilului tău, respectiv persoanei care poate accesa S.P.V. ANAF cu certificat digital pentru firma ta."]
                            [:div.block
                             [:textarea.textarea "ceva text in textarea"]]
                            [:div.buttons
                             [:button.button.is-small "Copiază"]
                             [:button.button.is-small "Trimite pe mail"]]]]]
                [:button.modal-close.is-large {:aria-label "close"
                                               :_ "on click trigger closeModal"}]])))

(defn handler-autorizare-fara-certificat
  [req]
  (let [{:keys [path-params]} req
        {:keys [cif]} path-params
        content (modala-link-autorizare cif)]
    {:status 200
     :body content
     :headers {"content-type" "text/html"}}))


(defn routes
  [anaf-conf]
  [["/login-anaf" (o2a/make-anaf-login-handler
                   (anaf-conf :client-id)
                   (anaf-conf :redirect-uri))]
   ["/integrare/:cif" handler-integrare]
   ["/autorizeaza-acces-fara-certificat/:cif" handler-autorizare-fara-certificat]])
