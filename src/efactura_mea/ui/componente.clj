(ns efactura-mea.ui.componente
  (:require [hiccup2.core :as h]))

(defn table-header-facturi []
  (h/html
   [:tr
    [:th {:style {"font-weight" "normal"
                  "text-align" "left"}} "Dată răspuns"]
    [:th "Tip"]
    [:th "Număr înregistrare"]
    [:th "Detalii"]
    [:th "Id descărcare"]]))

(defn table-row-factura [data ora tip-factura id_solicitare detalii id]
  (h/html
   [:tr
    [:td data [:br] ora]
    [:td tip-factura]
    [:td id_solicitare]
    [:td detalii]
    [:td id]]))

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