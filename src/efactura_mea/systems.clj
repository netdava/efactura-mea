(ns efactura-mea.systems
  "Systeme de stări pre-definite."
  (:require
   [efactura-mea.config]
   [efactura-mea.http-server]
   [efactura-mea.job-scheduler]
   [efactura-mea.db.ds]
   [mount-up.core :as mu]
   [mount.core :refer [only]]))

;; Log mount service up / down
(mu/on-upndown :info mu/log :before)

(def cli-app
  "System de stări pentru aplicația cli"
  (only #{#'efactura-mea.config/conf
          #'efactura-mea.db.ds/ds
          #'efactura-mea.job-scheduler/job-scheduler-pool
          #'efactura-mea.job-scheduler/executor-service}))

(def web-app
  "System de stări pentru aplicația web"
  (only #{#'efactura-mea.config/conf
          #'efactura-mea.http-server/server
          #'efactura-mea.db.ds/ds
          #'efactura-mea.job-scheduler/job-scheduler-pool
          #'efactura-mea.job-scheduler/executor-service}))