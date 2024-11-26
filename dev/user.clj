(ns user
  (:require ;;[hyperfiddle.rcf]

   [malli.core :as m]
   [malli.generator :as mg]
   [clojure.test.check.generators :as gen]))

;; https://github.com/djblue/portal?tab=readme-ov-file#api
;; (def p (p/open {:launcher :vs-code}))  ; jvm / node only
;; (add-tap #'p/submit) ; Add portal as a tap> target

; (com.mjdowney.rich-comment-tests/run-ns-tests! *ns*)

(comment

  (require '[malli.core :as m])
  (require '[malli.generator :as mg])
  (require '[malli.registry :as mr])
  (require '[malli.experimental.time :as met])
  (require '[malli.experimental.time.generator])

  (defn generate-timestamp []
    (.toString (java.time.ZonedDateTime/now)))
  
  (def id-gen
    (gen/fmap str (gen/large-integer* {:min 1000000000 :max 9999999999})))
  
  (def data-creare-gen
    (gen/fmap (fn [_]
                (let [year "2024"
                      month (format "%02d" (+ 1 (rand-int 12)))
                      day (format "%02d" (+ 1 (rand-int 28)))
                      hour (format "%02d" (rand-int 24))
                      minute (format "%02d" (rand-int 60))]
                  (str year month day hour minute)))
              (gen/return nil)))
  
  (def descarcare-schema
    [:map
     [:data_descarcare {:gen/gen (gen/fmap (fn [_] (generate-timestamp)) (gen/return nil))} string?]
     [:id_descarcare {:gen/gen id-gen} string?]
     [:cif {:gen/gen (gen/return "35586426")} string?]
     [:tip {:gen/gen (gen/return "FACTURA TRIMISA")} string?]
     [:detalii {:gen/gen (gen/return "detalii")} string?]
     [:data_creare {:gen/gen data-creare-gen} string?]
     [:id_solicitare {:gen/gen id-gen} string?]])
  
  (defn generate-descarcare-data [n]
    (repeatedly n #(mg/generate descarcare-schema)))
  
  (generate-descarcare-data 5)

  (mr/set-default-registry!
   (mr/composite-registry
    (m/default-schemas)
    (met/schemas)))

  (let [listare-mesaj-spec
        [:map {:title "Validare cerere listare-mesaje"}
         [:zile {:title "Număr de zile de listat"
                 :optional true}
          [:int {:min 1 :max 60}]]]
        date-de-validat {:zilea -1}]
    (if (m/validate listare-mesaj-spec date-de-validat)
      "ok"
      (:errors (m/explain listare-mesaj-spec date-de-validat)))
    #_(mg/generate listare-mesaj-spec))

  (m/validate [:map ["my-int-key" :string]] {"my-int-key" ""})

  (m/validate [:or
               :nil
               [:map {:title "Un map cu zile si cui"}
                [:zile :int]
                [:cui {:title "CUI societata"
                       :optional true} :string]]]
              nil)

  (mg/generate :time/duration)

  (def Address
    [:map
     [:street :string]
     [:country [:enum "FI" "UA"]]])

  (mg/generate [:vector {:min 0 :max 3} Address])


  )