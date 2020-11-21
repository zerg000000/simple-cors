(ns simple-cors.core
  (:require
    [clojure.string :as str])
  (:import [clojure.lang ILookup]))


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


(deftype CORSOriginAnyOriginHandler [preflight-response-template response-headers]
         CORSOriginHandler
         (origin [this] "*")
         (preflight-response [this _]
           preflight-response-template)
         (add-headers-to-response [this response _]
           (update response :headers merge
                   response-headers))
         ILookup
         (valAt [this request-origin]
           (when-not (nil? request-origin)
             this))
         (valAt [this request-origin default-handler]
           (if-not (nil? request-origin)
             this
             default-handler)))


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
         ILookup
         (valAt [this request-origin]
           (when (and request-origin (allowed-origin? request-origin))
             this))
         (valAt [this request-origin default-handler]
           (if (and request-origin (allowed-origin? request-origin))
             this
             default-handler)))


(deftype CombinedCORSHandlerLookup [lookups any-origin-handler]
  ILookup
  (valAt [_ request-origin]
    (if-let [handler (some #(get % request-origin) lookups)]
      handler
      any-origin-handler))
  (valAt [_ request-origin _]
    (if-let [handler (some #(get % request-origin) lookups)]
      handler
      any-origin-handler)))


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
             cors-handler (get cors request-origin)]
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


(defn compile-combined-cors-configs
  "Combine multiple CORSHandlerLookup, lookup time will grow linearly"
  ([configs] (compile-combined-cors-configs configs nil))
  ([configs any-origin-config]
   (->CombinedCORSHandlerLookup (map compile-cors-config configs)
                                (when any-origin-config
                                  (compile-cors-config any-origin-config)))))

(defn compile-cors [{:keys [cors-config
                            preflight-forbidden-response
                            preflight-ok-response]
                     :or {preflight-forbidden-response default-preflight-forbidden-response
                          preflight-ok-response default-preflight-ok-response}}]
  (let [cors (cond
               (map? cors-config) (compile-cors-config cors-config)
               (and (vector? cors-config) (= (count cors-config) 1))
               (compile-cors-config (first cors-config))
               (vector? cors-config)
               (let [any-origin (first (filter #(= "*" (:origins %)) cors-config))
                     others (filter #(not= "*" (:origins %)) cors-config)]
                 (compile-combined-cors-configs others any-origin))
               :else (throw (ex-info "not a valid cors config" {})))]
    {:cors cors
     :preflight-handler (make-cors-preflight-handler cors
                                                     preflight-forbidden-response
                                                     preflight-ok-response)}))