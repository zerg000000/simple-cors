(ns simple-cors.ring.handler-test
  (:require [clojure.test :refer :all]
            [simple-cors.data :as data]
            [reitit.http :as http]
            [reitit.interceptor.sieppari]
            [simple-cors.reitit.interceptor :as interceptor]
            [simple-cors.ring.middleware :as mdw]
            [simple-cors.core :as cors-core]))

(def ^:dynamic *app* nil)

(defn provide-reitit [f]
  (binding [*app* (http/ring-handler
                    (http/router ["/api" {:get data/ok-handler
                                          :post data/ok-handler
                                          :delete data/ok-handler}]
                                 {:reitit.http/default-options-endpoint (cors-core/make-cors-preflight-handler
                                                                          (cors-core/compile-cors-config data/cors-config)
                                                                          cors-core/default-preflight-forbidden-response
                                                                          cors-core/default-preflight-ok-response)})
                    {:executor reitit.interceptor.sieppari/executor
                     :interceptors [(interceptor/cors-interceptor {:cors-config data/cors-config})]})]
    (f)))

(defn provide-ring [f]
  (binding [*app* (mdw/wrap data/ok-handler {:cors-config data/cors-config})]
    (f)))

(use-fixtures :once (juxt provide-ring provide-reitit))

(deftest test-handler
  (testing "normal browser behaviour"
    (is (= data/normal-preflight-response
           (*app* data/normal-preflight-request))
        "should return preflight response")

    (is (= data/normal-cors-response
           (*app* data/normal-cors-request))
        "should return response with cors headers"))

  (testing "non browser should work as usual"
    (is (= data/normal-non-cors-response
           (*app* data/dirty-non-cors-request))
        "should response without cors headers")

    (is (= data/normal-non-cors-response
           (*app* data/normal-non-cors-request))
        "should response without cors headers"))

  (testing "browser under cross origin attack"
    (is (= data/normal-non-cors-response
           (*app* data/cross-origin-cors-request))
        "should response without cors headers")

    (is (= cors-core/default-preflight-forbidden-response
           (*app* data/cross-origin-preflight-request))
        "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods")

    (is (= cors-core/default-preflight-forbidden-response
           (*app* (update data/cross-origin-preflight-request :headers dissoc "access-control-request-headers")))
        "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods")

    (is (= cors-core/default-preflight-forbidden-response
           (*app* (update data/cross-origin-preflight-request :headers dissoc "origin")))
        "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods")))

(deftest test-ring-middleware-async
  (let [app (mdw/wrap data/ok-handler {:cors-config data/cors-config})]

    (testing "normal browser behaviour"
      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :options
              :headers {"origin" "https://yahoo.com"
                        "access-control-request-headers" "authorization,content-type"
                        "access-control-request-method" "POST"}}
             respond
             raise)
        (is (= {:headers {"access-control-allow-headers" "Authorization, Accept-Language, Content-Language, Content-Type, Accept"
                          "access-control-allow-methods" "POST, GET"
                          "access-control-allow-origin" "https://yahoo.com"
                          "access-control-max-age" 300
                          "vary" "https://yahoo.com"}
                :status 200}
               @p)
            "should return preflight response"))

      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :post
              :headers {"origin" "https://yahoo.com"
                        "access-control-request-headers" "authorization,content-type"
                        "access-control-request-method" "POST"}}
             respond
             raise)
        (is (= {:headers {"access-control-allow-origin" "https://yahoo.com"
                          "vary" "https://yahoo.com"}
                :body "OK"
                :status 200}
               @p)

            "should return response with cors headers")))

    (testing "non browser should work as usual"
      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :post
              :headers {"access-control-request-headers" "authorization,content-type"
                        "access-control-request-method" "POST"}}
             respond
             raise)
        (is (= {:body "OK"
                :status 200}
               @p)
            "should return response without cors headers"))

      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :post} respond raise)
        (is (= {:body "OK"
                :status 200}
               @p)

            "should return response without cors headers")))

    (testing "browser under cross origin attack"
      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :post
              :headers {"origin" "https://not.exists.in.config"
                        "access-control-request-headers" "authorization,content-type"
                        "access-control-request-method" "POST"}}
             respond raise)

        (is (= {:body "OK"
                :status 200}
               @p)
            "should return response without cors headers"))

      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :options
              :headers {"origin" "https://not.exists.in.config"
                        "access-control-request-headers" "authorization,content-type"
                        "access-control-request-method" "POST"}}
             respond raise)
        (is (= {:status 403} @p)
            "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods"))

      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :options
              :headers {"origin" "https://yahoo.com"
                        "access-control-request-headers" "authorization,content-type"}}
             respond raise)
        (is (= {:status 403} @p)
            "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods"))

      (let [p (promise)
            respond #(deliver p %)
            raise #(throw (ex-info "unlikely" {}))]
        (app {:request-method :options
              :headers {"access-control-request-headers" "authorization,content-type"
                        "access-control-request-method" "POST"}}
             respond raise)
        (is (= {:status 403} @p)
            "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods")))))