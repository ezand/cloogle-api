(defproject cloogle-api "0.1.0-SNAPSHOT"
  :description "Clojure wrapper for Google API"
  :url "http://github.com/ezand/cloogle-api"
  :java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure"]
  :resource-paths ["src/main/resources"]
  :test-paths ["src/test/clojure"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/fs "1.4.2"]
                 [com.google.api-client/google-api-client "1.14.1-beta"]
                 [com.google.api-client/google-api-client-extensions "1.6.0-beta"]
                 [com.google.http-client/google-http-client-jackson2 "1.14.1-beta"]
                 [com.google.oauth-client/google-oauth-client-java7 "1.14.1-beta"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.14.1-beta"]]
  :main cloogle-api.core)
