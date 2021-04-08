(ns robotini-clojure.util)

(defn quantile [q xs]
  (let [n (dec (count xs))
        i (-> (* n q)
              (+ 1/2)
              (int))]
    (nth (sort xs) i)))

(defn statistical-summary
  [values]
  {:min (apply min values)
   :max (apply max values)
   :median (quantile 0.5 values)
   :q95 (quantile 0.95 values)})
