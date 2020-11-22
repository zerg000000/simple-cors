(ns simple-cors.ring.middleware
  (:require
    [simple-cors.core :as cors])
  (:import [clojure.lang ILookup]))


(defn wrap
  [handler full-config]
  (let [{:keys [cors preflight-handler]} (cors/compile-cors full-config)]
    (fn cors-middleware
      ([req]
       (if (identical? :options (:request-method req))
         (preflight-handler req)
         (let [request-origin (cors/get-origin req)]
           (if-let [cors-handler (.valAt ^ILookup cors request-origin)]
             (cors/add-headers-to-response cors-handler (handler req) request-origin)
             (handler req)))))
      ([req respond raise]
       (if (identical? :options (:request-method req))
         (respond (preflight-handler req))
         (let [request-origin (cors/get-origin req)]
           (if-let [cors-handler (.valAt ^ILookup cors request-origin)]
             (handler req #(respond (cors/add-headers-to-response cors-handler % request-origin))
                      raise)
             (handler req respond raise))))))))
