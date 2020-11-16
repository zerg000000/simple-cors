(ns simple-cors.reitit.interceptor
  (:require [simple-cors.core :as cors]))

(defn cors-interceptor
  "Create a Reitit interceptor.
   Since reitit using a separate handler to handle OPTIONS request,
   all preflight handling logic will not exists in this interceptor"
  [{:keys [cors-config]}]
  (let [cors (cors/compile-cors-config cors-config)]
    {:name ::cors
     :leave (fn cors-leave
              [ctx]
              (let [request-origin (-> ctx :request cors/get-origin)
                    cors-handler (cors/get-handler cors request-origin)]
                (cond-> ctx
                        cors-handler (update :response #(cors/add-headers-to-response cors-handler % request-origin)))))}))

(defn make-default-options-endpoint
  "Create a handler for reitit's :reitit.http/default-options-endpoint.
   Accept the same config map of cors-interceptor

   (http/router routes
    {:reitit.http/default-options-endpoint (cors/default-options-endpoint config)})
  "
  [{:keys [cors-config
           preflight-forbidden-response
           preflight-ok-response]
    :or {preflight-forbidden-response cors/default-preflight-forbidden-response
         preflight-ok-response cors/default-preflight-ok-response}}]
  (let [cors (cors/compile-cors-config cors-config)]
    (cors/make-cors-preflight-handler cors
                                      preflight-forbidden-response
                                      preflight-ok-response)))
