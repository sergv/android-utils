
;; date:   Sunday,  8 September 2013
;; author: Sergey Vinokurov
;; email:  serg.foo@gmail.com

(ns android.clojure.graphic_utils
  (:import [android.graphics Bitmap Canvas Color Matrix Paint Rect]
           [android.util.Log]))

(defn ^Paint color->paint
  ([argb]
     (let [p (Paint.)]
       (.setColor p argb)
       p))
  ([alpha red green blue]
     (color->paint (Color/argb alpha red green blue))))

(defmacro with-saved-matrix [canvas-var & body]
  `(try
     (.save ^Canvas ~canvas-var Canvas/MATRIX_SAVE_FLAG)
     ~@body
     (finally
       (.restore ^Canvas ~canvas-var))))

