(ns efactura-mea.util
  (:require
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [jsonista.core :as j]
   [java-time.api :as jt])
  (:import
   (java.util.zip ZipFile)
   (java.time.format DateTimeFormatter)))

(defn back-to-string-formatter
  [date]
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd")]
    (.format date formatter)))

(defn file-in-dir? [dir file-name]
  (let [dir-file (io/file dir)]
    (some #(= (.getName %) file-name) (file-seq dir-file))))

(defn formatted-date-now
  []
  (let [inst-now (jt/zoned-date-time)
        now (jt/format "H:mm - MMMM dd, yyyy" inst-now)]
    now))

(defn simple-date-now
  []
  (let [inst-now (jt/zoned-date-time)
        now (jt/format "yyyy-MM-dd" inst-now)]
    now))

(defn build-url
  "Build a url from a base and a target {:endpoint <type>}
   - <type> can be :prod or :test;"
  [url-base target]
  (let [type (:endpoint target)]
    (format url-base (name type))))

(defn build-path [data-creare]
  (let [an (subs data-creare 0 4)
        luna (subs data-creare 4 6)]
    (str an "/" luna)))

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

(defn encode-body-json->edn [body]
  (let [object-mapper (j/object-mapper {:decode-key-fn true})
        response (j/read-value body object-mapper)]
    response))

(defn zip-file->xml-to-bytearray [filepath]
  (let [is (io/input-stream filepath)
        zin (java.util.zip.ZipInputStream. is)]
    ((fn proc-entry [zin]
       (if-let [entry (.getNextEntry zin)]
         (if (and (not (.isDirectory entry)) (not (.contains (.getName entry) "semnatura")))
           (let [in (java.io.BufferedInputStream. zin)
                 out (java.io.ByteArrayOutputStream.)]
             (io/copy in out)
             (.closeEntry zin)
             (lazy-seq (cons {:entry entry :contents (.toByteArray out)} (proc-entry zin))))
           (lazy-seq (proc-entry zin)))
         (do
           (.close is)
           (.close zin)))) zin)))

(defn parse-xml-byte-array
  [byte-array]
  (with-open [stream (java.io.ByteArrayInputStream. byte-array)]
    (xml/parse stream)))

(defn extract-nested-field [edn tags]
  (if (empty? tags)
    (first (:content edn))
    (let [tag (first tags)
          next-edn (some #(when (= tag (:tag %)) %) (:content edn))]
      (if next-edn
        (recur next-edn (rest tags))
        nil))))

(defn parse-invoice-data [data]
  (let [serie-numar (extract-nested-field data [:ID])
        data-emitere (extract-nested-field data [:IssueDate])
        data-scadenta (extract-nested-field data [:DueDate])
        valuta (extract-nested-field data [:DocumentCurrencyCode])
        furnizor (or (extract-nested-field data [:AccountingSupplierParty :Party :PartyName :Name])
                     (extract-nested-field data [:AccountingSupplierParty :Party :PartyLegalEntity :RegistrationName]))
        client (or (extract-nested-field data [:AccountingCustomerParty :Party :PartyName :Name])
                   (extract-nested-field data [:AccountingCustomerParty :Party :PartyLegalEntity :RegistrationName]))
        suma-de-plata (extract-nested-field data [:LegalMonetaryTotal :TaxInclusiveAmount])]
    {:serie_numar serie-numar
     :data_emitere data-emitere
     :data_scadenta data-scadenta
     :furnizor furnizor
     :client client
     :total suma-de-plata
     :valuta valuta}))

(defn get-invoice-data [path]
  (try (let [xml-data-from-zip (zip-file->xml-to-bytearray path)
             inv-byte-arr (:contents (first xml-data-from-zip))
             edn-inv-content (parse-xml-byte-array inv-byte-arr)]
         (parse-invoice-data edn-inv-content))
       (catch Exception _ {:error (str "lipseste fisierul: " path)})))

(defn read-file-from-zip [zip-file-path file-name-inside-zip]
  (with-open [zip-file (ZipFile. zip-file-path)]
    (let [entry (.getEntry zip-file file-name-inside-zip)]
      (if entry
        (with-open [stream (.getInputStream zip-file entry)]
          (slurp (io/reader stream)))
        (throw (Exception. (str "File " file-name-inside-zip " not found in " zip-file-path)))))))

#_(defn extract-query-params [url]
  (try (let [uri (java.net.URI. url)
             query (.getQuery uri)
             params (when query
                      (->> (s/split query #"&")
                           (map #(s/split % #"="))
                           (map (fn [[k v]] [(keyword k) (Integer/parseInt v)]))
                           (into {})))]
         params)
       (catch Exception _ {:page nil :per-page nil})))