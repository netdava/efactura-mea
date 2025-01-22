(ns efactura-mea.web.companii.profil
  (:require
   [efactura-mea.web.ui.componente :as ui :refer [details-table]]
   [efactura-mea.util :as u]
   [efactura-mea.db.db-ops :as db
    :refer [get-company-data fetch-company-token-data]]
   [hiccup2.core :as h]
   [java-time.api :as jt]))

(defn afisare-profil-companie
  [{:keys [cif ds]}]
  (let [company (get-company-data ds cif)
        token-data (fetch-company-token-data ds cif)
        {:keys [expiration_date _updated expires_in]} token-data
        milliseconds->days (u/seconds->days expires_in)
        valability (or (str milliseconds->days " zile") "")
        parse-exp-date (u/format-utc-date expiration_date)
        parsed-token-updated-at (u/format-utc-date _updated)
        {:keys [name website address desc_aut_status date_modified]} company
        descarcare-automata-url (str "/descarcare-automata/" cif)
        refresh-token-anaf-uri (str "/anaf/refresh-access-token/" cif)]
    [:div
     [:div.columns.is-vcentered
      [:div.column.is-2.has-text-centered
       [:figure.image.is-128x128.is-inline-block
        [:img.is-rounded {:src "/android-chrome-192x192.png" :alt "Netdava logo"}]]]
      [:div.column
       [:div.content
        [:h1.title.is-4 name]
        [:a {:href website} website]]]]
     [:div.columns
      (details-table
       {"Companie:" name
        "CIF:" cif
        "Website:" website
        "Adresă:" address
        "Descărcare automată:" [:div#das
                                [:a {:href descarcare-automata-url}
                                 [:span.icon [:i.fa.fa-pencil-square]]]
                                [:span.has-text-weight-bold.is-uppercase desc_aut_status]
                                " - "
                                [:span.is-size-6 date_modified]]})
      (details-table
       {"Token" ""
        "Dată expirare: " [:div parse-exp-date
                           [:div
                            [:a.button.is-small.is-info
                             {:href refresh-token-anaf-uri}
                             "Înnoiește token"]
                            [:a.button.is-small.is-info
                             {:href refresh-token-anaf-uri}
                             "Revocă token"]]]
        "Reînnoit la:" parsed-token-updated-at
        "Valabilitate: " valability})]]))


(comment
  (jt/format   (jt/zoned-date-time "2025-01-14T16:24:20.369794436Z[UTC]"))
  0)