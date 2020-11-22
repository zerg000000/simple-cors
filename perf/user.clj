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

(def encors-config
  {:allowed-origins #{"https://google.com"
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
   :ignore-failures? true})

(def simple-cors-app
  (simple-cors/wrap handler simple-cors-config))

(def simple-cors-fn-app
  (simple-cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization" "Content-Type"]
                                           :origins #{"https://google.com"
                                                      "https://1.google.com"
                                                      "https://2.google.com"
                                                      "https://3.google.com"
                                                      "https://4.google.com"
                                                      "https://5.google.com"
                                                      "https://6.google.com"}}}))

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
  (com.unbounce.encors.aleph/wrap-cors aleph-handler encors-config))

(def encors-app
  (encors/wrap-cors handler encors-config))

(def sieppari-app
  [(simple-cors-interceptor/cors-interceptor simple-cors-config)
   aleph-handler])

(comment
  ; async tests
  (cc/quick-bench (let [p (d/deferred)]
                    (simple-cors-app request #(d/success! p %) #(d/error! p %))
                    @p))
  ; Execution time mean : 535.353161 ns
  (cc/quick-bench (let [p (d/deferred)]
                    (ring-cors-app request #(d/success! p %) #(d/error! p %))
                    @p))
  ; Execution time mean : 7.326039 µs
  (cc/quick-bench @(simple-cors-aleph-app request))
  ; Execution time mean : 529.702230 ns
  (cc/quick-bench @(encors-aleph-app request))
  ; Execution time mean : 1.660398 µs
  (cc/quick-bench (sieppari/execute sieppari-app request))
  ; Execution time mean : 23.357015 µs
  (comment))

(comment
  (simple-cors-app request)
  (cc/quick-bench (simple-cors-app preflight-request))
  (cc/quick-bench (simple-cors-app request))
  ;Evaluation count : 2937684 in 6 samples of 489614 calls.
  ;             Execution time mean : 196.854083 ns
  ;    Execution time std-deviation : 0.236949 ns
  ;   Execution time lower quantile : 196.594299 ns ( 2.5%)
  ;   Execution time upper quantile : 197.069139 ns (97.5%)
  ;                   Overhead used : 7.545142 ns

  ;Evaluation count : 2336154 in 6 samples of 389359 calls.
  ;             Execution time mean : 285.621191 ns
  ;    Execution time std-deviation : 42.414432 ns
  ;   Execution time lower quantile : 251.454378 ns ( 2.5%)
  ;   Execution time upper quantile : 336.427211 ns (97.5%)
  ;                   Overhead used : 7.545142 ns

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

  (simple-cors-fn-app request)
  (cc/quick-bench (simple-cors-fn-app preflight-request))
  (cc/quick-bench (simple-cors-fn-app request))
  ; with set
  ; Execution time mean : 248.514481 ns
  ; Execution time mean : 350.506847 ns

  (cc/quick-bench (simple-cors-any-app preflight-request))
  (cc/quick-bench (simple-cors-any-app request))
  ; Execution time mean : 250.614513 ns
  ; Execution time mean : 306.921599 ns
  (doc cc/quick-bench))