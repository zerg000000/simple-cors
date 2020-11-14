(ns simple-cors.core
  (:require
    [clojure.string :as str]))


(def default-preflight-forbidden-response {:status 403})


(def default-preflight-ok-response {:status 200})


(defprotocol CORSOriginHandler
  (origin [this] "Origin that this handler can handle")
  (preflight-response [this] "Success preflight response for the origin")
  (add-headers-to-response [this response] "Add CORS headers to response for the origin"))


(deftype CORSOriginHandlerImpl [preflight-headers response-headers origin]
  CORSOriginHandler
  (origin [this] origin)
  (preflight-response [this]
    {:headers (assoc preflight-headers
                "access-control-allow-origin" origin
                "vary" origin)
     :status 200})
  (add-headers-to-response [this response]
    (update response :headers merge
            response-headers
            {"access-control-allow-origin" origin
             "vary" origin})))


(defn preflight-request?
  "Check if the ring request is a valid preflight request"
  [req]
  (and (identical? :options (:request-method req))
       (contains? (:headers req) "origin")
       (contains? (:headers req) "access-control-request-method")))


(defn get-origin
  [req]
  (-> req :headers (get "origin")))


(defn handle-preflight
  [cors-handler request-origin forbidden-response ok-response]
  (cond
    cors-handler
    (preflight-response cors-handler)
    request-origin
    forbidden-response
    :else
    ok-response))


(defn make-cors-preflight-handler
  "Create a ring handler fn that only handle preflight request"
  [cors forbidden-response ok-response]
  (fn cors-preflight-handler
    ([req]
     (if (preflight-request? req)
       (let [request-origin (get-origin req)
             cors-handler (get cors request-origin)]
         (handle-preflight cors-handler
                           request-origin forbidden-response ok-response))
       forbidden-response))
    ([req respond raise]
     (respond (cors-preflight-handler req)))))


(defn preflight-response-headers
  "Generate preflight response headers from config"
  [config]
  (cond-> {"access-control-allow-methods" (->> (:allowed-request-methods config)
                                               (map name)
                                               (map str/upper-case)
                                               (str/join ", "))
           "access-control-allow-headers" (->> (:allowed-request-headers config)
                                               (map name)
                                               (str/join ", "))
           "access-control-max-age"       (:max-age config 0)}
          (true? (:allow-credentials? config))
          (assoc "access-control-allow-credentials" "true")
          (seq (:preflight-response-headers config))
          (merge (:preflight-response-headers config))))


(defn response-headers
  "Generate CORS headers for a valid request"
  [config]
  (cond-> {}
          (true? (:allow-credentials? config))
          (assoc "access-control-allow-credentials" "true")
          (seq (:exposed-headers config))
          (assoc "access-control-expose-headers" (:exposed-headers config))))


(defn compile-cors-config
  "Compile CORS config to map[string,CORSOriginHandler]"
  [config]
  (let [preflight-headers (preflight-response-headers config)
        cors-headers (response-headers config)]
    (->> (for [origin (:origins config)]
           [origin (->CORSOriginHandlerImpl preflight-headers cors-headers origin)])
         (into {}))))

