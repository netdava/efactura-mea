(ns efactura-mea.web.companii.profil
  (:require
   [efactura-mea.web.ui.componente :as ui :refer [title details-table]]
   [efactura-mea.util :as u]
   [efactura-mea.db.db-ops :as db
    :refer [get-company-data fetch-company-token-expiration-date]]
   [hiccup2.core :as h]
   [java-time.api :as jt]))

(defn afisare-profil-companie
  [req]
  (let [{:keys [path-params ds]} req
        {:keys [cif]} path-params
        company (get-company-data ds cif)
        token-expiration-date (fetch-company-token-expiration-date ds cif)
        parse-exp-date (try (let [formatter "yyyy-MM-dd HH:mm:ss"
                                  zoned-time (jt/zoned-date-time token-expiration-date)]
                              (jt/format formatter zoned-time))
                            (catch Exception _ "could not be displayed"))
        {:keys [name website address desc_aut_status date_modified]} company
        descarcare-automata-status  (h/html [:span.has-text-weight-bold.is-uppercase desc_aut_status] " - " [:span.is-size-6 date_modified])
        descarcare-automata-url (str "/descarcare-automata/" cif)
        descarcare-automata-link [:a {:href descarcare-automata-url} [:span.icon [:i.fa.fa-pencil-square]]]
        refresh-token-anaf-uri (str "/refresh-access-token" cif)
        refresh-btn [:button.button.is-small.is-info 
                     {:hx-get refresh-token-anaf-uri}
                     "Refresh token"]]
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
       (details-table 
        {"Companie:" name "CIF:" cif "Website:" website "Adresă:" address "Dată expirare access_token: " [:div parse-exp-date [:div refresh-btn]] "Descărcare automată:" [:div#das descarcare-automata-link descarcare-automata-status]})]])))

(comment
  (jt/format   (jt/zoned-date-time "2025-01-14T16:24:20.369794436Z[UTC]"))
  0)