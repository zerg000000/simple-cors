(ns simple-cors.ring.middleware
  (:require
    [simple-cors.core :as cors]))


(defn wrap
  [handler {:keys [cors-config
                   preflight-forbidden-response
                   preflight-ok-response]
            :or {preflight-forbidden-response cors/default-preflight-forbidden-response
                 preflight-ok-response cors/default-preflight-ok-response}}]
  (let [cors (cors/compile-cors-config cors-config)
        preflight-handler (cors/make-cors-preflight-handler cors preflight-forbidden-response preflight-ok-response)]
    (fn cors-middleware
      ([req]
       (if (identical? :options (:request-method req))
         (preflight-handler req)
         (if-let [cors-handler (cors/get-handler cors (cors/get-origin req))]
           (cors/add-headers-to-response cors-handler (handler req) (cors/get-origin req))
           (handler req))))
      ([req respond raise]
       (if (identical? :options (:request-method req))
         (respond (preflight-handler req))
         (if-let [cors-handler (cors/get-handler cors (cors/get-origin req))]
           (handler req #(respond (cors/add-headers-to-response cors-handler % (cors/get-origin req)))
                    raise)
           (handler req respond raise)))))))
