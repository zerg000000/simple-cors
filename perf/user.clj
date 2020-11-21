(ns user
  (:require [criterium.core :as cc]
            [simple-cors.ring.middleware :as simple-cors]
            [simple-cors.aleph.middleware :as simple-cors-aleph]
            [simple-cors.reitit.interceptor :as simple-cors-interceptor]
            [ring.middleware.cors :as ring-cors]
            [com.unbounce.encors :as encors]
            [com.unbounce.encors.aleph]
            [manifold.deferred :as d]
            [sieppari.async.manifold]
            [sieppari.core :as sieppari]))

(defn handler
  ([req]
   {:status 200
    :body "OK"})
  ([req respond raise]
   (respond {:status 200
             :body "OK"})))

(defn aleph-handler
  [req]
  (d/success-deferred {:status 200
                       :body "OK"}))

(def preflight-request
  {:request-method :options
   :uri "/api"
   :headers {"origin" "https://google.com"
             "access-control-request-headers" "authorization,content-type"
             "access-control-request-method" "POST"}})

(def request
  {:request-method :post
   :uri "/api"
   :headers {"origin" "https://google.com"
             "access-control-request-headers" "authorization,content-type"
             "access-control-request-method" "POST"}})

(def simple-cors-config
  {:cors-config {:allowed-request-methods [:post :get]
                 :allowed-request-headers ["Authorization" "Content-Type"]
                 :origins ["https://google.com"
                           "https://1.google.com"
                           "https://2.google.com"
                           "https://3.google.com"
                           "https://4.google.com"
                           "https://5.google.com"
                           "https://6.google.com"]}})

(def simple-cors-app
  (simple-cors/wrap handler simple-cors-config))

(def simple-cors-fn-app
  (simple-cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization" "Content-Type"]
                                           :origins #{["https://google.com"
                                                       "https://1.google.com"
                                                       "https://2.google.com"
                                                       "https://3.google.com"
                                                       "https://4.google.com"
                                                       "https://5.google.com"
                                                       "https://6.google.com"]}}}))

(def simple-cors-any-app
  (simple-cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization" "Content-Type"]
                                           :origins "*"}}))

(def simple-cors-combined-app
  (simple-cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization" "Content-Type"]
                                           :origins "*"}}))

(def simple-cors-aleph-app
  (simple-cors-aleph/wrap aleph-handler simple-cors-config))

(def ring-cors-app
  (ring-cors/wrap-cors handler
                       :access-control-allow-origin [#"https://google.com"
                                                     #"https://([^.^/])+.google.com"]
                       :access-control-allow-methods [:get :post]))

(def encors-aleph-app
  (com.unbounce.encors.aleph/wrap-cors aleph-handler {:allowed-origins #{"https://google.com"
                                                                         "https://1.google.com"
                                                                         "https://2.google.com"
                                                                         "https://3.google.com"
                                                                         "https://4.google.com"
                                                                         "https://5.google.com"
                                                                         "https://6.google.com"}
                                                      :allowed-methods #{:get :post}
                                                      :request-headers #{"Authorization" "Content-Type"}
                                                      :exposed-headers nil
                                                      :allow-credentials? false
                                                      :origin-varies? true
                                                      :max-age nil
                                                      :require-origin? false
                                                      :ignore-failures? true}))

(def encors-app
  (encors/wrap-cors handler {:allowed-origins #{"https://google.com"
                                                "https://1.google.com"
                                                "https://2.google.com"
                                                "https://3.google.com"
                                                "https://4.google.com"
                                                "https://5.google.com"
                                                "https://6.google.com"}
                             :allowed-methods #{:get :post}
                             :request-headers #{"Authorization" "Content-Type"}
                             :exposed-headers nil
                             :allow-credentials? false
                             :origin-varies? true
                             :max-age nil
                             :require-origin? false
                             :ignore-failures? true}))

(def sieppari-app
  [(simple-cors-interceptor/cors-interceptor {:cors-config {:allowed-request-methods [:post :get]
                                                            :allowed-request-headers ["Authorization" "Content-Type"]
                                                            :origins ["https://google.com"
                                                                      "https://1.google.com"
                                                                      "https://2.google.com"
                                                                      "https://3.google.com"
                                                                      "https://4.google.com"
                                                                      "https://5.google.com"
                                                                      "https://6.google.com"]}})
   aleph-handler])

