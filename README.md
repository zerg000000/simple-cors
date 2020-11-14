# Simple CORS

Bare minimum CORS handler for Clojure. Alpha quality.

## Features

* Provide just enough CORS required by [Browser](https://fetch.spec.whatwg.org/#cors-protocol).
* Reasonable performance
* Support Ring middleware / Reitit interceptor
* Support all CORS feature, especially `Access-Control-Max-Age`

## Get Started

Add to your deps.edn

```clojure
{zerg000000/simple-cors {:git/url "https://github.com/zerg000000/simple-cors" :sha "..."}}
```

When use in [ring](https://github.com/ring-clojure/ring) handler

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
      :interceptors [(interceptor/cors-interceptor config)]})))
```

Full config map, you can also see the spec in `simple-cors.specs`

```clojure
{:cors-config {:allowed-request-methods [:post :get]
               :allowed-request-headers ["Authorization"]
               :allow-credentials? true
               :origins ["https://yahoo.com"
                         "https://google.com"]
               :max-age 300
               :exposed-headers ["x-amz-date"]}}
```

## Detail

### Support origin exact match only

Since regex is bad for performance and hard to write flawless pattern.

### Not Support Any Origin

Since this is not used in my application, unless someone could provide a PR

### Not checking or blocking invalid request

Since the main idea of CORS is to provide information for a browser to take action.
In most of the case, we can do little on pure server side

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
