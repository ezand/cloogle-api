(ns cloogle-api.picasa
  (:import [com.google.api.services.picasa.model PhotoDetailsFeed])
  (:import [com.google.api.services.picasa PicasaClient]
           [com.google.api.client.auth.oauth2 Credential]
           [com.google.api.services.picasa PicasaUrl]
           [com.google.api.client.extensions.java6.auth.oauth2 AuthorizationCodeInstalledApp FileCredentialStore]
           [com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver]
           [com.google.api.client.googleapis.auth.oauth2 GoogleClientSecrets GoogleAuthorizationCodeFlow GoogleAuthorizationCodeFlow$Builder]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [java.util Date]
           [java.nio.charset Charset]
           [java.io ByteArrayOutputStream]
           [com.google.api.services.picasa.model AlbumEntry PhotoDetailsFeed])
  (:use [cloogle-api.common]
        [clojure.java.io])
  (:require [me.raynes.fs :as fs]))

(def ^:private credentials-files
  (fs/file (System/getProperty "user.home") ".credentials/picasa.json"))

(defn- init-credentials []
  (fs/mkdirs (.getParentFile credentials-files))
  (fs/create credentials-files))

(declare ^:dynamic ^PicasaClient *picasa-client*)

(defn picasa-client [name client-secret]
  (init-credentials)
  (let [http-transport (GoogleNetHttpTransport/newTrustedTransport)
        json-factory (JacksonFactory.)
        secrets (GoogleClientSecrets/load json-factory client-secret)
        credentials (-> (GoogleAuthorizationCodeFlow$Builder. http-transport json-factory secrets (list PicasaUrl/ROOT_URL))
                      (.setCredentialStore (FileCredentialStore. credentials-files json-factory))
                      (.build)
                      (AuthorizationCodeInstalledApp. (LocalServerReceiver.))
                      (.authorize "user"))
        client (PicasaClient. (.createRequestFactory http-transport credentials))]
    (.setApplicationName client name)
    client))

(defn set-client! [client]
  (alter-var-root (var *picasa-client*) (constantly client)))

(defn- album-as-map [album]
  {:_self album
   :title (.title album)
   :feed-link (.getFeedLink album)
   :updated (.updated album)
   :summary (.summary album)})

(defn- photo-as-map [photo]
  (let [content (-> photo
                  (.mediaGroup)
                  (.content))]
    {:_self photo
     :title (.title photo)
     :type (.type content)
     :url (.url content)
     :updated (.updated photo)
     :summary (.summary photo)
     :feed-link (.getFeedLink photo)}))

(defn- photo-details-as-map [photo-details]
  (let [content (-> photo-details
                  (.mediaGroup)
                  (.content))]
    {:_self photo-details
     :id (.id photo-details)
     :album-id (.albumId photo-details)
     :title (.title photo-details)
     :subtitle (.subtitle photo-details)
     :icon (.icon photo-details)
     :updated (.updated photo-details)
     :type (.type content)
     :url (.url content)}))

(defn login [app-name]
  (let [template (slurp (load-resource "clients_secret_template.json"))]
    (let [client_secrets (format template (*properties* :client-id ) (*properties* :client-secret ))]
      (set-client! (picasa-client app-name (string-input-stream (str client_secrets)))))))

(defn albums []
  (let [user-feed (.executeGetUserFeed *picasa-client* (PicasaUrl/relativeToRoot "feed/api/user/default"))]
    (map album-as-map (vec (.albums user-feed)))))

(defn photos [{:keys [feed-link]}]
  (let [album-feed (.executeGetAlbumFeed *picasa-client* (PicasaUrl. feed-link))]
    (map photo-as-map (vec (.photos album-feed)))))

(defn photo-details [{:keys [feed-link]}]
  (let [photo-details-feed (.executeGetPhotoDetailsFeed *picasa-client* (PicasaUrl. feed-link))]
    (photo-details-as-map photo-details-feed)))
