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
  (:use [cloogle-api.common])
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

(defn- album-as-map [album-entry]
  {:_self album-entry
   :title (.title album-entry)
   :feed-link (.getFeedLink album-entry)
   :updated (.updated album-entry)
   :summary (.summary album-entry)})

(defn- photo-as-map [photo-entry]
  (let [content (-> photo-entry
                  (.mediaGroup)
                  (.content))]
    {:_self photo-entry
     :title (.title photo-entry)
     :type (.type content)
     :url (.url content)
     :updated (.updated photo-entry)
     :summary (.summary photo-entry)
     :feed-link (.getFeedLink photo-entry)}))

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

(defn albums []
  (set-client! (picasa-client "GOOGLE SYNC" (load-resource "clients_secret.json")))
  (let [user-feed (.executeGetUserFeed *picasa-client* (PicasaUrl/relativeToRoot "feed/api/user/default"))]
    (map album-as-map (vec (.albums user-feed)))))

(defn photos [{:keys [feed-link]}]
  (let [album-feed (.executeGetAlbumFeed *picasa-client* (PicasaUrl. feed-link))]
    (map photo-as-map (vec (.photos album-feed)))))

(defn photo-details [{:keys [feed-link]}]
  (let [photo-details-feed (.executeGetPhotoDetailsFeed *picasa-client* (PicasaUrl. feed-link))]
    (photo-details-as-map photo-details-feed)))
