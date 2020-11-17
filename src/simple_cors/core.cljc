(ns simple-cors.core
  (:require
    [clojure.string :as str]))


(def default-preflight-forbidden-response {:status 403})


(def default-preflight-ok-response {:status 200})


(defprotocol CORSOriginHandler
  (origin [this] "Origin that this handler can handle")
  (preflight-response [this request-origin] "Success preflight response for the origin")
  (add-headers-to-response [this response request-origin] "Add CORS headers to response for the origin"))


(deftype CORSOriginStaticHandler [preflight-response-template response-headers origin]
  CORSOriginHandler
  (origin [this] origin)
  (preflight-response [this _]
    preflight-response-template)
  (add-headers-to-response [this response _]
    (update response :headers merge
            response-headers)))


(defprotocol CORSOriginHandlerLookup
  (get-handler [this request-origin]))


(extend-protocol CORSOriginHandlerLookup
  clojure.lang.APersistentMap
  (get-handler [this request-origin] (get this request-origin)))


(deftype CORSOriginAnyOriginHandler [preflight-response-template response-headers]
  CORSOriginHandler
  (origin [this] "*")
  (preflight-response [this _]
    preflight-response-template)
  (add-headers-to-response [this response _]
    (update response :headers merge
            response-headers))
  CORSOriginHandlerLookup
  (get-handler [this request-origin]
    (when-not (nil? request-origin)
      this)))

(deftype CORSOriginFnHandler [preflight-response-template response-headers allowed-origin?]
  CORSOriginHandler
  (origin [this] "?")
  (preflight-response [this request-origin]
    (update preflight-response-template :headers assoc
            "access-control-allow-origin" request-origin
            "vary" request-origin))
  (add-headers-to-response [this response request-origin]
    (update response :headers merge
            (assoc response-headers
              "access-control-allow-origin" request-origin
              "vary" request-origin)))
  CORSOriginHandlerLookup
  (get-handler [this request-origin]
    (when (and request-origin (allowed-origin? request-origin))
      this)))

(defn preflight-request?
  "Check if the ring request is a valid preflight request"
  [req]
  (and (identical? :options (:request-method req))
       (contains? (:headers req) "origin")
       (contains? (:headers req) "access-control-request-method")))


(defn get-origin
  "Get origin header from standard Ring request"
  [req]
  (-> req :headers (get "origin")))


(defn make-cors-preflight-handler
  "Create a ring handler fn that only handle preflight request"
  [cors forbidden-response ok-response]
  (fn cors-preflight-handler
    ([req]
     (if (preflight-request? req)
       (let [request-origin (get-origin req)
             cors-handler (get-handler cors request-origin)]
         (cond
           cors-handler
           (preflight-response cors-handler request-origin)
           request-origin
           forbidden-response
           :else
           ok-response))
       forbidden-response))
    ([req respond raise]
     (respond (cors-preflight-handler req)))))


(defn preflight-response-headers
  "Generate preflight response headers from config"
  [config origin]
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
          (merge (:preflight-response-headers config))
          origin
          (assoc "access-control-allow-origin" origin)
          (not= origin "*")
          (assoc "vary" origin)))


(defn response-headers
  "Generate CORS headers for a valid request"
  [config origin]
  (cond-> {}
          (true? (:allow-credentials? config))
          (assoc "access-control-allow-credentials" "true")
          (seq (:exposed-headers config))
          (assoc "access-control-expose-headers" (:exposed-headers config))
          origin
          (assoc "access-control-allow-origin" origin)
          (not= origin "*")
          (assoc "vary" origin)))


(defn compile-cors-static-config
  "Compile CORS config to map[string,CORSOriginHandler]"
  [config]
  (->> (for [origin (:origins config)]
         [origin (->CORSOriginStaticHandler {:status 200
                                             :headers (preflight-response-headers config origin)}
                                            (response-headers config origin)
                                            origin)])
       (into {})))


(defn compile-cors-any-origin-config
  "Compile cors for any origin"
  [config]
  ; https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#Credentialed_requests_and_wildcards
  (->CORSOriginAnyOriginHandler {:status 200
                                 :headers (preflight-response-headers config "*")}
                                (dissoc (response-headers config "*")
                                        "access-control-allow-credentials")))


(defn compile-cors-fn-origin-config
  "Slow but flexible cors"
  [config]
  (->CORSOriginFnHandler {:status 200
                          :headers (preflight-response-headers config nil)}
                         (response-headers config nil)
                         (:origins config)))


(defn compile-cors-config
  [{:keys [origins] :as config}]
  (cond
    (= "*" origins) (compile-cors-any-origin-config config)
    (or (sequential? origins)
        (set? origins)) (compile-cors-static-config config)
    (fn? origins) (compile-cors-fn-origin-config config)))