(comment
  ; async tests
  (cc/quick-bench (let [p (d/deferred)]
                    (simple-cors-app request #(d/success! p %) #(d/error! p %))
                    @p))
  ; Execution time mean : 536.591903 ns
  (cc/quick-bench (let [p (d/deferred)]
                    (ring-cors-app request #(d/success! p %) #(d/error! p %))
                    @p))
  ; Execution time mean : 7.326039 µs
  (cc/quick-bench @(simple-cors-aleph-app request))
  ; Execution time mean : 454.023941 ns
  (cc/quick-bench @(encors-aleph-app request))
  ; Execution time mean : 1.660398 µs
  (cc/quick-bench (sieppari/execute sieppari-app request))
  ; Execution time mean : 23.357015 µs
  (comment))

(comment
  (simple-cors-app request)
  (cc/quick-bench (simple-cors-app preflight-request))
  (cc/quick-bench (simple-cors-app request))
  ;Evaluation count : 245159880 in 60 samples of 4085998 calls.
  ;             Execution time mean : 238.204980 ns
  ;    Execution time std-deviation : 5.898590 ns
  ;   Execution time lower quantile : 232.733948 ns ( 2.5%)
  ;   Execution time upper quantile : 251.402905 ns (97.5%)
  ;                   Overhead used : 7.548822 ns

  ;Evaluation count : 136347540 in 60 samples of 2272459 calls.
  ;             Execution time mean : 430.691792 ns
  ;    Execution time std-deviation : 10.678282 ns
  ;   Execution time lower quantile : 422.111687 ns ( 2.5%)
  ;   Execution time upper quantile : 455.802903 ns (97.5%)
  ;                   Overhead used : 7.548822 ns

  (ring-cors-app request)
  (cc/quick-bench (ring-cors-app preflight-request))
  (cc/quick-bench (ring-cors-app request))
  ;Evaluation count : 4578720 in 60 samples of 76312 calls.
  ;             Execution time mean : 13.342993 µs
  ;    Execution time std-deviation : 499.978395 ns
  ;   Execution time lower quantile : 13.017588 µs ( 2.5%)
  ;   Execution time upper quantile : 14.974899 µs (97.5%)
  ;                   Overhead used : 7.548822 ns

  ;Evaluation count : 8291640 in 60 samples of 138194 calls.
  ;             Execution time mean : 7.251174 µs
  ;    Execution time std-deviation : 236.993086 ns
  ;   Execution time lower quantile : 7.008236 µs ( 2.5%)
  ;   Execution time upper quantile : 7.730234 µs (97.5%)
  ;                   Overhead used : 7.548822 ns

  (encors-app request)
  (cc/quick-bench (encors-app preflight-request))
  (cc/quick-bench (encors-app request))
  ;Evaluation count : 5705220 in 60 samples of 95087 calls.
  ;             Execution time mean : 11.012463 µs
  ;    Execution time std-deviation : 315.370709 ns
  ;   Execution time lower quantile : 10.566757 µs ( 2.5%)
  ;   Execution time upper quantile : 11.750389 µs (97.5%)
  ;                   Overhead used : 7.548822 ns

  ;Evaluation count : 37766640 in 60 samples of 629444 calls.
  ;             Execution time mean : 1.559231 µs
  ;    Execution time std-deviation : 36.172127 ns
  ;   Execution time lower quantile : 1.525029 µs ( 2.5%)
  ;   Execution time upper quantile : 1.641337 µs (97.5%)
  ;                   Overhead used : 7.548822 ns

  (cc/quick-bench (simple-cors-fn-app preflight-request))
  (cc/quick-bench (simple-cors-fn-app request))
  ; with set
  ; Execution time mean : 248.514481 ns
  ; Execution time mean : 56.350691 ns

  (cc/quick-bench (simple-cors-any-app preflight-request))
  (cc/quick-bench (simple-cors-any-app request))
  ; Execution time mean : 305.917533 ns
  ; Execution time mean : 502.012656 ns
  (doc cc/quick-bench))