(ns efactura-mea.util
  (:require [clojure.string :as s]
            [clojure.data.xml :as xml]
            [clojure.java.io :as io]
            [jsonista.core :as j])
  (:import (java.util.zip ZipFile)))

(defn list-files-from-dir [dir]
  (let [directory (io/file dir)
        dir? #(.isDirectory %)]
    (map #(.getPath %)
         (filter (comp not dir?)
                 (tree-seq dir? #(.listFiles %) directory)))))


(defn is-file-in-dir? [file dir]
  (let [files (list-files-from-dir dir)]
    (some #(= file %) files)))
(defn entries [zipfile]
  (enumeration-seq (.entries zipfile)))

(defn file-in-dir? [dir file-name]
  (let [dir-file (io/file dir)]
    (some #(= (.getName %) file-name) (file-seq dir-file))))

(defn walkzip [fileName]
  (with-open [z (ZipFile. fileName)]
    (doseq [e (entries z)]
      (println (.getName e)))))

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

(defn json-encode->edn [json-str]
  (-> json-str
      (j/write-value-as-bytes j/default-object-mapper)
      (j/read-value j/keyword-keys-object-mapper)))

(defn encode-body-json->edn [body]
  (let [object-mapper (j/object-mapper {:decode-key-fn true})
        response (j/read-value body object-mapper)]
    response))

(defn encode-request-params->edn [req]
  (let [q (:query-params req)
        edn-q-params (json-encode->edn q)]
    edn-q-params))

(defn zip-file->xml-to-bytearray [filepath]
  (let [is (io/input-stream filepath)
        zin (java.util.zip.ZipInputStream. is)]
    ((fn proc-entry [zin]
       (if-let [entry (.getNextEntry zin)]
         (if (and (not (.isDirectory entry)) (not (.contains (.getName entry) "semnatura")))
           (do
             (let [in (java.io.BufferedInputStream. zin)
                   out (java.io.ByteArrayOutputStream.)]
               (io/copy in out)
               (.closeEntry zin)
               (lazy-seq (cons {:entry entry :contents (.toByteArray out)} (proc-entry zin)))))
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
        suma-de-plata (extract-nested-field data [:LegalMonetaryTotal :PayableAmount])]
    {:serie-numar serie-numar
     :data-emitere data-emitere
     :data-scadenta data-scadenta
     :furnizor furnizor
     :client client
     :total suma-de-plata
     :valuta valuta}))

(defn get-invoice-data [path]
  (let [xml-data-from-zip (zip-file->xml-to-bytearray path)
        inv-byte-arr (:contents (first xml-data-from-zip))
        edn-inv-content (parse-xml-byte-array inv-byte-arr)]
    (parse-invoice-data edn-inv-content)))

(defn path->id-mesaj [invoice-local-path]
  (let [zip-file-name (last (s/split invoice-local-path #"/"))
        id (first (s/split zip-file-name #"\.zip"))]
    id))

(defn input-stream-to-byte-array [input-stream]
  (with-open [baos (java.io.ByteArrayOutputStream.)]
    (io/copy input-stream baos)
    (.toByteArray baos)))

(defn input-stream-to-bytes [stream]
  (let [bytes (input-stream-to-byte-array stream)]
    (vec (map byte bytes))))
