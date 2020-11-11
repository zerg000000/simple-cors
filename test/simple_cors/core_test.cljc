(ns simple-cors.core-test
  (:require [clojure.test :refer :all]
            [simple-cors.core :as cors]))

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
