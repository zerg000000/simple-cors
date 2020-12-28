(ns simple-cors.core
  (:require
    [clojure.string :as str])
  (:import
    (clojure.lang
      Associative
      ILookup)))


(def default-preflight-forbidden-response {:status 403})


(def default-preflight-ok-response {:status 200})

;;; Credit Metosin
;;; https://github.com/metosin/reitit/blob/0bcfda755f139d14cf4eff37e2b294f573215213/modules/reitit-core/src/reitit/impl.cljc#L136
(defn fast-assoc
  "Like assoc but only takes one kv pair. Slightly faster."
  {:inline
   (fn [a k v]
     `(.assoc ~(with-meta a {:tag 'clojure.lang.Associative}) ~k ~v))}
  [^Associative a k v]
  (.assoc a k v))

;;; Credit Metosin
;;; https://github.com/metosin/compojure-api/blob/master/src/compojure/api/common.clj#L46
(defn fast-merge
  [m1 m2]
  (reduce-kv
    (fn [acc k v]
      (fast-assoc acc k v))
    (or m1 {})
    m2))


(defmacro val-at
  "Inline .valAt"
  [m k]
  `(.valAt  ~(with-meta m {:tag 'clojure.lang.ILookup}) ~k))


(defprotocol CORSOriginHandler
  (preflight-response [this request-origin] "Success preflight response for the origin")
  (add-headers-to-response [this response request-origin] "Add CORS headers to response for the origin"))


(deftype CORSOriginStaticHandler [preflight-response-template response-headers origin]
         CORSOriginHandler
         (preflight-response [_ _]
           preflight-response-template)
         (add-headers-to-response [_ response _]
           (update response :headers fast-merge
                   response-headers)))


(deftype CORSOriginAnyOriginHandler [preflight-response-template response-headers]
         CORSOriginHandler
         (preflight-response [_ _]
           preflight-response-template)
         (add-headers-to-response [_ response _]
           (update response :headers fast-merge
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
         (preflight-response [this request-origin]
           (update preflight-response-template :headers assoc
                   "access-control-allow-origin" request-origin
                   "vary" request-origin))
         (add-headers-to-response [this response request-origin]
           (update response :headers fast-merge
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
           (if-let [handler (some #(val-at % request-origin) lookups)]
             handler
             any-origin-handler))
         (valAt [_ request-origin _]
           (if-let [handler (some #(val-at % request-origin) lookups)]
             handler
             any-origin-handler)))


(defn ^boolean preflight-request?
  "Check if the ring request is a valid preflight request"
  [req]
  (and (identical? :options (:request-method req))
       (contains? (:headers req) "origin")
       (contains? (:headers req) "access-control-request-method")))


(defmacro get-origin
  "Get origin header from standard Ring request (inline).
   Assume :headers will always be an IPersistentMap"
  [req]
  `(-> ~req
       (:headers)
       (val-at "origin")))


(defn make-cors-preflight-handler
  "Create a ring handler fn that only handle preflight request"
  [cors forbidden-response ok-response]
  (fn cors-preflight-handler
    ([req]
     (if (preflight-request? req)
       (let [request-origin (get-origin req)
             cors-handler (val-at cors request-origin)]
         (if cors-handler
           (preflight-response cors-handler request-origin)
           (if request-origin
             forbidden-response
             ok-response)))
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
                                               (str/join ", "))}
    (:max-age config)
    (assoc "access-control-max-age" (str (:max-age config)))
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
  [config preflight-ok-response]
  (->> (for [origin (:origins config)]
         [origin (->CORSOriginStaticHandler (update preflight-ok-response :headers
                                                    fast-merge (preflight-response-headers config origin))
                                            (response-headers config origin)
                                            origin)])
       (into {})))


(defn compile-cors-any-origin-config
  "Compile cors for any origin"
  [config preflight-ok-response]
  ; https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS#Credentialed_requests_and_wildcards
  (->CORSOriginAnyOriginHandler (update preflight-ok-response :headers
                                        fast-merge (preflight-response-headers config "*"))
                                (dissoc (response-headers config "*")
                                        "access-control-allow-credentials")))


(defn compile-cors-fn-origin-config
  "Slow but flexible cors"
  [config preflight-ok-response]
  (->CORSOriginFnHandler (update preflight-ok-response :headers
                                 fast-merge (preflight-response-headers config nil))
                         (response-headers config nil)
                         (:origins config)))


(defn compile-cors-config
  [{:keys [origins] :as config} preflight-ok-response]
  (cond
    (= "*" origins) (compile-cors-any-origin-config config preflight-ok-response)
    (or (sequential? origins)
        (set? origins)) (compile-cors-static-config config preflight-ok-response)
    (fn? origins) (compile-cors-fn-origin-config config  preflight-ok-response)))


(defn compile-combined-cors-configs
  "Combine multiple ILookup, lookup time will grow linearly"
  ([configs preflight-ok-response] (compile-combined-cors-configs configs nil  preflight-ok-response))
  ([configs any-origin-config preflight-ok-response]
   (->CombinedCORSHandlerLookup (map #(compile-cors-config % preflight-ok-response) configs)
                                (when any-origin-config
                                  (compile-cors-config any-origin-config preflight-ok-response)))))


(defn compile-cors
  "Return {:cors ... :preflight-handler} cors must implemented ILookup for lookup handler by origin.
   preflight-handler is a normal ring handler both 1-arity, 3-arity would be provided."
  [{:keys [cors-config
           preflight-forbidden-response
           preflight-ok-response]
    :or {preflight-forbidden-response default-preflight-forbidden-response
         preflight-ok-response default-preflight-ok-response}}]
  (let [cors (cond
               (map? cors-config) (compile-cors-config cors-config  preflight-ok-response)
               (and (vector? cors-config) (= (count cors-config) 1))
               (compile-cors-config (first cors-config)  preflight-ok-response)
               (vector? cors-config)
               (let [any-origin (first (filter #(= "*" (:origins %)) cors-config))
                     others (filter #(not= "*" (:origins %)) cors-config)]
                 (compile-combined-cors-configs others any-origin preflight-ok-response))
               :else (throw (ex-info "not a valid cors config" {})))]
    {:cors cors
     :preflight-handler (make-cors-preflight-handler cors
                                                     preflight-forbidden-response
                                                     preflight-ok-response)}))
