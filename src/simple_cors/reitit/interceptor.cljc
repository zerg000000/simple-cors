(ns simple-cors.reitit.interceptor
  (:require
    [simple-cors.core :as cors])
  (:import [clojure.lang ILookup]))


(defn cors-interceptor
  "Create a Reitit interceptor.
   Since reitit using a separate handler to handle OPTIONS request,
   all preflight handling logic will not exists in this interceptor"
  [full-config]
  (let [{:keys [cors]} (cors/compile-cors full-config)]
    {:name ::cors
     :leave (fn cors-leave
              [ctx]
              (let [request-origin (-> ctx :request cors/get-origin)
                    cors-handler (.valAt ^ILookup cors request-origin)]
                (cond-> ctx
                  cors-handler (update :response #(cors/add-headers-to-response cors-handler % request-origin)))))}))


(defn make-default-options-endpoint
  "Create a handler for reitit's :reitit.http/default-options-endpoint.
   Accept the same config map of cors-interceptor

   (http/router routes
    {:reitit.http/default-options-endpoint (cors/default-options-endpoint config)})
  "
  [full-config]
  (let [{:keys [preflight-handler]} (cors/compile-cors full-config)]
    preflight-handler))
