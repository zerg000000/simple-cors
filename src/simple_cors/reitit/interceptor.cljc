(ns simple-cors.reitit.interceptor
  (:require [simple-cors.core :as cors]))

(defn cors-interceptor
  "This interceptor is intended to be used in reitit.
   Since reitit handle {:request-method :options} using a separate handler,
   all preflight handling logic will not exists in this interceptor"
  [{:keys [cors-config]}]
  (let [cors (cors/compile-cors-config cors-config)]
    {:name ::cors
     :enter (fn cors-enter
              [ctx]
              (let [request-origin (-> ctx :request :headers (get "origin"))
                    cors-handler (get cors request-origin)]
                (spit "value" (str request-origin " " cors-handler))
                (cond-> ctx
                  (and request-origin cors-handler)
                  (assoc :cors/handler cors-handler))))
     :leave (fn cors-leave
              [ctx]
              (let [cors-handler (:cors/handler ctx)]
                (cond-> ctx
                        cors-handler (update :response #(cors/add-headers-to-response cors-handler %)))))}))
