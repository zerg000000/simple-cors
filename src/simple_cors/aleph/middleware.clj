(ns simple-cors.aleph.middleware
  (:require
    [manifold.deferred :as d]
    [simple-cors.core :as cors]))


(defn wrap
  [handler full-config]
  (let [{:keys [cors preflight-handler]} (cors/compile-cors full-config)]
    (fn cors-middleware [req]
      (let [request-origin (cors/get-origin req)
            cors-handler (get cors request-origin)]
        (if (identical? :options (:request-method req))
          (d/success-deferred (preflight-handler req))
          (d/chain' (handler req)
                    #(if cors-handler
                       (cors/add-headers-to-response cors-handler % request-origin)
                       %)))))))
