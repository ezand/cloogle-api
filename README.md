# cloogle-api #

![Build Status](https://travis-ci.org/ezand/cloogle-api.png?branch=master)

clooge-api is a Clojure wrapper for the Google API.

## Currently suppored features: ##
* Picasa Web Albums
    * List albums
    * List photos and videos in an album
    * Show album photo or video details
    * Create albums
    * Upload photo or video

## Requirements: ##
* [mime-detector](https://github.com/ezand/mime-detector)
* [fs](https://github.com/Raynes/fs)

## Usage: ##
In your `project.clj` file, just add `[org.clojars.ezand/cloogle-api "1.0.1"]` under the `:dependencies` vector.
