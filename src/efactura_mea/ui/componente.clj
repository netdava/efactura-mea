(ns efactura-mea.ui.componente
  (:require [hiccup2.core :as h]))

(defn table-header-facturi-anaf []
  (h/html
   [:tr
    [:th {:style {"font-weight" "normal"
                  "text-align" "left"}} "dată răspuns"]
    [:th "tip"]
    [:th "număr înregistrare"]
    [:th "detalii"]
    [:th "id descărcare"]]))

(defn table-header-facturi-descarcate []
  (h/html
   [:tr
    [:th {:style {"font-weight" "normal"
                  "text-align" "left"}} "nume fișier"]
    [:th "data creare"]
    [:th "detalii"]
    [:th "tip"]
    [:th "număr înregistrare"]]))

(defn row-factura-anaf [data ora tip-factura id_solicitare detalii id]
  (h/html
   [:tr
    [:td data [:br] ora]
    [:td tip-factura]
    [:td id_solicitare]
    [:td detalii]
    [:td id]]))

(defn row-factura-descarcata [href name creation-date detalii tip id_solicitare]
  (h/html
   [:tr
    [:td [:a {:href href :target "_blank"} name]]
    [:td creation-date]
    [:td detalii]
    [:td tip]
    [:td id_solicitare]]))

(defn tabel-facturi-descarcate [rows]
  (h/html
   [:table
    (table-header-facturi-descarcate)
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
               [:div.facturi
                [:h4 "Facturi ANAF"]
                [:div {:style {"width" "1000px"
                               "word-wrap" "break-word"}}
                 [:table
                  r]]]))
   :headers {"content-type" "text/html"}})

(defn days-select-options [days]
  (h/html
   (for [n days]
     [:option {:value n} n])))