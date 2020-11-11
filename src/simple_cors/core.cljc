(ns simple-cors.core
  (:require
    [clojure.string :as str]))


(def default-preflight-forbidden-response {:status 403})

(def default-preflight-ok-response {:status 200})


(defn preflight-request?
  "Check if the ring request is a valid preflight request"
  [req]
  (and (= :options (:request-method req))
       (contains? (:headers req) "origin")
       (contains? (:headers req) "access-control-request-method")))


(defn handle-preflight
  [preflight-response-fn request-origin forbidden-response ok-response]
  (cond
    (and preflight-response-fn request-origin)
    (preflight-response-fn request-origin)
    request-origin
    forbidden-response
    :else
    ok-response))


(defn make-cors-preflight-handler
  [cors-configs forbidden-response ok-response]
  (fn cors-preflight-handler
    [req]
    (if (preflight-request? req)
      (let [request-origin (-> req :headers (get "origin"))
            config (get cors-configs request-origin)]
        (handle-preflight (:preflight-response-fn config)
                          request-origin forbidden-response ok-response))
      forbidden-response)))


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


(defn make-preflight-response-fn [config]
  (let [preflight-headers (preflight-response-headers config)]
    (fn preflight-response-fn [request-origin]
      {:headers (assoc preflight-headers
                  "access-control-allow-origin" request-origin
                  "vary" request-origin)
       :status 200})))


(defn make-cors-response-fn [config]
  (let [response-headers (response-headers config)]
    (fn cors-response-fn [response request-origin]
      (update response :headers merge
              response-headers
              {"access-control-allow-origin" request-origin
               "vary" request-origin}))))


(defn compile-cors-config
  [config]
  {:preflight-response-fn (make-preflight-response-fn config)
   :cors-response-fn (make-cors-response-fn config)})


(defn compile-cors-configs
  [configs]
  (reduce
    (fn [acc config]
      (-> (zipmap (:origins config)
                  (repeat (compile-cors-config config)))
          (merge acc)))
    {}
    configs))

