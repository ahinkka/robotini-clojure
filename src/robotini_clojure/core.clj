(ns robotini-clojure.core
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clojure.string :as str]
            [robotini-clojure.http :as http])
  (:import (java.net Socket)
           (java.io DataInputStream PrintWriter ByteArrayInputStream)
           (javax.imageio ImageIO)
           (java.awt Dimension)))

(def team-name "Thicci Clovalainen")
(def team-color "#ffff00")

(defn connect
  [ip port]
  (println "Connecting to simulator" ip port)
  (let [simulator-socket (doto (Socket. ip port)
                           (.setTcpNoDelay true))
        in (-> simulator-socket (.getInputStream) (DataInputStream.))
        out (-> simulator-socket (.getOutputStream) (PrintWriter. true))]
    (println "Connected!")
    [in out]))

(defn write-as-json
  [output-socket data]
  (let [data-as-json (str (json/write-str data) "\n")]
    ;; (println "* send" data-as-json)
    (doto output-socket
      (.println data-as-json)
      (.flush))))

(defn read-image-bytes
  [input-socket]
  (let [image-length (.readUnsignedShort input-socket)
        image-bytes (byte-array image-length)]
    ;; (println "* reading image" image-length "bytes")
    (loop [bytes-read-so-far 0]
      (when (< bytes-read-so-far (- image-length 1))
        (recur
         (+ bytes-read-so-far
            (.read
             input-socket
             image-bytes
             bytes-read-so-far
             (- image-length bytes-read-so-far))))))
    image-bytes))

(defn bytes->image
  [image-bytes]
  (let [byte-input-stream (ByteArrayInputStream. image-bytes)]
    (ImageIO/read byte-input-stream)))

(defn signed-to-unsigned
  [signed-bytes]
  (map (fn
         [signed-byte]
         (bit-and signed-byte 0xff)) signed-bytes))

(defn image->bgr-triples
  [image]
  (partition 3
             (-> image .getRaster .getDataBuffer .getData signed-to-unsigned)))

(defn get-actions
  [pixels]
  (let [threshold 160 ;; disregard darker pixels
        [b-count g-count r-count] (reduce
                                   (fn [[b-acc g-acc r-acc] [b g r]]
                                     (cond
                                       (< (+ r g b) threshold) [b-acc g-acc r-acc]
                                       (and (< r b) (< g b)) [(inc b-acc) g-acc r-acc]
                                       (and (< r g) (< b g)) [b-acc (inc g-acc) r-acc]
                                       (and (< b r) (< g r)) [b-acc g-acc (inc r-acc)]
                                       :else [b-acc g-acc r-acc]))
                                   [0 0 0]
                                   pixels)
        total (+ b-count g-count r-count)
        r-rel (if (pos? total) (/ r-count total) 0)
        g-rel (if (pos? total) (/ g-count total) 0)
        turn (+ (* r-rel -1) g-rel)]
    ;; (println b-count r-count g-count)
    [[{"action" "forward" "value" 0.05}
      {"action" "turn" "value" turn}]
     {:b-count b-count :g-count g-count :r-count r-count
      :total total :r-rel r-rel :g-rel g-rel}]))

(defn -main
  []
  (println "Starting")
  (let [team-id (or (System/getenv "teamid") (do (println "Defaulting to teamid thicci") "thicci"))
        [simulator-ip simulator-port] (str/split
                                       (or
                                        (System/getenv "SIMULATOR")
                                        (do (println "Defaulting simulator to localhost:11000") "localhost:11000"))
                                       #":")
        [in out] (connect simulator-ip (Integer/parseInt simulator-port))
        display? (not (= "true" (System/getenv "NO_DISPLAY")))]

    (when display?
      (do
        (reset! http/move false)
        (add-watch http/move :move-changed (fn [_ _ _ should-move]
                                             (when should-move
                                               (write-as-json out {"action" "forward" "value" 0.000001}))))
        (.start (Thread. http/-main))))

    (write-as-json out {"teamId" team-id "name" team-name "color" team-color})
    (while true
      (let [buffered-image (-> in read-image-bytes bytes->image)
            pixels (-> buffered-image image->bgr-triples)
            [actions debug] (get-actions pixels)]
        (when display?
          (reset! http/state
                  {:original_frame buffered-image
                   :processed_frame buffered-image
                   :action actions
                   :debug debug}))
        (doseq [action actions]
          (when @http/move
            (write-as-json out action)))))))
