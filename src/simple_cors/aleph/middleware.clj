(ns simple-cors.aleph.middleware
  (:require [manifold.deferred :as d]
            [simple-cors.core :as cors]))

(defn wrap
  [handler {:keys [cors-config
                   preflight-forbidden-response
                   preflight-ok-response]
            :or {preflight-forbidden-response cors/default-preflight-forbidden-response
                 preflight-ok-response cors/default-preflight-ok-response}}]
  (let [cors (cors/compile-cors-config cors-config)
        preflight-handler (cors/make-cors-preflight-handler cors preflight-forbidden-response preflight-ok-response)]
    (fn [req]
      (let [request-origin (cors/get-origin req)
            cors-handler (cors/get-handler cors request-origin)]
        (if (identical? :options (:request-method req))
          (d/success-deferred (preflight-handler req))
          (d/chain' (handler req)
                    #(if cors-handler
                       (cors/add-headers-to-response cors-handler % request-origin)
                       %)))))))