# Datomic Spec

[![Build Status](https://travis-ci.org/alexanderkiel/datomic-spec.svg?branch=master)](https://travis-ci.org/alexanderkiel/datomic-spec)

Clojure specs for all functions of the `datomic.api` namespace.

## Usage

This library is intended to be included at dev and test time. If you use leiningen, you should include the dependency in your dev profile.

```clojure
(defproject my-app "0.1"
  :profiles {:dev {:dependencies [[datomic-spec "0.1-SNAPSHOT"]]}})
```

In order to instrument the `datomic.api` functions, you call the following in a namespace which is only loaded at dev and/or test time.

```clojure
(ns user
  (:require [datomic-spec.core :as ds])
  
(ds/instrument)
```

The function `datomic-spec.core/instrument` is similar to `clojure.spec.test/instrument` but instruments the functions of `datomic.api` with the appropriate spec overrides. It doesn't instrument other functions. So you have to call `clojure.spec.test/instrument` yourself in addition to `datomic-spec.core/instrument`.

## Specs

This library contains specs for Datomic data structures. You can use specs like `:datomic-spec.core/db` in your own code.

## Similar Projects

* [datomic-spec](https://github.com/nwjsmith/datomic-spec)

## License

Copyright © 2016 Alexander Kiel

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
