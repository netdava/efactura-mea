(ns efactura-mea.web.companii.profil
  (:require
   [efactura-mea.web.ui.componente :as ui :refer [title details-table]]
   [efactura-mea.db.db-ops :as db :refer [get-company-data]]
   [efactura-mea.db.facturi :as facturi :refer [select-acc-token-exp-date]]
   [hiccup2.core :as h]))

(defn afisare-profil-companie
  [req]
  (let [{:keys [path-params ds]} req
        {:keys [cif]} path-params
        company (get-company-data ds cif)
        token-expiration-date (select-acc-token-exp-date ds {:cif cif})
        {:keys [name website address desc_aut_status date_modified]} company
        descarcare-automata-status  (h/html [:span.has-text-weight-bold.is-uppercase desc_aut_status] " - " [:span.is-size-6 date_modified])
        descarcare-automata-url (str "/descarcare-automata/" cif)
        descarcare-automata-link [:a {:href descarcare-automata-url} [:span.icon [:i.fa.fa-pencil-square]]]]
    (h/html
     (title "Pagina de profil a companiei")
     [:div.columns.is-vcentered
      [:div.column.is-2.has-text-centered
       [:figure.image.is-128x128.is-inline-block
        [:img.is-rounded {:src "/android-chrome-192x192.png" :alt "Netdava logo"}]]]
      [:div.column
       [:div.content
        [:h1.title.is-4 name]
        [:a {:href website} website]]]]
     [:div.columns
      [:div.column
       (details-table {"Companie:" name "CIF:" cif "Website:" website "Adresă:" address "Dată expirare access_token: " token-expiration-date "Descărcare automată:" [:div#das descarcare-automata-link descarcare-automata-status]})]])))