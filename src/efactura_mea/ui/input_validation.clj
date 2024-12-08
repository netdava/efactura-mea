(ns efactura-mea.ui.input-validation
  (:require [malli.core :as m]
            [malli.error :as me]))

(def listare-mesaje-spec
  [:map {:title "Validare cerere listare-mesaje"}
   [:zile {:title "Numărul de zile pentru care să se afișeze răspunsul"
           :optional false}
    [:int {:min 1 :max 60}]]
   [:cif {:title "CIF-ul companiei pentru care se cere listare-mesaje"
          :optional false}
    [:string {:min 1 :max 9}]]])

(defn validate-input-data [zile cif-str]
  (-> listare-mesaje-spec
      (m/explain
       {:zile zile :cif cif-str})
      (me/humanize
       {:errors
        (-> me/default-errors
            (assoc :int {:error/fn ((fn [{:keys [pred message]}]
                                      (fn [{:keys [schema value]} _]
                                        (let [{:keys [min max]} (m/properties schema)]
                                          (cond
                                            (not (pred value)) message
                                            (and min (= min max)) (str "Numărul minim de zile trebuie sa fie " min)
                                            (and min max) (str "Numărul de zile trebuie să fie cuprins între " min " și " max)
                                            min (str "Numărul de zile trebuie să fie cel puțin " min)
                                            max (str "Numărul de zile trebuie să fie maxim " max))))) {:pred int?, :message "Trebuie să introduci un număr cuprins între 1 și 60"})})
            (assoc :string {:error/fn (fn [{:keys [schema value]} _]
                                        (let [{:keys [min max]} (m/properties schema)]
                                          (cond
                                            (not (string? value)) "Cif introdus trebuie să fie de tip string"
                                            ;;(and min (= min max)) (str "should be " min " characters")
                                            (and min max) (str "CIF introdus trebuie să aibe un număr cuprins intre 1 si 9 caractere. Valoarea introdusă conține " (count value) " caractere")
                                            min (str "CIF introdus trebuie să aibe un număr cuprins intre 1 si 9 caractere. Valoarea introdusă conține " (count value) " caractere")
                                            max (str "CIF introdus trebuie să aibe un număr cuprins intre 1 si 9 caractere. Valoarea introdusă conține " (count value) " caractere"))))}))})))

^:rct/test
(comment
  (validate-input-data 0 "1")
  ;; => {:zile ["Numărul de zile trebuie să fie cuprins între 1 și 60"]}
  (validate-input-data 0 nil)
;; => {:zile ["Numărul de zile trebuie să fie cuprins între 1 și 60"], :cif ["Cif introdus trebuie să fie de tip string"]}
  (validate-input-data -1 "asd")
  ;; => {:zile ["Numărul de zile trebuie să fie cuprins între 1 și 60"]}
  (validate-input-data nil "asd")
    ;; => {:zile ["Trebuie să introduci un număr cuprins între 1 și 60"]}
  (validate-input-data 1 "1234567890")
  ;; => {:cif ["CIF introdus trebuie să aibe un număr cuprins intre 1 si 9 caractere. Valoarea introdusă conține 10 caractere"]}
  0)

(comment
  (require '[com.mjdowney.rich-comment-tests :refer [run-ns-tests!]])
  (run-ns-tests! *ns*)
  )