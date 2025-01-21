(ns user
  (:require
   [efactura-mea.config :refer [conf]]
   [efactura-mea.db.ds :refer [ds]]
   [efactura-mea.web.api :as api]
   [efactura-mea.systems :as sys :refer [web-app cli-app]]
   [com.mjdowney.rich-comment-tests.test-runner :as test-runner]
   [portal.api :as p]
   [mount.core :as mount :refer [defstate]]
   [clojure.tools.logging :as log]))

(comment

  (do
  ;; https://github.com/djblue/portal?ta=b=readme-ov-file#api
    (def p (p/open {:launcher :vs-code}))
  ; Add portal as a tap> target
    (add-tap #'p/submit)
    (tap> {:message "Test"}))

  ;; run tests
  (test-runner/run-tests-in-file-tree! :dirs #{"src"})

  (mount/start web-app)

  (mount/start cli-app)
  (mount/stop)

  ;; reset 
  (do
    (mount/stop)
    (mount/start)
    (log/info "Start"))
  
  (mount/running-states)

  conf

  (do
    (api/pornire-serviciu-descarcare-automata ds conf))

  0
  )