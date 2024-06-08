(ns user
  (:require ;;[hyperfiddle.rcf]
            [portal.api :as p]
            [malli.core :as m]
            [malli.generator :as mg]))

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