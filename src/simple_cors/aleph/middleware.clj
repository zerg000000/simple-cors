(ns simple-cors.aleph.middleware
  (:require
    [manifold.deferred :as d]
    [simple-cors.core :as cors])
  (:import [clojure.lang ILookup]))


(defn wrap
  [handler full-config]
  (let [{:keys [cors preflight-handler]} (cors/compile-cors full-config)]
    (fn cors-middleware [req]
      (if (identical? :options (:request-method req))
        (d/success-deferred (preflight-handler req))
        (let [request-origin (cors/get-origin req)]
          (d/chain' (handler req) #(if-let [cors-handler (.valAt ^ILookup cors request-origin)]
                                      (cors/add-headers-to-response cors-handler % request-origin)
                                      %)))))))
