(ns simple-cors.core
  (:require
    [clojure.string :as str]))


(def default-preflight-forbidden-response {:status 403})


(def default-preflight-ok-response {:status 200})


(defprotocol CORS
  (origin [this])
  (preflight-response [this])
  (add-headers-to-response [this response]))


(deftype CORSHandler [preflight-headers response-headers origin]
  CORS
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
  (and (= :options (:request-method req))
       (contains? (:headers req) "origin")
       (contains? (:headers req) "access-control-request-method")))


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
  [cors forbidden-response ok-response]
  (fn cors-preflight-handler
    [req]
    (if (preflight-request? req)
      (let [request-origin (-> req :headers (get "origin"))
            cors-handler (get cors request-origin)]
        (handle-preflight cors-handler
                          request-origin forbidden-response ok-response))
      forbidden-response)))


(defn preflight-response-headers
  "Generate preflight response headers from config"
  [config]
  (cond-> {"access-control-allow-methods" (->> (:allowed-request-methods config)
                                               (map name)
                                               (map str/upper-case)
                                               (str/join ", "))
           "access-control-allow-headers" (->> (-> (:allowed-request-headers config)
                                                   (set)
                                                   ; https://fetch.spec.whatwg.org/#cors-safelisted-request-header
                                                   (conj "Content-Type" "Accept" "Accept-Language" "Content-Language"))
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
  "Compile CORS config to map[string,CORSHandler]"
  [config]
  (let [preflight-headers (preflight-response-headers config)
        cors-headers (response-headers config)]
    (->> (for [origin (:origins config)]
           [origin (->CORSHandler preflight-headers cors-headers origin)])
         (into {}))))

