
;; date:   Sunday, 31 March 2013
;; author: Sergey Vinokurov
;; email:  serg.foo@gmail.com

(ns android.clojure.graphic
  (:import [android.graphics Bitmap Canvas Color Paint Rect]))

(defn color->paint
  ([argb]
     (let [p (Paint.)]
       (.setColor p argb)
       p))
  ([alpha red green blue]
     (color->paint (Color/argb alpha red green blue))))

(defn draw-grid [^Canvas canvas
                 n
                 ^Paint paint]
  (let [width  (.getWidth canvas)
        height (.getHeight canvas)
        dx (/ width n)
        dy (/ height n)]
    (loop [i 0]
      (.drawLine canvas (* i dx) 0        (* i dx) height   paint)
      (.drawLine canvas 0        (* i dy) width    (* i dy) paint)
      (when (not= i n)
        (recur (+ i 1))))))

