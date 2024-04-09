(ns efactura-mea.ui.listare-mesaje
  (:require [clojure.string :as s]
            [clojure.edn :as edn]
            [hiccup2.core :as h]
            [jsonista.core :as j]
            [efactura-mea.web.api :as api]
            [efactura-mea.ui.input-validation :as v]
            [efactura-mea.db.facturi :as facturi]
            [efactura-mea.config :as c]
            [efactura-mea.util :as u]
            [ring.middleware.file :refer [wrap-file]]
            [ring.util.response :as r]
            [ring.util.request :as ring-req])
  (:import [java.time ZonedDateTime]))

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

(defn log-calls-with-error [ds e tip-apel]
  (let [now (ZonedDateTime/now)
        data (.getData e)
        url (.toString (:uri data))
        status (:status data)
        err-msg (.getMessage e)]
    (facturi/insert-row-apel-api ds {:data_apelare now
                                     :url url
                                     :tip tip-apel
                                     :status_code status
                                     :response err-msg})))

(defn call-for-lista-facturi [target ds zile cif]
  (let [apel-lista-mesaje (api/obtine-lista-facturi target ds zile cif)
        status (:status apel-lista-mesaje)
        body (:body apel-lista-mesaje)
        err (:eroare body)
        mesaje (:mesaje body)]
    (if (= 200 status)
      (if mesaje
        (let [parsed-messages (for [m mesaje]
                                (parse-message m))
              theader (table-header)
              table-rows (cons theader parsed-messages)]
          table-rows)
        err)
      body)))

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
            (call-for-lista-facturi target ds zile cif)
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

(defn fetch-lista-mesaje [target ds zile cif conf]
  (let [apel-lista-mesaje (api/obtine-lista-facturi target ds zile cif)
        status (:status apel-lista-mesaje)
        body (:body apel-lista-mesaje)
        err (:eroare body)
        mesaje (:mesaje body)]
    (if (= 200 status)
      (if mesaje
        (let [_ (api/track-descarcare-mesaje ds mesaje)
              queue-lista-mesaje (facturi/select-queue-lista-mesaje ds)
              queue-id (:id queue-lista-mesaje)
              lm-str (:lista_mesaje queue-lista-mesaje)
              lista-mesaje (edn/read-string lm-str)
              download-to (c/download-dir conf)
              raport-descarcare-facturi (reduce
                                         (fn [acc f]
                                           (let [id (:id f)
                                                 zip-name (str id ".zip")
                                                 test-file-exist (facturi/test-factura-descarcata? ds {:id id})]
                                             (if (empty? test-file-exist)
                                               (do (api/download-zip-file f target ds download-to)
                                                   (api/scrie-factura->db f ds)
                                                   (conj acc (str "am descarcat mesajul " zip-name)))
                                               (conj acc (str "factura " zip-name " exista salvata local")))))
                                         [] lista-mesaje)
              a (h/html [:ul
                         (for [item raport-descarcare-facturi]
                           [:li item])])
              b (let [data-dir (c/download-dir conf)
                      facturi-descarcate (u/list-files-from-dir data-dir)]
                  (for [f facturi-descarcate]
                    (let [vec-str (clojure.string/split f #"/")
                          dropped (drop 1 vec-str)
                          new-path (clojure.string/join "/" dropped)]
                      [:a {:href new-path :target "_blank"} f])))]
          (facturi/delete-row-download-queue ds {:id queue-id})
          (cons a b))
        err)
      body)))

(defn descarca-mesaje
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
            (fetch-lista-mesaje target ds zile cif conf)
            (parse-validation-result validation-result))]
    {:status 200
     :body (str (h/html
                 [:div.facturi
                  [:h4 "Facturi ANAF"]
                  [:div {:style {"width" "1000px"
                                 "word-wrap" "break-word"}}
                   r]]))
     :headers {"content-type" "text/html"}}))



