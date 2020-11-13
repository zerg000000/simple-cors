# Simple CORS

Bare minimum CORS handler for Clojure. Alpha quality.

## Features

* Provide just enough CORS required by [Browser](https://fetch.spec.whatwg.org/#cors-protocol).
* Reasonable performance
* Support ring middleware / reitit interceptor
* Support all CORS feature, especially `Access-Control-Max-Age`

## Get Started

Add to your deps

```clojure
{zerg000000/simple-cors {:git/url "https://github.com/zerg000000/simple-cors" :sha "..."}}
```

When use in ring handler

```clojure
(require '[simple-cors.ring.middleware :as cors])

(def app (cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization"]
                                           :origins ["https://yahoo.com"
                                                     "https://google.com"]
                                           :max-age 300}}))
```

When use in reitit

```clojure
(require '[simple-cors.reitit.interceptor :as cors]
         '[reitit.interceptor.sieppari]
         '[reitit.http :as http])

(def app (http/ring-handler
           (http/router routes
                        {:reitit.http/default-options-endpoint 
                         (cors/default-options-endpoint {:cors-config {:allowed-request-methods [:post :get]
                                                                       :allowed-request-headers ["Authorization"]
                                                                       :origins ["https://yahoo.com"
                                                                                 "https://google.com"]
                                                                       :max-age 300}})})
          {:executor reitit.interceptor.sieppari/executor
           :interceptors [(interceptor/cors-interceptor {:cors-config {:allowed-request-methods [:post :get]
                                                                       :allowed-request-headers ["Authorization"]
                                                                       :origins ["https://yahoo.com"
                                                                                 "https://google.com"]
                                                                       :max-age 300}})]}))
```


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
