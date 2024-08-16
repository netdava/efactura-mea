(ns efactura-mea.job-scheduler
  (:import (java.util.concurrent Executors ExecutorService
                                 ScheduledThreadPoolExecutor)))

(def descarcari-pool ^ExecutorService (Executors/newFixedThreadPool 4))

(def sched-pool (ScheduledThreadPoolExecutor. 4))

