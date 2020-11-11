(ns simple-cors.sieppari
  (:require [sieppari.context :as context]))

(defn cors-interceptor
  [cors-configs]
  (let [enter-fn (fn short-cricuited-cors-enter
                   [ctx]
                   (let [request-origin (get-in ctx [:request :headers "origin"])
                         config (first (filter (cors-config-for-origin? request-origin) cors-configs))]
                     (cond
                       (and request-origin config)
                       (assoc ctx :cors/config config :cors/request-origin request-origin)
                       request-origin
                       (context/terminate forbidden-response)
                       :else
                       ctx)))]
    {:name ::cors
     :enter enter-fn
     :leave (fn cors-leave
              [ctx]
              (let [request-origin (:cors/request-origin ctx)
                    config (:cors/config ctx)]
                (cond-> ctx
                        config (update :response (config :cors-response-fn) request-origin))))}))
