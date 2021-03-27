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

(def state (atom {}))
(def move (atom true))

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

(defn stream-handler [request]
  (with-channel request channel
    (let [watch-key (str (:remote-addr request))]
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

(defn move-handler [request]
  (let [body (req/body-string request)
        json-payload (json/read-str body)]
    (reset! move
            (cond
              (= json-payload true) true
              (= json-payload false) false
              :else (throw (ex-info "wat" {:body body}))))
    {:status 200, :body ""}))

(defn handler [request]
  ((cond
     (str/ends-with? (req/path-info request) "stream") stream-handler
     (str/ends-with? (req/path-info request) "move") move-handler
     :else redirect-handler)
   request))

(def app
  (-> handler
      (wrap-resource "public")
      (wrap-content-type)
      (wrap-not-modified)))

(defn -main
  [& args]
  (run-server #'app {:port 8080}))
