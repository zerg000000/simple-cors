# Simple CORS

[![Clojars Project](https://img.shields.io/clojars/v/zerg000000/simple-cors.svg)](https://clojars.org/zerg000000/simple-cors)

Bare minimum CORS middleware/interceptor for Clojure.

## Features

* Provide just enough CORS required by [Browser](https://fetch.spec.whatwg.org/#cors-protocol).
* Reasonable performance
* Support Ring middleware / Reitit interceptor / Aleph middleware
* Support all CORS features, especially `Access-Control-Max-Age`

## Get Started

Add to your deps.edn

```clojure
{zerg000000/simple-cors {:mvn/version "0.0.8"}}
```

When use in [Ring](https://github.com/ring-clojure/ring) handler

```clojure
(require '[simple-cors.ring.middleware :as cors])

(def app (cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization" "Content-Type"]
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
                              :allowed-request-headers ["Authorization" "Content-Type"]
                              :origins ["https://yahoo.com"
                                        "https://google.com"]
                              :max-age 300}}]
    (http/ring-handler
     (http/router routes
                  {:reitit.http/default-options-endpoint 
                   (cors/make-default-options-endpoint config)})
     {:executor reitit.interceptor.sieppari/executor
      :interceptors [(cors/cors-interceptor config)]})))
```

When use in [Aleph](https://github.com/aleph-io/aleph)

```clojure
(require '[simple-cors.aleph.middleware :as cors])

(def app (cors/wrap handler {:cors-config {:allowed-request-methods [:post :get]
                                           :allowed-request-headers ["Authorization" "Content-Type"]
                                           :origins ["https://yahoo.com"
                                                     "https://google.com"]
                                           :max-age 300}}))
```

Full config map, you can also see the spec in `simple-cors.specs`

```clojure
{:cors-config {:allowed-request-methods [:post :get]
               :allowed-request-headers ["Authorization" "Content-Type"]
               :allow-credentials? true
               :origins ["https://yahoo.com"
                         "https://google.com"]
               :max-age 300
               :exposed-headers ["x-amz-date"]}
 :preflight-forbidden-response {:status 403}
 :preflight-ok-response {:status 200}}
```

### Static / Any Origin / Fn CORS

Normally, Static is good and enough

```clojure
{:cors-config {...
               :origins ["https://whatever.co"]
               ...}}
```

Some casual user might want CORS matched with any origin

```clojure
{:cors-config {...
               :origins "*"
               ...}}
```

The ultimate solution is to provide your own matching function

```clojure
{:cors-config {...
               :origins #{"https://whatever.co"}
               ...}}
; or
{:cors-config {...
               :origins (fn [origin] (and (str/starts-with? origin "https://")
                                          (str/ends-with? origin ".google.com")))
               ...}}
```

### Combine Multiple Config

Support combining multiple CORS config with performance penalty.
At most one AnyOrigin in configs, and will act as the last fallback. 

```clojure
{:cors-config [{...
                :origin "*"
                ...}
               {...
                :origin ["http://abc"]
                ...}]}
```

## Why

### Not checking or blocking invalid request

Since the main idea of CORS is to provide information for a browser to take action.
In most of the cases, we can do little on pure server side

## TODO

- [ ] more tests
- [ ] more docstring

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
