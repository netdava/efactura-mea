(ns user
  (:require ;;[hyperfiddle.rcf]
            [portal.api :as p]))

;; (hyperfiddle.rcf/enable!)

;; https://github.com/djblue/portal?tab=readme-ov-file#api
(def p (p/open {:launcher :vs-code}))  ; jvm / node only
(add-tap #'p/submit) ; Add portal as a tap> target