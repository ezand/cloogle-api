(defproject org.clojars.ezand/cloogle-api "1.0.1"
  :description "Clojure wrapper for Google API"
  :url "http://github.com/ezand/cloogle-api"
  :repositories [["ezand-repo" "https://raw.github.com/ezand/ezand-maven-repo/master/releases"]]
  :java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure"]
  :resource-paths ["src/main/resources"]
  :test-paths ["src/test/clojure"]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [me.raynes/fs "1.4.2"]
                 [org.clojure/data.json "0.2.2"]
                 [com.google.api-client/google-api-client "1.14.1-beta"]
                 [com.google.api-client/google-api-client-extensions "1.6.0-beta"]
                 [com.google.http-client/google-http-client-jackson2 "1.14.1-beta"]
                 [com.google.oauth-client/google-oauth-client-java7 "1.14.1-beta"]
                 [com.google.oauth-client/google-oauth-client-jetty "1.14.1-beta"]
                 [no.ezand/mime-detector "1.0"]]
  :main cloogle-api.core)
