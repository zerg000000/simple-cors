(ns simple-cors.reitit.interceptor
  (:require [simple-cors.core :as cors]))

(defn cors-interceptor
  "This interceptor is intended to be used in reitit.
   Since reitit using a separate handler to handle {:request-method :options},
   all preflight handling logic will not exists in this interceptor"
  [{:keys [cors-config]}]
  (let [cors (cors/compile-cors-config cors-config)]
    {:name ::cors
     :leave (fn cors-leave
              [ctx]
              (let [request-origin (-> ctx :request :headers (get "origin"))
                    cors-handler (get cors request-origin)]
                (cond-> ctx
                        cors-handler (update :response #(cors/add-headers-to-response cors-handler %)))))}))

(defn make-default-options-endpoint
  [{:keys [cors-config
           preflight-forbidden-response
           preflight-ok-response]
    :or {preflight-forbidden-response cors/default-preflight-forbidden-response
         preflight-ok-response cors/default-preflight-ok-response}}]
  (let [cors (cors/compile-cors-config cors-config)]
    (cors/make-cors-preflight-handler cors
                                      preflight-forbidden-response
                                      preflight-ok-response)))
