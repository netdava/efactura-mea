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
    [:string {:min 8 :max 8}]]])

(defn validate-input-data [zile-int cif-str]
  (-> listare-mesaje-spec
      (m/explain
       {:zile zile-int :cif cif-str})
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
                                            (and min max) (str "CIF introdus trebuie să aibe un număr fix de 8 caractere. Valoarea introdusă conține " (count value) " caractere")
                                            min (str "CIF introdus trebuie să aibe un număr fix de 8 caractere. Valoarea introdusă conține " (count value) " caractere")
                                            max (str "CIF introdus trebuie să aibe un număr fix de 8 caractere. Valoarea introdusă conține " (count value) " caractere"))))}))})))
