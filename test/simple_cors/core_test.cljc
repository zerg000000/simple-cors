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


(deftest test-compile-cors-configs
  (let [config {:allowed-request-methods [:get]
                :allowed-request-headers ["Authorization" "Content-Type"]
                :origins ["https://yahoo.com"
                          "https://google.com"]
                :max-age 300}
        configs (cors/compile-cors-configs [config])]
    (is (= (keys configs)
           (:origins config))
        "should compile to origin/config map")))

(deftest test-ring-middleware
  (let [config {:allowed-request-methods [:post :get]
                :allowed-request-headers ["Authorization" "Content-Type"]
                :origins ["https://yahoo.com"
                          "https://google.com"]
                :max-age 300}
        configs (cors/compile-cors-configs [config])
        handler (fn [req] {:status 200 :body "OK"})
        app (mdw/wrap-cors handler {:cors-configs configs})]

    (is (= {:headers {"access-control-allow-headers" "Authorization, Content-Type",
                      "access-control-allow-methods" "POST, GET",
                      "access-control-allow-origin" "https://yahoo.com",
                      "access-control-max-age" 300,
                      "vary" "https://yahoo.com"},
            :status 200}
           (app {:request-method :options
                 :headers {"origin" "https://yahoo.com"
                           "access-control-request-headers" "authorization,content-type"
                           "access-control-request-method" "POST"}}))
        "should return preflight response")

    (is (= {:headers {"access-control-allow-origin" "https://yahoo.com",
                      "vary" "https://yahoo.com"},
            :body "OK"
            :status 200}
           (app {:request-method :post
                 :headers {"origin" "https://yahoo.com"
                           "access-control-request-headers" "authorization,content-type"
                           "access-control-request-method" "POST"}}))
        "should return preflight response")))

