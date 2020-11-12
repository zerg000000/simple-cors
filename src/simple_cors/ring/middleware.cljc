(ns simple-cors.ring.middleware
  (:require [simple-cors.core :as cors]))

(defn wrap [handler {:keys [cors-config
                            preflight-forbidden-response
                            preflight-ok-response]
                     :or {preflight-forbidden-response cors/default-preflight-forbidden-response
                          preflight-ok-response cors/default-preflight-ok-response}}]
  (let [cors (cors/compile-cors-config cors-config)
        preflight-handler (cors/make-cors-preflight-handler cors preflight-forbidden-response preflight-ok-response)]
    (fn cors-middleware
      ([req]
       (if (= :options (:request-method req))
         (preflight-handler req)
         (if-let [cors-handler (get cors (-> req :headers (get "origin")))]
           (cors/add-headers-to-response cors-handler (handler req))
           (handler req))))
      ([req respond raise]
       (if (= :options (:request-method req))
         (respond (preflight-handler req))
         (if-let [cors-handler (get cors (-> req :headers (get "origin")))]
           (handler req #(respond (cors/add-headers-to-response cors-handler %))
                    raise)
           (handler req respond raise)))))))
