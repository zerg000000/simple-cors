(ns simple-cors.core-test
  (:require [clojure.test :refer :all]
            [simple-cors.core :as cors]
            [simple-cors.ring.middleware :as mdw]))

(deftest test-preflight-request?
  (testing "skip preflight check if don't have cors headers"
    (is (= true
           (cors/preflight-request? {:request-method :options
                                     :headers {"access-control-request-headers" "authorization,content-type"
                                               "access-control-request-method" "POST"
                                               "origin" "https://google.com"}}))
        "this is a standard cors preflight request")
    (is (= false
           (cors/preflight-request? {:request-method :options}))
        "false since don't have cors headers")
    (is (= false
           (cors/preflight-request? {:request-method :get
                                     :headers {"access-control-request-headers" "authorization,content-type"
                                               "access-control-request-method" "POST"
                                               "origin" "https://google.com"}}))
        "false since it is not an options request")
    (is (= false
           (cors/preflight-request? {:request-method :options}))
        "false since don't have cors headers")
    (is (= false
           (cors/preflight-request? {:request-method :get
                                     :headers {"access-control-request-headers" "authorization,content-type"
                                               "access-control-request-method" "POST"
                                               "origin" "https://google.com"}}))
        "false since don't have origin")))


(deftest test-compile-cors-config
  (let [config {:allowed-request-methods [:get]
                :allowed-request-headers ["Authorization" "Content-Type"]
                :origins ["https://yahoo.com"
                          "https://google.com"]
                :max-age 300}
        cors (cors/compile-cors-config config)]
    (is (= (keys cors)
           (:origins config))
        "should compile to origin/config map")))
