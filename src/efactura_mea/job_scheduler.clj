(ns efactura-mea.job-scheduler
  (:require [mount.core :refer [defstate]])
  (:import (java.util.concurrent Executors ExecutorService
                                 ScheduledThreadPoolExecutor)))

(defstate executor-service 
  "Thread pool for regular service execution."
  :start ^ExecutorService (Executors/newFixedThreadPool 4)
  :stop (.shutdown executor-service))

(defstate job-scheduler-pool
  "Thread pool for scheduled services.
   Schedule periodic running services."
  :start 
  (let [pool (ScheduledThreadPoolExecutor. 4)]
    ;; Putem adăuga o opțiune de configurare.
    ;; ca să facem opțional activarea serviciilor

    pool)
  :stop (.shutdown job-scheduler-pool))
