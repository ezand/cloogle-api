(ns cloogle-api.picasa
  (:import [com.google.api.services.picasa PicasaClient PicasaUrl]
           [com.google.api.client.http FileContent]
           [com.google.api.client.extensions.java6.auth.oauth2 AuthorizationCodeInstalledApp FileCredentialStore]
           [com.google.api.client.extensions.jetty.auth.oauth2 LocalServerReceiver]
           [com.google.api.client.googleapis.auth.oauth2 GoogleClientSecrets GoogleAuthorizationCodeFlow GoogleAuthorizationCodeFlow$Builder]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.services.picasa.model AlbumEntry PhotoEntry PhotoDetailsFeed UserFeed Entry Link]
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

(defn- entry-type [entry]
  (last (clojure.string/split (:term (:category entry)) #"#")))

(defn- link-as-map [link]
  {:href (.href link)
   :rel (.rel link)})

(defn- category-as-map [category]
  {:scheme (.scheme category)
   :term (.term category)})

(defn- entry-as-map [entry]
  {:id (.id entry)
   :etag (.etag entry)
   :title (.title entry)
   :feed-link (.getFeedLink entry)
   :updated (.updated entry)
   :summary (.summary entry)
   :category (category-as-map (.category entry))
   :links (map link-as-map (.links entry))})

(defn- album-as-map [album]
  (merge {:_self album
          :access (.access album)
          :photo-count (.numPhotos album)}
    (entry-as-map album)))

(defn- photo-as-map [photo]
  (let [content (-> photo
                  (.mediaGroup)
                  (.content))]
    (merge {:_self photo
            :type (.type content)
            :url (.url content)}
      (entry-as-map photo))))

(defn- photo-details-as-map [photo-details]
  (let [content (-> photo-details
                  (.mediaGroup)
                  (.content))]
    {:_self photo-details
     :id (.id photo-details)
     :album-id (.albumId photo-details)
     :title (.title photo-details)
     :summary (.summary photo-details)
     :icon (.icon photo-details)
     :updated (.updated photo-details)
     :type (.type content)
     :url (.url content)}))

(defn- user-as-map [user-feed]
  {:thumbnail (.thumbnail user-feed)
   :author (.name (.author user-feed))
   :url (.uri (.author user-feed))
   :updated (.updated user-feed)
   :id (.title user-feed)
   :summary (.summary user-feed)
   :links (map link-as-map (.links user-feed))})

(defn- map-as-album [album-map]
  (let [album (AlbumEntry.)]
    (set! (. album access) (:access album-map))
    (set! (. album title) (:title album-map))
    (set! (. album summary) (:summary album-map))
    album))

(defn- map-as-link [link-map]
  (let [link (Link.)]
    (set! (. link href) (:href link-map))
    (set! (. link rel) (:rel link-map))
    link))

(defn- user-feed []
  (.executeGetUserFeed *picasa-client* (PicasaUrl/relativeToRoot "feed/api/user/default")))

(defn auth [app-name]
  "Authenticate and authorize to use Picasa services"
  (let [template (slurp (load-resource "clients_secret_template.json"))]
    (let [client_secrets (format template (:client-id *properties*) (:client-secret *properties*))]
      (set-client! (picasa-client app-name (string-input-stream (str client_secrets)))))))

(defn create-album! [album]
  "Creates a new album in the currently authorized user account"
  (album-as-map (.executeInsert *picasa-client* (user-feed) (map-as-album album))))

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

(defn user-info []
  "Details of currently authorized user"
  (user-as-map (user-feed)))

(defn albums []
  "Albums owned by currently authorized user"
  (map album-as-map (vec (.albums (user-feed)))))

(defn photos [{:keys [feed-link]}]
  "Photos that belongs to the specified album"
  (let [album-feed (.executeGetAlbumFeed *picasa-client* (PicasaUrl. feed-link))]
    (map photo-as-map (vec (.photos album-feed)))))

(defn photo-details [{:keys [feed-link]}]
  "Detailed information of the specified photo"
  (let [photo-details-feed (.executeGetPhotoDetailsFeed *picasa-client* (PicasaUrl. feed-link))]
    (photo-details-as-map photo-details-feed)))
