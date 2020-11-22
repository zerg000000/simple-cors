(ns simple-cors.ring.handler-test
  (:require
    [clojure.test :refer :all]
    [manifold.deferred :as d]
    [reitit.http :as http]
    [reitit.interceptor.sieppari]
    [simple-cors.aleph.middleware :as aleph]
    [simple-cors.core :as cors-core]
    [simple-cors.data :as data]
    [simple-cors.reitit.interceptor :as interceptor]
    [simple-cors.ring.middleware :as ring]))


(def ^:dynamic *app* nil)


(defn provide-reitit
  [f]
  (binding [*app* (http/ring-handler
                    (http/router ["/api" {:get data/ok-handler
                                          :post data/ok-handler
                                          :delete data/ok-handler}]
                                 {:reitit.http/default-options-endpoint (interceptor/make-default-options-endpoint
                                                                          {:cors-config data/cors-config})})
                    {:executor reitit.interceptor.sieppari/executor
                     :interceptors [(interceptor/cors-interceptor {:cors-config data/cors-config})]})]
    (f)))


(defn provide-reitit-future
  [f]
  (binding [*app* (http/ring-handler
                    (http/router ["/api" {:get data/ok-future-handler
                                          :post data/ok-future-handler}]
                                 {:reitit.http/default-options-endpoint (interceptor/make-default-options-endpoint
                                                                          {:cors-config data/cors-config})})
                    {:executor reitit.interceptor.sieppari/executor
                     :interceptors [(interceptor/cors-interceptor {:cors-config data/cors-config})]})]
    (f)))


(defn provide-ring
  [f]
  (binding [*app* (ring/wrap data/ok-handler {:cors-config data/cors-config})]
    (f)))


(defn wrap-aleph-async-ring-adatpor
  [h]
  (fn aleph-async-ring-adatpor
    ([req] (let [resp (h req)]
             (if (d/deferrable? resp)
               @resp
               resp)))
    ([req respond raise]
     (try
       (respond (let [resp (h req)]
                  (if (d/deferrable? resp)
                    @resp
                    resp)))
       (catch Exception ex
         (.printStackTrace ex)
         (raise ex))))))


(defn provide-aleph
  [f]
  (binding [*app* (-> data/ok-deferred-handler
                      (aleph/wrap {:cors-config data/cors-config})
                      (wrap-aleph-async-ring-adatpor))]
    (f)))


(use-fixtures :once (juxt provide-ring provide-reitit provide-reitit-future provide-aleph))


(deftest test-ring-handler
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


(deftest test-ring-handler-async
  (testing "normal browser behaviour"
    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* data/normal-preflight-request
             respond
             raise)
      (is (= data/normal-preflight-response @p)
          "should return preflight response"))

    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* data/normal-cors-request
             respond
             raise)
      (is (= data/normal-cors-response @p)
          "should return response with cors headers")))

  (testing "non browser should work as usual"
    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* data/normal-non-cors-request
             respond
             raise)
      (is (= data/normal-non-cors-response @p)
          "should return response without cors headers"))

    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* data/dirty-non-cors-request respond raise)
      (is (= data/normal-non-cors-response @p)
          "should return response without cors headers")))

  (testing "browser under cross origin attack"
    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* data/cross-origin-cors-request
             respond raise)

      (is (= data/normal-non-cors-response @p)
          "should return response without cors headers"))

    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* data/cross-origin-preflight-request
             respond raise)
      (is (= cors-core/default-preflight-forbidden-response @p)
          "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods"))

    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* (update data/cross-origin-preflight-request :headers dissoc "access-control-request-headers")
             respond raise)
      (is (= cors-core/default-preflight-forbidden-response @p)
          "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods"))

    (let [p (promise)
          respond #(deliver p %)
          raise #(deliver p %)]
      (*app* (update data/cross-origin-preflight-request :headers dissoc "origin")
             respond raise)
      (is (= cors-core/default-preflight-forbidden-response @p)
          "preflight request should at least contain origin, access-control-request-headers, access-control-request-methods"))))
