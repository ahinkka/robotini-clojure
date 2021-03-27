(ns robotini-clojure.http
  (:use org.httpkit.server
        ring.middleware.resource
        ring.middleware.content-type
        ring.middleware.not-modified)
  (:require [clojure.string :as str]
            [ring.util.request :as req]
            [clojure.data.json :as json])
  (:import (java.util Base64)
           (java.io ByteArrayOutputStream)
           (javax.imageio ImageIO)))

(defn redirect-handler [request]
  {:status 301
   :headers {"Content-Type" "text/plain"
             "Location" "index.html"}
   :body "See index.html"})

(defn buffered-image->base64-png
  [buffered-image]
  (let [os (ByteArrayOutputStream.)
        _ (ImageIO/write buffered-image "png", os)
        encoder (Base64/getEncoder)
        encoded (.encodeToString encoder (.toByteArray os))]
    (str "data:image/png;base64," encoded)))

(def state (atom {}))
(defn stream-handler [request]
  (with-channel request channel
    (let [watch-key (:remote-addr request)]
      (on-close channel (fn [status]
                          (remove-watch @state watch-key)
                          (println "channel closed, " status)))
      (add-watch
       state
       watch-key
       (fn [_ _ _ current-state]
         (when (some? current-state)
           (let [update (merge current-state
                               {:original_frame (buffered-image->base64-png (:original_frame current-state))
                                :processed_frame (buffered-image->base64-png (:processed_frame current-state))})]
             (send! channel (str (json/write-str update) "\r\n") false))))))))

(defn handler [request]
  (if (str/ends-with? (req/path-info request) "stream")
    (-> request stream-handler)
    (-> request redirect-handler)))

(def app
  (-> handler
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defn -main
  [& args]
  (run-server #'app {:port 8080}))
