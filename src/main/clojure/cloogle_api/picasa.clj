(ns cloogle-api.picasa
  (:import [com.google.api.services.picasa.model PhotoDetailsFeed UserFeed]
           [com.google.api.services.picasa PicasaClient]
           [com.google.api.services.picasa PicasaUrl]
           [com.google.api.client.http FileContent InputStreamContent]
           [com.google.api.client.auth.oauth2 Credential]
           [com.google.api.client.extensions.java6.auth.oauth2 AuthorizationCodeInstalledApp FileCredentialStore]
           [com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver]
           [com.google.api.client.googleapis.auth.oauth2 GoogleClientSecrets GoogleAuthorizationCodeFlow GoogleAuthorizationCodeFlow$Builder]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [java.util Date]
           [java.nio.charset Charset]
           [java.io ByteArrayOutputStream]
           [com.google.api.services.picasa.model AlbumEntry PhotoEntry PhotoDetailsFeed]
           [java.net URL])
  (:use [cloogle-api.common])
  (:require [me.raynes.fs :as fs]
            [clojure.java.io :as io]))

(def ^:private credentials-files
  (let [file (fs/file (str (System/getProperty "user.home") "/.credentials/picasa.json"))]
    (if-not (fs/file? file)
      (do
        (fs/mkdirs (.getParentFile file))
        (spit file "{}\n\n"))
      file)))

(declare ^:dynamic ^PicasaClient *picasa-client*)
(declare ^:dynamic ^UserFeed *user-feed*)

(defn- picasa-client [name client-secret]
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

(defn- set-client! [client]
  (alter-var-root (var *picasa-client*) (constantly client)))

(defn- album-as-map [album]
  {:_self album
   :access (.access album)
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

(defn- map-as-album [album-map]
  (let [album (AlbumEntry.)]
    (set! (. album access) (:access album-map))
    (set! (. album title) (:title album-map))
    (set! (. album summary) (:summary album-map))
    album))

(defn login [app-name]
  (let [template (slurp (load-resource "clients_secret_template.json"))]
    (let [client_secrets (format template (:client-id *properties*) (:client-secret *properties*))]
      (set-client! (picasa-client app-name (string-input-stream (str client_secrets))))
      (alter-var-root (var *user-feed*) (constantly (.executeGetUserFeed *picasa-client* (PicasaUrl/relativeToRoot "feed/api/user/default")))))))

(defn create-album! [album]
  (album-as-map (.executeInsert *picasa-client* *user-feed* (map-as-album album))))

(defn upload-media!
  "Upload videos and photos to a Picasa album with optional meta data"
  ([album file] (upload-media! album file nil))
  ([album file meta-data]
    (let [file-content (FileContent. (mime-type file) file)]
      (let [entry (PhotoEntry.)]
        (set! (. entry title) (.getName file))
        (if-not (nil? meta-data)
          (do
            (set! (. entry summary) (:summary meta-data))))
        (photo-as-map (.executeInsertPhotoEntryWithMetadata *picasa-client* entry (PicasaUrl. (:feed-link album)) file-content))))))

(defn albums []
  (map album-as-map (vec (.albums *user-feed*))))

(defn photos [{:keys [feed-link]}]
  (let [album-feed (.executeGetAlbumFeed *picasa-client* (PicasaUrl. feed-link))]
    (map photo-as-map (vec (.photos album-feed)))))

(defn photo-details [{:keys [feed-link]}]
  (let [photo-details-feed (.executeGetPhotoDetailsFeed *picasa-client* (PicasaUrl. feed-link))]
    (photo-details-as-map photo-details-feed)))