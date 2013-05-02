(ns cloogle-api.core
  (:gen-class )
  (:require [me.raynes.fs :as fs])
  (:use [cloogle-api.common]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;; work around dangerous default behaviour in Clojure
  (alter-var-root #'*read-eval* (constantly false))
  (load-properties))
