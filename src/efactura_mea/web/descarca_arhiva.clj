(ns efactura-mea.web.descarca-arhiva
  (:require
   [clojure.string :as str]
   [efactura-mea.config :as config]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.web.ui.componente :as ui]
   [efactura-mea.util :as u]
   [efactura-mea.web.descarca-exporta :as de]
   [hiccup2.core :as h]
   [ring.util.response :as rur])
  (:import
   (java.time DayOfWeek LocalDate)
   (java.time.format DateTimeFormatter)
   (java.time.temporal TemporalAdjusters)
   (java.util Locale)))

(defn get-week-range
  [date]
  (let [monday (.with date (TemporalAdjusters/previousOrSame (DayOfWeek/valueOf
                                                              "MONDAY")))
        sunday (.with date (TemporalAdjusters/nextOrSame (DayOfWeek/valueOf "SUNDAY")))]
    {:start-date monday
     :end-date sunday}))

(defn simple-formatter
  [formatter date]
  (let [fmt-date (.format date formatter)]
    fmt-date))

(defn date->month-range
  "Receives a date as java.time.LocalDate
  returns a map with first and last days of the current month."
  [date]
  (try
    (let [first-month-day (.with date (TemporalAdjusters/firstDayOfMonth))
          last-month-day (.with date (TemporalAdjusters/lastDayOfMonth))]
      {:start-date first-month-day
       :end-date   last-month-day})
    (catch Exception e
      (throw (ex-info (str "Invalid date format: " (ex-message e)) {:date date})))))

(defn handler-descarca-arhiva
  [req]
  (let [{:keys [query-params ds conf]} req
        {:strs [file_type_pdf file_type_zip]} query-params
        content (de/descarca-lista-mesaje ds conf query-params)
        {:keys [archive-content archive-name]} content
        content-disposition (str "attachment; filename=" archive-name)
        no-file-selected-info "Trebuie sa selectezi cel puțin un fip de fișier"
        no-invoices-found-info "În perioada selectata, nu au fost identificate facturi pentru descarcare"]
    (if (and (not file_type_pdf) (not file_type_zip))
      (-> (rur/response no-file-selected-info)
          (rur/content-type "text/html"))
      (if (nil? archive-content)
        (-> (rur/response no-invoices-found-info)
            (rur/content-type "text/html"))
        (-> (rur/response archive-content)
            (rur/header "Content-Type" "application/zip, application/octet-stream")
            (rur/header "Content-Disposition" content-disposition))))))

(defn month-name-formatter [date locale]
  (let [[language country] (str/split locale #"-")
        month-locale-format (DateTimeFormatter/ofPattern "MMMM" (Locale. language country))]
    (.format month-locale-format date)))

(defn sumar-descarcare-arhiva
  [ds conf query-params locale]
  (let [{:strs [cif date_first perioada]} query-params
        parsed-date (LocalDate/parse date_first)
        year-value (.getYear parsed-date)
        month-name (month-name-formatter parsed-date locale)
        month-name (ui/hiccup-bold-span month-name)
        download-dir (config/download-dir conf)
        path-facturi-disk (str download-dir "/" cif "/" year-value)
        date-after-spv-start? (de/validate-date-after-spv-start parsed-date)
        filter-opts (if date-after-spv-start?
                      (case perioada
                        "saptamana" (get-week-range parsed-date)
                        "luna" (date->month-range parsed-date))
                      (throw (Exception. "Te rog sa introduci o dată mai mare decat 2024-01-01")))
        filter-opts (assoc filter-opts :cif cif)
        facturi-cerute (map :id_descarcare (db/fetch-facturi-in-date-range ds filter-opts))
        facturi-confirmate-disk (vec (filter #(= true (u/file-in-dir? path-facturi-disk (str % ".zip"))) facturi-cerute))
        facturi-negsite (when (> (count facturi-cerute) (count facturi-confirmate-disk))
                          (let [unfound (vec (filter #(= nil (u/file-in-dir? path-facturi-disk (str % ".zip"))) facturi-cerute))
                                message (str "Următoarele facturi nu vor fi incluse în arhivă deoarece nu au fost găsite pe disk: " unfound)]
                            [:span.tag.is-warning.is-light message]))
        sum-facturi (db/count-facturi-in-date-range ds filter-opts)
        sum-facturi (ui/hiccup-bold-span sum-facturi)
        cif (h/html [:span.has-text-weight-bold cif])
        last-day-formatter (DateTimeFormatter/ofPattern "dd.MM.yyyy")
        first-day-formatter (DateTimeFormatter/ofPattern "dd")
        ;; TODO: pentru ziua 2024-10-31 imi selecteaza saptamana 28.10-03.11.2024
        ;; trebuie sa compar prin > < prima si ultima zi din saptamana
        monday (-> parsed-date
                   (.with (TemporalAdjusters/previousOrSame DayOfWeek/MONDAY))
                   (simple-formatter first-day-formatter)
                   ui/hiccup-bold-span)
        sunday (-> parsed-date
                   (.with (TemporalAdjusters/nextOrSame DayOfWeek/SUNDAY))
                   (simple-formatter last-day-formatter)
                   ui/hiccup-bold-span)
        message-text-saptamana (h/html "Pentru intervalul " monday "-" sunday " am identificat un număr de " sum-facturi " facturi pentru cif " cif)
        message-text-luna  (h/html "Pentru luna " month-name " am identificat un număr de " sum-facturi " facturi pentru cif " cif)
        mesage-text (case perioada
                      "saptamana" message-text-saptamana
                      "luna" message-text-luna)
        info-message [:div
                      [:div.notification.is-info.is-light
                       mesage-text]
                      facturi-negsite]]
    info-message))

(defn handler-sumar-descarcare-arhiva
  [req]
  (let [{:keys [query-params ds conf]} req
        locale "ro-RO"
        sumar-descarcare-arhiva (sumar-descarcare-arhiva ds conf query-params locale)]
    (-> (rur/response (str (h/html sumar-descarcare-arhiva)))
        (rur/content-type "text/html"))))

(comment
  (let [message-text-saptamana [:span "12-12-2024"]
        a (h/html "pentru " message-text-saptamana)]
    (@a))
  

  
  0)