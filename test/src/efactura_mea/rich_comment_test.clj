(ns efactura-mea.rich-comment-test
  (:require [clojure.test :refer :all]
            [com.mjdowney.rich-comment-tests.test-runner :as test-runner]))

(deftest rct-tests
  (test-runner/run-tests-in-file-tree! :dirs #{"src"}))