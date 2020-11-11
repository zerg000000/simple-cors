(ns simple-cors.sieppari
  (:require [sieppari.context :as context]
            [simple-cors.core :as cors]))

(defn cors-interceptor
  [{:keys [cors-configs forbidden-response]}]
  (let [compiled-configs (cors/compile-cors-configs cors-configs)
        enter-fn (fn short-cricuited-cors-enter
                   [ctx]
                   (let [request-origin (-> ctx :request :headers (get "origin"))
                         config (get compiled-configs request-origin)]
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
                    response-fn (-> ctx :cors/config :cors-response-fn)]
                (cond-> ctx
                        response-fn (update :response response-fn request-origin))))}))
