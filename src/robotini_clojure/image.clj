(ns robotini-clojure.image
  (:import (java.io DataInputStream PrintWriter ByteArrayInputStream)
           (javax.imageio ImageIO)
           (java.awt.image BufferedImage)
           (boofcv.struct.image InterleavedU8 ImageDataType)
           (boofcv.io.image ConvertBufferedImage)
           (ar.com.hjg.pngj PngReaderByte)))

;; https://hjg.com.ar/pngj/apidocs/ar/com/hjg/pngj/PngReaderByte.html
;; https://hjg.com.ar/pngj/apidocs/ar/com/hjg/pngj/ImageLineByte.html
(defn bytes->unsigned-byte-array
  [image-bytes]
  {:pre  [(bytes? image-bytes)]
   :post [(map? %)
          (bytes? (:data %))
          (= (* 3 (.getTotalPixels (:image-info %))) (count (:data %)))]}
  (let [is (ByteArrayInputStream. image-bytes)
        reader (PngReaderByte. is)
        image-info (.imgInfo reader)
        rows (.-rows image-info)
        cols (.-cols image-info)
        array (byte-array (* 3 cols rows))]
    (when (not (= (.-channels image-info) 3))
      (throw (ex-info "unexpected number of channels" {:image-info image-info})))
    ;; https://javadoc.io/doc/ar.com.hjg/pngj/latest/ar/com/hjg/pngj/IImageLineSet.html
    (let [image-line-set (.readRows reader)
          idx (volatile! 0)]
      (doseq [row-num (range rows)]
        (let [row (.getImageLine image-line-set row-num)
              scanline (.getScanlineByte row)
              byte-count (count scanline)]
          (System/arraycopy scanline 0 array @idx byte-count)
          (vswap! idx #(+ % byte-count))))) ;) ;)

    {:data array
     :image-info image-info
     :cols cols
     :rows rows}))

(defn unsigned-byte-array->interleaved
  [byte-array cols rows]
  {:pre [(bytes? byte-array)
         (integer? cols)
         (> cols 0)
         (integer? rows)
         (> rows 0)]
   :post [(= (type %) InterleavedU8)
          (= (-> % .-imageType .getNumBands) 3)
          (= (-> % .-imageType .getDataType) ImageDataType/U8)]}
  (let [result (InterleavedU8. cols rows 3)]
    (set! (. result data) byte-array)
    result))

(defn bytes->interleaved
  [image-bytes]
  {:pre  [(bytes? image-bytes)]
   :post [(instance? InterleavedU8 %)
          (= (-> % .-imageType .getNumBands) 3)
          (= (-> % .-imageType .getDataType) ImageDataType/U8)]}
     (let [array (bytes->unsigned-byte-array image-bytes)]
       (unsigned-byte-array->interleaved (:data array) (:cols array) (:rows array))))

(defn interleaved->buffered-image
  [image]
  {:pre [(instance? InterleavedU8 image)]
   :post [(instance? BufferedImage %)
          (= (.getType %) BufferedImage/TYPE_3BYTE_BGR)]}
  (let [result (BufferedImage. (.-width image) (.-height image) BufferedImage/TYPE_3BYTE_BGR)]
    (ConvertBufferedImage/convertTo image result true)
    result))
