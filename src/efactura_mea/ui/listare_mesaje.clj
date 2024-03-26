(ns efactura-mea.ui.listare-mesaje
  (:require [clojure.string :as s]
            [hiccup2.core :as h]
            [jsonista.core :as j]
            [efactura-mea.web.api :as api]
            [efactura-mea.ui.input-validation :as v]))

(defn table-header []
  (h/html
   [:tr
    [:th {:style {"font-weight" "normal"
                  "text-align" "left"}} "Dată răspuns"]
    [:th "Tip"]
    [:th "Număr înregistrare"]
    [:th "Detalii"]
    [:th "Id descărcare"]]))

(defn parse-date [date]
  (let [an (subs date 0 4)
        luna (subs date 4 6)
        zi (subs date 6 8)
        ora (subs date 8 10)
        min (subs date 10 12)
        data-creare (str zi "." luna "." an)
        ora-creare (str ora ":" min)]
    {:data_c data-creare
     :ora_c ora-creare}))

(defn parse-message [m]
  (let [{:keys [data_creare tip id_solicitare detalii id]} m
        data-creare-mesaj (parse-date data_creare)
        d (:data_c data-creare-mesaj)
        h (:ora_c data-creare-mesaj)
        tip-low (s/lower-case tip)]
    (h/html
     [:tr
      [:td d [:br] h]
      [:td tip-low]
      [:td id_solicitare]
      [:td detalii]
      [:td id]])))

(defn call-for-lista-facturi [target ds zile cif]
  (let [lista-mesaje (api/obtine-lista-facturi target ds zile cif)
        err (:eroare lista-mesaje)
        mesaje (:mesaje lista-mesaje)
        parsed-messages (for [m mesaje]
                          (parse-message m))]
    (when mesaje
      (table-header)
      parsed-messages)
    (when err err)))

(defn parse-validation-result [validation-result]
  (let [err validation-result
        msg-err-zile (first (:zile err))
        msg-err-cif (first (:cif err))]
    [:ul.err-msg
     (when msg-err-zile [:li msg-err-zile])
     (when msg-err-cif [:li msg-err-cif])]))

(defn listeaza-mesaje
  [req conf ds]
  (let [target (:target conf)
        q (:query-params req)
        querry-params (-> q
                          (j/write-value-as-bytes j/default-object-mapper)
                          (j/read-value j/keyword-keys-object-mapper))
        zile (:zile querry-params)
        zile-int (if (contains? querry-params :zile)
                   (try (Integer/parseInt zile)
                        (catch Exception _ zile))
                   nil)
        cif (:cif querry-params)
        validation-result  (v/validate-input-data zile-int cif)
        r (if (nil? validation-result)
            (try (call-for-lista-facturi target ds zile cif)
                 (catch Exception e (str (.getMessage e) ": parametri cerere " querry-params)))
            (parse-validation-result validation-result))]
    {:status 200
     :body (str (h/html
                 [:div.facturi
                  [:h4 "Facturi ANAF"]
                  [:div {:style {"width" "1000px"
                                 "word-wrap" "break-word"}}
                   [:table
                    r]]]))
     :headers {"content-type" "text/html"}}))
