(ns cloogle-api.common)
  (defn load-resource
    [name]
    (let [rsc-name (str name)
          thr (Thread/currentThread)
          ldr (.getContextClassLoader thr)]
      (.getResourceAsStream ldr rsc-name)))