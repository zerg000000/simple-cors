(ns simple-cors.ring.middleware
  (:require [simple-cors.core :as cors]))

(defn wrap-cors [handler {:keys [cors-configs
                                 preflight-forbidden-response
                                 preflight-ok-response
                                 forbidden-response]
                          :or {preflight-forbidden-response cors/default-preflight-forbidden-response
                               preflight-ok-response cors/default-preflight-ok-response
                               forbidden-response cors/default-preflight-forbidden-response}}]
  (let [preflight-handler (cors/make-cors-preflight-handler cors-configs preflight-forbidden-response preflight-ok-response)]
    (fn cors-handler [req]
      (if (= :options (:request-method req))
        (preflight-handler req)
        (let [request-origin (-> req :headers (get "origin"))
              response-fn (-> cors-configs (get request-origin) :cors-response-fn)
              resp (handler req)]
          (response-fn resp request-origin))))))