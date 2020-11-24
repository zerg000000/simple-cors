(ns simple-cors.core-test
  (:require
    [clojure.spec.alpha :as s]
    [clojure.string :as str]
    [clojure.test :refer :all]
    [expound.alpha :as expound]
    [simple-cors.core :as cors]
    [simple-cors.specs]))


(set! s/*explain-out* (expound/custom-printer {}))


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
           (cors/preflight-request? {:request-method :options
                                     :headers {"access-control-request-headers" "authorization,content-type"
                                               "access-control-request-method" "POST"}}))
        "false since don't have origin")))


(deftest test-preflight-responses-config
  (let [{:keys [cors preflight-handler]} (cors/compile-cors {:cors-config {:allowed-request-methods [:get]
                                                                           :allowed-request-headers ["Authorization" "Content-Type"]
                                                                           :origins ["https://yahoo.com"
                                                                                     "https://google.com"]
                                                                           :max-age 300}
                                                             :preflight-ok-response {:status 204 :body "Fun"}
                                                             :preflight-forbidden-response {:status 401 :body "Not Fun"}})]
    (is (= (preflight-handler {:request-method :options
                               :headers {"access-control-request-headers" "authorization,content-type"
                                         "access-control-request-method" "POST"
                                         "origin" "https://google.com"}})
           {:status 204 :body "Fun"
            :headers {"access-control-allow-headers" "Authorization, Content-Type",
                      "access-control-allow-methods" "GET",
                      "access-control-allow-origin" "https://google.com",
                      "access-control-max-age" 300,
                      "vary" "https://google.com"}})
        "should return custom status code ok response")
    (is (= (preflight-handler {:request-method :options
                               :headers {"access-control-request-headers" "authorization,content-type"
                                         "access-control-request-method" "POST"}})
           {:status 401 :body "Not Fun"})
        "should return custom status code forbidden response")))


(deftest test-compile-cors-static-config
  (let [config {:allowed-request-methods [:get]
                :allowed-request-headers ["Authorization" "Content-Type"]
                :origins ["https://yahoo.com"
                          "https://google.com"]
                :max-age 300}
        cors (cors/compile-cors-config config cors/default-preflight-ok-response)]
    (is (get cors "https://yahoo.com")
        "should get handler for origins")
    (is (get cors "https://google.com")
        "should get handler for origins")
    (is (nil? (get cors "https://baidu.cn"))
        "should not get handler for origin that not in list")))


(deftest test-compile-cors-any-origin-config
  (let [config {:allowed-request-methods [:get]
                :allowed-request-headers ["Authorization"]
                :origins "*"
                :max-age 300}
        cors (cors/compile-cors-config config cors/default-preflight-ok-response)]
    (is (get cors "https://yahoo.com")
        "should get handler for all origins")
    (is (get cors "https://google.com")
        "should get handler for all origins")
    (is (nil? (get cors nil))
        "should not get handler for no origin")
    (let [h (get cors "https://anyway.co")]
      (is (= {:headers {"access-control-allow-headers" "Authorization"
                        "access-control-allow-methods" "GET"
                        "access-control-allow-origin" "*"
                        "access-control-max-age" 300}
              :status 200}
             (cors/preflight-response h "https://anyway.co"))
          "should allow preflight for all origins")
      (is (= {:headers {"access-control-allow-origin" "*"}
              :status 200}
             (cors/add-headers-to-response h {:status 200} "https://anyway.co"))
          "should add cors headers for all origins"))))


(deftest test-compile-cors-fn-config
  (let [config {:allowed-request-methods [:get]
                :allowed-request-headers ["Authorization"]
                :origins                 (fn [origin]
                                           (and (str/starts-with? origin "https://")
                                                (str/ends-with? origin ".com")))
                :max-age                 300}
        cors (cors/compile-cors-config config cors/default-preflight-ok-response)]
    (is (get cors "https://yahoo.com")
        "should get handler for all https://*.com")
    (is (get cors "https://google.com")
        "should get handler for all https://*.com")
    (is (nil? (get cors nil))
        "should not get handler for no origin")
    (is (nil? (get cors "https://anyway.co"))
        "should not handle not matched origin")))
