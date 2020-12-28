(ns simple-cors.data
  (:require
    [manifold.deferred :as d]))


(def ok-response {:status 200 :body "OK"})


(defn ok-handler
  ([_] ok-response)
  ([_ respond raise] (respond ok-response)))


(defn ok-future-handler
  ([_] ok-response)
  ([_ respond raise] (respond (future ok-response))))


(defn ok-deferred-handler
  ([_] ok-response)
  ([_ respond raise] (respond (d/success-deferred ok-response))))


(def cors-config
  {:allowed-request-methods [:post :get]
   :allowed-request-headers ["Authorization"]
   :origins ["https://yahoo.com"
             "https://google.com"]
   :max-age 300})


(def normal-preflight-request
  {:request-method :options
   :uri "/api"
   :headers {"origin" "https://yahoo.com"
             "access-control-request-headers" "authorization"
             "access-control-request-method" "POST"}})


(def normal-preflight-response
  {:headers {"access-control-allow-headers" "Authorization"
             "access-control-allow-methods" "POST, GET"
             "access-control-allow-origin" "https://yahoo.com"
             "access-control-max-age" "300"
             "vary" "https://yahoo.com"}
   :status 200})


(def normal-cors-request
  {:request-method :post
   :uri "/api"
   :headers {"origin" "https://yahoo.com"
             "access-control-request-headers" "authorization,content-type"
             "access-control-request-method" "POST"}})


(def normal-cors-response
  (merge ok-response
         {:headers {"access-control-allow-origin" "https://yahoo.com"
                    "vary" "https://yahoo.com"}}))


(def normal-non-cors-request
  {:request-method :post
   :headers {}
   :uri "/api"})


(def dirty-non-cors-request
  {:request-method :post
   :uri "/api"
   :headers {"access-control-request-headers" "authorization,content-type"
             "access-control-request-method" "POST"}})


(def normal-non-cors-response ok-response)


(def cross-origin-cors-request
  {:request-method :post
   :uri "/api"
   :headers {"origin" "https://not.exists.in.config"
             "access-control-request-headers" "authorization,content-type"
             "access-control-request-method" "POST"}})


(def cross-origin-preflight-request
  {:request-method :options
   :uri "/api"
   :headers {"origin" "https://not.exists.in.config"
             "access-control-request-headers" "authorization,content-type"
             "access-control-request-method" "POST"}})
