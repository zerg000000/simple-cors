(ns simple-cors.specs
  (:require
    [clojure.spec.alpha :as s]
    [simple-cors.core]))


(s/def :cors.config/max-age pos-int?)


(s/def :cors.config/origins
  (s/or :static-origins (s/+ string?)
        :any-origin #(= "*" %)
        :fn-origin #(or (fn? %) (set? %))))


(s/def :cors.config/allow-credentials? boolean?)
(s/def :cors.config/allowed-request-headers (s/+ string?))
(s/def :cors.config/allowed-request-methods (s/every #{:get :post :put :delete}))
(s/def :cors.config/preflight-response-headers (s/map-of string? string?))
(s/def :cors.config/exposed-headers (s/+ string?))


(s/def :cors/config
  (s/keys :req-un [:cors.config/origins
                   :cors.config/allowed-request-methods
                   :cors.config/allowed-request-headers]
          :opt-un [:cors.config/max-age
                   :cors.config/allow-credentials?
                   :cors.config/preflight-response-headers
                   :cors.config/exposed-headers]))


(s/fdef simple-cors.core/compile-cors-config
        :args (s/cat :config :cors/config :preflight-ok-response map?))

(s/def :cors/cors-config
  (s/or :config :cors/config :configs (s/+ :cors/config)))

(s/def :cors/preflight-ok-response map?)
(s/def :cors/preflight-forbidden-response map?)

(s/fdef simple-cors.core/compile-cors
        :args (s/cat :full-config (s/keys :req-un [:cors/cors-config]
                                          :opt-un [:cors/preflight-ok-response :cors/preflight-forbidden-response])))
