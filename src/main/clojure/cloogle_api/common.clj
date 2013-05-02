(ns cloogle-api.common
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs])
  (:use [clojure.java.io])
  (:import [java.io ByteArrayInputStream]))

(declare ^:dynamic *properties*)

(defn load-properties
  ([] (load-properties (fs/file (System/getProperty "cloogle-properties"))))
  ([file] (alter-var-root (var *properties*) (constantly (json/read-json (str (json/read (reader file))))))))

(defn load-resource [name]
  (let [rsc-name (str name)
        thr (Thread/currentThread)
        ldr (.getContextClassLoader thr)]
    (.getResourceAsStream ldr rsc-name)))

(defn string-input-stream [#^String s]
  "Returns a ByteArrayInputStream for the given String."
  (ByteArrayInputStream. (.getBytes s)))