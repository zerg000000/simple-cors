(ns simple-cors.specs
  (:require [clojure.spec.alpha :as s]))

(s/def :cors.config/max-age pos-int?)
(s/def :cors.config/origins (s/+ string?))
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
