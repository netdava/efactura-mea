(ns efactura-mea.web.descarca-exporta
  (:require
   [babashka.http-client :as http]
   [clojure.java.io :as cio]
   [efactura-mea.config :as config]
   [efactura-mea.db.db-ops :as db]
   [efactura-mea.layout :as layout]
   [efactura-mea.ui.componente :as ui]
   [efactura-mea.util :as u]
   [efactura-mea.web.api :as api]
   [ring.util.io :as ruio])
  (:import
   (java.io FileInputStream)
   (java.time DayOfWeek LocalDate)
   (java.time.temporal TemporalAdjusters)
   (java.util.zip ZipEntry ZipOutputStream)))

(defn validate-date-after-spv-start
  [parsed-date]
  (let [anaf-start-date (LocalDate/parse "2024-01-01")]
    (not (.isBefore parsed-date anaf-start-date))))

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

^:rct/test
(comment
  (try
    (date->month-range nil)
    (catch Exception e (ex-message e)))
  ;;=> "Invalid date format: Cannot invoke \"Object.getClass()\" because \"target\" is null"

  (try
    (date->month-range "")
    (catch Exception e (ex-message e)))
  ;;=> "Invalid date format: No matching method with found taking 1 args for class java.lang.String"

  (map key (date->month-range (LocalDate/parse "2024-11-19")))
  ;;=> (:start-date :end-date)

  (map (comp str val) (date->month-range (LocalDate/parse "2024-11-19")))
  ;;=> ("2024-11-01" "2024-11-30")

  (map (comp str val) (date->month-range (LocalDate/parse "2024-12-01")))
  ;;=> ("2024-12-01" "2024-12-31")

  (map (comp str val) (date->month-range (LocalDate/parse "2024-12-31")))
  ;;=> ("2024-12-01" "2024-12-31")

  0)

(defn get-week-range
  [date]
  (let [monday (.with date (TemporalAdjusters/previousOrSame (DayOfWeek/valueOf
                                                              "MONDAY")))
        sunday (.with date (TemporalAdjusters/nextOrSame (DayOfWeek/valueOf "SUNDAY")))]
    {:start-date monday
     :end-date sunday}))

(defn facturi->input-stream
  "Returns an input-stream (piped-input-stream) to be used directly in Ring HTTP responses"
  [zip-files]
  (ruio/piped-input-stream
   (fn [output-stream]
     (with-open [zip-output-stream (ZipOutputStream. output-stream)]
       (doseq [zip-file zip-files]
         (let [zip-file (cio/as-file zip-file)
               zip-name (.getName zip-file)]
           (if (.exists zip-file)
             (let [zip-entry (ZipEntry. zip-name)]
               (.putNextEntry zip-output-stream zip-entry)
               (with-open [file-stream (FileInputStream. zip-file)]
                 (cio/copy file-stream zip-output-stream))
               (.closeEntry zip-output-stream))
             (println "File not found:" zip-name))))
       (.finish zip-output-stream)))))

(defn fetch-xml-data-from-zip
  [id_descarcare id_solicitare zip-path]
  (let [zip-file-name (str id_descarcare ".zip")
        zip-path (str zip-path "/" zip-file-name)
        xml-name (str id_solicitare ".xml")
        xml-data (u/read-file-from-zip zip-path xml-name)]
    xml-data))

(defn transformare-xml-to-pdf-salvare
  [ds cif pdf-name pdf-path xml-content]
  (let [a-token (db/fetch-access-token ds cif)
        url "https://api.anaf.ro/prod/FCTEL/rest/transformare/FACT1"
        r (http/post url {:headers {"Authorization" (str "Bearer " a-token)
                                    "Content-Type" "text/plain"}
                          :body xml-content
                          :as :stream})
        pdf-content (:body r)
        _ (api/save-pdf pdf-path pdf-name pdf-content)]
    (str pdf-path "/" pdf-name)))

(defn descarca-lista-mesaje
  [ds conf query-params]
  (let [{:strs [date_first cif perioada file_type_pdf file_type_zip]} query-params]
    (try
      (let [parsed-date (LocalDate/parse date_first)
            date-after-spv-start? (validate-date-after-spv-start parsed-date)
            filter-opts (if date-after-spv-start?
                          (case perioada
                            "saptamana" (get-week-range parsed-date)
                            "luna" (date->month-range parsed-date))
                          (throw (Exception. "Te rog sa introduci o dată mai mare decat 2024-01-01")))
            download-dir (config/download-dir conf)
            app-dir (System/getProperty "user.dir")
            filter-opts (assoc filter-opts :cif cif)
            facturi-in-date-range (db/fetch-facturi-in-date-range ds filter-opts)
            _ (println "facturiile cu iduri" facturi-in-date-range)
            empty-facturi-in-date-range? ((comp not seq) facturi-in-date-range)]
        (if empty-facturi-in-date-range?
          {:archive-content nil}
          (let [facturi-paths-for-archive (when (= file_type_zip "on")
                                            (vec
                                             (map
                                              (fn [f]
                                                (let [{:keys [id_descarcare data_creare]} f
                                                      file-path (u/build-path data_creare)]
                                                  (str download-dir "/" cif "/" file-path "/" id_descarcare ".zip"))) facturi-in-date-range)))
                ;; VREM SI PDF, ATUNCI                                \
                id->pdf-names (when (= file_type_pdf "on")
                                (vec (map (fn [f]
                                            (let [{:keys [id_descarcare data_creare id_solicitare]} f
                                                  date->path (u/build-path data_creare)
                                                  search-path (str app-dir "/" download-dir "/" cif "/" date->path)
                                                  pdf-path (str download-dir "/" cif "/" date->path)
                                                  pdf-name (str id_descarcare ".pdf")
                                                  pdf-file-path (str pdf-path "/" pdf-name)
                                                  pdf-exist? (u/file-in-dir? search-path pdf-name)
                                                  xml-content (fetch-xml-data-from-zip id_descarcare id_solicitare search-path)]
                                              (if pdf-exist?
                                                pdf-file-path
                                                (transformare-xml-to-pdf-salvare ds cif pdf-name pdf-path xml-content))))
                                          facturi-in-date-range)))
                _ (println "id->pdf-names: " id->pdf-names)
                facturi-for-archive (into [] (concat facturi-paths-for-archive id->pdf-names))
                _ (println "facturi-for-archive : " facturi-for-archive)
                {:keys [start-date end-date]} filter-opts
                arhive-name (str  cif "_" start-date "_" end-date ".zip")
                facturi-input-stream (facturi->input-stream facturi-for-archive)]
            {:archive-content facturi-input-stream
             :archive-name arhive-name})))
      (catch Exception e
        (ex-message e)))))

(defn handler-descarca-exporta [req]
  (let [{:keys [path-params headers]} req
        {:strs [hx-request]} headers
        {:keys [cif]} path-params
        content (api/afisare-descarcare-exportare cif)
        opts {:cif cif}
        sidebar (ui/sidebar-company-data opts)]
    (if (= hx-request "true")
      content
      (layout/main-layout (:body content) sidebar))))









