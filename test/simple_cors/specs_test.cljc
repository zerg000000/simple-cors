(ns simple-cors.specs-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [simple-cors.specs]))

(deftest test-preflight-request?
  (testing "skip preflight check if don't have cors headers"
    (is (= true
           (s/valid? :cors/config {:allowed-request-methods [:get]
                                   :allowed-request-headers ["Authorization" "Content-Type"]
                                   :origins ["https://yahoo.com"
                                             "https://google.com"]
                                   :max-age 300})))))