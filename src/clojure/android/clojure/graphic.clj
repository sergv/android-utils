
;; date:   Sunday, 31 March 2013
;; author: Sergey Vinokurov
;; email:  serg.foo@gmail.com

(ns android.clojure.graphic
  (:import [android.graphics Bitmap Canvas Color Paint Rect]
           [android.util.Log]))

(defn- log
  ([msg] (android.util.Log/d "graphic.clj" msg))
  ([msg & args] (log (apply format msg args))))

(defn color->paint
  ([argb]
     (let [p (Paint.)]
       (.setColor p argb)
       p))
  ([alpha red green blue]
     (color->paint (Color/argb alpha red green blue))))

(defn adjust-stroke-width ^Paint [^Paint p
                                  new-width]
  (let [new-paint (Paint. p)]
    (.setStrokeWidth new-paint new-width)
    new-paint))

(defmacro with-saved-matrix [canvas-var & body]
  `(try
     (.save ^Canvas ~canvas-var Canvas/MATRIX_SAVE_FLAG)
     ~@body
     (finally
       (.restore ^Canvas ~canvas-var))))

(defn draw-grid
  ^{:pre [(not (nil? canvas))
          (not (nil? paint))
          (not (nil? label-paint))]}
  [^Canvas canvas
   x-labels
   y-labels
   ^Paint paint
   ^Paint label-paint]
  (let [width  (.getWidth canvas)
        height (.getHeight canvas)
        dx (/ width (dec (count x-labels)))
        dy (/ height (dec (count y-labels)))
        x-label-paint (let [p (Paint. label-paint)]
                        (.setTextAlign p android.graphics.Paint$Align/CENTER)
                        p)
        y-label-paint (let [p (Paint. label-paint)]
                        (.setTextAlign p android.graphics.Paint$Align/RIGHT)
                        p)]
    (loop [i 0
           x-labels (seq x-labels)]
      (when x-labels
        (.drawLine canvas (* i dx) 0 (* i dx) height paint)
        (.drawText canvas (first x-labels) (* i dx) 0 x-label-paint)
        (recur (inc i) (next x-labels))))
    (loop [i 0
           y-labels (seq y-labels)]
      (when y-labels
        (.drawLine canvas 0 (* i dy) width (* i dy) paint)
        (.drawText canvas (first y-labels) 0 (* i dy) y-label-paint)
        (recur (inc i) (next y-labels))))))

