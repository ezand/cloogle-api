(ns cloogle-api.common
  (:require [clojure.data.json :as json]
            [me.raynes.fs :as fs])
  (:use [clojure.java.io])
  (:import [java.io ByteArrayInputStream File]
           [java.nio.file Files Paths]))

(declare ^:dynamic *properties*)

(defn load-properties
  ([] (load-properties (fs/file (System/getProperty "cloogle-properties"))))
  ([props]
    (if (instance? File props)
      (alter-var-root (var *properties*) (constantly (json/read-str (str (json/read (reader props))) :key-fn keyword)))
      (alter-var-root (var *properties*) (constantly props)))))

(defn load-resource [name]
  (let [rsc-name (str name)
        thr (Thread/currentThread)
        ldr (.getContextClassLoader thr)]
    (.getResourceAsStream ldr rsc-name)))

(defn string-input-stream [#^String s]
  "Returns a ByteArrayInputStream for the given String."
  (ByteArrayInputStream. (.getBytes s)))

(defn mime-type [file]
  (Files/probeContentType (Paths/get (.toURI file))))
