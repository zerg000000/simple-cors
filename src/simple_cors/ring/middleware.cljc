(ns simple-cors.ring.middleware
  (:require
    [simple-cors.core :as cors]))


(defn wrap
  [handler full-config]
  (let [{:keys [cors preflight-handler]} (cors/compile-cors full-config)]
    (fn cors-middleware
      ([req]
       (if (identical? :options (:request-method req))
         (preflight-handler req)
         (if-let [cors-handler (get cors (cors/get-origin req))]
           (cors/add-headers-to-response cors-handler (handler req) (cors/get-origin req))
           (handler req))))
      ([req respond raise]
       (if (identical? :options (:request-method req))
         (respond (preflight-handler req))
         (if-let [cors-handler (get cors (cors/get-origin req))]
           (handler req #(respond (cors/add-headers-to-response cors-handler % (cors/get-origin req)))
                    raise)
           (handler req respond raise)))))))
