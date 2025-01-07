(ns user
  (:require
   [efactura-mea.config :refer [conf]]
   [efactura-mea.db.ds :refer [ds]]
   [efactura-mea.web.api :as api]
   [efactura-mea.http-server]
   [com.mjdowney.rich-comment-tests.test-runner :as test-runner]
   [mount.core :as mount]))

;; https://github.com/djblue/portal?tab=readme-ov-file#api
;; (def p (p/open {:launcher :vs-code}))  ; jvm / node only
;; (add-tap #'p/submit) ; Add portal as a tap> target

(comment
  
  ;; run tests
  (test-runner/run-tests-in-file-tree! :dirs #{"src"})

  (mount/start)

  (do 
    (api/pornire-serviciu-descarcare-automata ds conf))

  (mount/stop)

  (mount/running-states)

  0)