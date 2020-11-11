(ns simple-cors.specs-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [simple-cors.specs]))

(deftest test-preflight-request?
  (testing "skip preflight check if don't have cors headers"
    (is (= nil
           (s/explain-data :cors/config {:allowed-request-methods [:get]
                                         :allowed-request-headers ["Authorization" "Content-Type"]
                                         :origins ["https://yahoo.com"
                                                   "https://google.com"]
                                         :max-age 300}))
        "normal cors")
    (is (= {:clojure.spec.alpha/problems '({:in [:max-age],
                                            :path [:max-age],
                                            :pred clojure.core/pos-int?,
                                            :val -1,
                                            :via [:cors/config :cors.config/max-age]}),
            :clojure.spec.alpha/spec :cors/config,
            :clojure.spec.alpha/value {:allowed-request-headers ["Authorization" "Content-Type"],
                                       :allowed-request-methods [:get],
                                       :max-age -1,
                                       :origins ["https://yahoo.com" "https://google.com"]}}
           (s/explain-data :cors/config {:allowed-request-methods [:get]
                                         :allowed-request-headers ["Authorization" "Content-Type"]
                                         :origins ["https://yahoo.com"
                                                   "https://google.com"]
                                         :max-age -1}))
        "max-age must be greater than zero")))