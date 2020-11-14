# Simple CORS

[![Clojars Project](https://img.shields.io/clojars/v/zerg000000/simple-cors.svg)](https://clojars.org/zerg000000/simple-cors)

Bare minimum CORS middleware/interceptor for Clojure.

## Features

* Provide just enough CORS required by [Browser](https://fetch.spec.whatwg.org/#cors-protocol).
* Reasonable performance
* Support Ring middleware / Reitit interceptor
* Support all CORS feature, especially `Access-Control-Max-Age`

## Get Started

Add to your deps.edn

```clojure
{zerg000000/simple-cors {:mvn/version "0.0.2"}}
```

When use in [Ring](https://github.com/ring-clojure/ring) handler

```clojure
(require '[simple-cors.ring.middleware :as cors])

(def app (cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization"]
                                           :origins ["https://yahoo.com"
                                                     "https://google.com"]
                                           :max-age 300}}))
```

When use in [Reitit](https://github.com/metosin/reitit)

```clojure
(require '[simple-cors.reitit.interceptor :as cors]
         '[reitit.interceptor.sieppari]
         '[reitit.http :as http])

(def app 
  (let [config {:cors-config {:allowed-request-methods [:post :get]
                              :allowed-request-headers ["Authorization"]
                              :origins ["https://yahoo.com"
                                        "https://google.com"]
                              :max-age 300}}]
    (http/ring-handler
     (http/router routes
                  {:reitit.http/default-options-endpoint 
                   (cors/default-options-endpoint config)})
     {:executor reitit.interceptor.sieppari/executor
      :interceptors [(cors/cors-interceptor config)]})))
```

Full config map, you can also see the spec in `simple-cors.specs`

```clojure
{:cors-config {:allowed-request-methods [:post :get]
               :allowed-request-headers ["Authorization"]
               :allow-credentials? true
               :origins ["https://yahoo.com"
                         "https://google.com"]
               :max-age 300
               :exposed-headers ["x-amz-date"]}
 :preflight-forbidden-response {:status 403}
 :preflight-ok-response {:status 200}}
```

## Why

### Support origin exact match only

Since regex is bad for performance and hard to write a flawless pattern.

### Not Support Any Origin

Since I don't use Any Origin `*`. We will have this feature unless someone could provide a PR ;-)

### Not checking or blocking invalid request

Since the main idea of CORS is to provide information for a browser to take action.
In most of the cases, we can do little on pure server side

## Reference

### Reference Doc

* [whatwg](https://fetch.spec.whatwg.org/#cors-protocol)
* [MDN](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)

### Reference Implementation 

* [pedestal](https://github.com/pedestal/pedestal/blob/aa71a3a630dd21861c0682eeeebec762cbf3f85c/service/src/io/pedestal/http/cors.clj)
* [nasus](https://github.com/kachayev/nasus/blob/371d60e08948c52c56ae1f0ac3f39f4105383aaf/src/http/server.clj#L317)
* [netty](https://github.com/barchart/barchart-project-netty/blob/master/codec-http/src/main/java/io/netty/handler/codec/http/cors/CorsHandler.java)


## License

Copyright © 2020 Simple CORS

Simple CORS is licensed under the MIT license, available at MIT and also in the LICENSE file.
