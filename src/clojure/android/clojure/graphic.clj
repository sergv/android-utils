
;; date:   Sunday, 31 March 2013
;; author: Sergey Vinokurov
;; email:  serg.foo@gmail.com

(ns android.clojure.graphic
  (:import [android.graphics Bitmap Canvas Color Matrix Paint Rect]
           [android.util.Log])
  (:use [android.clojure.graphic_utils]))

(defn- log
  ([msg] (android.util.Log/d "graphic.clj" msg))
  ([msg & args] (log (apply format msg args))))

(defn adjust-stroke-width ^Paint [^Paint p
                                  new-width]
  (let [new-paint (Paint. p)]
    (.setStrokeWidth new-paint new-width)
    new-paint))

(defn adjust-style ^Paint [^Paint p
                           ^android.graphics.Paint$Style new-style]
  (let [new-paint (Paint. p)]
    (.setStyle new-paint new-style)
    new-paint))

(defn adjust-stroke-cap ^Paint [^Paint p
                                ^android.graphics.Paint$Cap new-cap]
  (let [new-paint ^Paint (Paint. p)]
    (.setStrokeCap new-paint new-cap)
    new-paint))

(defn draw-grid
  ^{:pre [(not (nil? canvas))
          (not (nil? paint))
          (not (nil? label-paint))]}
  [^Canvas canvas
   x-labels ;; seq of strings
   y-labels ;; seq of strings
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

;;;; transformation

;; old data for 4-fields transformation
;; Corresponds to transformation matrix:
;; $$[[strip-comments]] \begin{math} T = \begin{bmatrix}
;;   \text{scale-x} & 0              & \text{offset-x}\\
;;   0              & \text{scale-y} & \text{offset-y}\\
;;   0              & 0              & 1\\
;; \end{bmatrix}\end{math} $$
;; The inverse would be:
;; $$[[strip-comments]] \begin{math} T^{-1} = \begin{bmatrix}
;;   \frac{1}{\text{scale-x}} & 0                        & \frac{- \text{offset-x}}{\text{scale-x}}\\
;;   0                        & \frac{1}{\text{scale-y}} & \frac{- \text{offset-y}}{\text{scale-y}}\\
;;   0                        & 0                        & 1\\
;; \end{bmatrix}\end{math} $$
;; The main argument for introducing this datastructure instead of using
;; exisisting android.graphics.Matrix class was to allow
;; $$ \text{scale-x}, \text{scale-y}, \text{offset-x}, \text{offset-y} \in \mathbb{Q} $$
;;
;; Transformation composition:
;; $$[[strip-comments]] \begin{math}
;; \begin{bmatrix}
;;   \text{scale-x}_A & 0                & \text{offset-x}_A\\
;;   0                & \text{scale-y}_A & \text{offset-y}_A\\
;;   0                & 0                & 1\\
;; \end{bmatrix}
;; \cdot
;; \begin{bmatrix}
;;   \text{scale-x}_B & 0                & \text{offset-x}_B\\
;;   0                & \text{scale-y}_B & \text{offset-y}_B\\
;;   0                & 0                & 1\\
;; \end{bmatrix}\end{math}
;; $$
;;
;;
;; for matrix transformation
;; Corresponds to transformation matrix:
;; $$[[strip-comments]] \begin{math} T = \begin{bmatrix}
;;   a_{1 1} & a_{1 2} & a_{1 3}\\
;;   a_{2 1} & a_{2 2} & a_{2 3}\\
;;   a_{3 1} & a_{3 2} & a_{3 3}\\
;; \end{bmatrix}\end{math} $$
;;
;; The inverse would be
;; $$[[strip-comments]] \begin{math} T = \begin{bmatrix}
;; \end{bmatrix}\end{math} $$
(defrecord Transformation [a11 a12 a13
                           a21 a22 a23
                           a31 a32 a33
                           ;; scale-x
                           ;; scale-y
                           ;; offset-x
                           ;; offset-y
                           ])

(defn ^Matrix transformation->matrix [^Transformation t]
  (let [m (Matrix.)]
    (.setValues m (float-array (vector (.a11 t) (.a12 t) (.a13 t)
                                       (.a21 t) (.a22 t) (.a23 t)
                                       (.a31 t) (.a32 t) (.a33 t))))
    m))

(defn ^Transformation make-transformation
  [scale-x
   scale-y
   offset-x
   offset-y]
  (Transformation. scale-x 0       offset-x
                   0       scale-y offset-y
                   0       0       1))

(def ^{:tag Transformation
       :const true}
  identity-transformation
  (Transformation. 1 0 0
                   0 1 0
                   0 0 1))

(defprotocol Transformable
  (apply-transform [item ^Transformation transform])
  (inverse-transform [item ^Transformation transform]))

(defn ^{:private true
        :inline (fn [a b c x y z]
                  `(+ (* ~a ~x) (* ~b ~y) (* ~c ~z)))}
  dot-prod [a b c x y z]
  (+ (* a x) (* b y) (* c z)))

(extend-protocol Transformable
  Transformation
  (apply-transform [^Transformation source ^Transformation transform]
    (Transformation. (dot-prod (.a11 source) (.a12 source) (.a13 source)
                               (.a11 transform) (.a21 transform) (.a31 transform))
                     (dot-prod (.a11 source) (.a12 source) (.a13 source)
                               (.a12 transform) (.a22 transform) (.a32 transform))
                     (dot-prod (.a11 source) (.a12 source) (.a13 source)
                               (.a13 transform) (.a23 transform) (.a33 transform))

                     (dot-prod (.a21 source) (.a22 source) (.a23 source)
                               (.a11 transform) (.a21 transform) (.a31 transform))
                     (dot-prod (.a21 source) (.a22 source) (.a23 source)
                               (.a12 transform) (.a22 transform) (.a32 transform))
                     (dot-prod (.a21 source) (.a22 source) (.a23 source)
                               (.a13 transform) (.a23 transform) (.a33 transform))

                     (dot-prod (.a31 source) (.a32 source) (.a33 source)
                               (.a11 transform) (.a21 transform) (.a31 transform))
                     (dot-prod (.a31 source) (.a32 source) (.a33 source)
                               (.a12 transform) (.a22 transform) (.a32 transform))
                     (dot-prod (.a31 source) (.a32 source) (.a33 source)
                               (.a13 transform) (.a23 transform) (.a33 transform))))
  (inverse-transform [^Transformation source ^Transformation m0]
    ;; yeah, this is not the best inverse computation ever
    (let [t-1-down-2 (if (zero? (.a21 m0))
                       identity-transformation
                       (Transformation. 1  0                       0
                                        -1 (/ (.a11 m0) (.a21 m0)) 0
                                        0  0                       1))
          t-1-down-3 (if (zero? (.a31 m0))
                       identity-transformation
                       (Transformation. 1  0 0
                                        0  1 0
                                        -1 0 (/ (.a11 m0) (.a31 m0))))
          ^Transformation m1 (->> m0
                                  (apply-transform t-1-down-2 ,,,)
                                  (apply-transform t-1-down-3 ,,,))
          ^Transformation source1 (->> source
                                       (apply-transform t-1-down-2 ,,,)
                                       (apply-transform t-1-down-3 ,,,))
          t-2-down-3 (if (zero? (.a32 m1))
                       identity-transformation
                       (Transformation. 1 0  0
                                        0 1  0
                                        0 -1 (/ (.a22 m1) (.a32 m1))))
          ^Transformation m2 (apply-transform t-2-down-3 m1)
          ^Transformation source2 (apply-transform t-2-down-3 source1)
          t-3-up-1 (if (zero? (.a13 m2))
                     identity-transformation
                     (Transformation. (/ (.a33 m2) (.a13 m2)) 0 -1
                                      0                       1 0
                                      0                       0 1))
          t-3-up-2 (if (zero? (.a23 m2))
                     identity-transformation
                     (Transformation. 1 0                        0
                                      0 (/ (.a33 m2) (.a23 m2)) -1
                                      0 0                        1))
          t-3-inv (if (zero? (.a33 m2))
                    identity-transformation
                    (Transformation. 1 0 0
                                     0 1 0
                                     0 0 (/ 1 (.a33 m2))))
          ;; optimization: no need to transform m's further
          ;; ^Transformation m3 (->> m2
          ;;                         (apply-transform t-3-up-1 ,,,)
          ;;                         (apply-transform t-3-up-2 ,,,)
          ;;                         (apply-transform t-3-inv  ,,,))
          ^Transformation source3 (->> source2
                                       (apply-transform t-3-up-1 ,,,)
                                       (apply-transform t-3-up-2 ,,,)
                                       (apply-transform t-3-inv  ,,,))
          t-2-up-1 (if (zero? (.a12 m2))
                     identity-transformation
                     (Transformation. (/ (.a22 m2) (.a12 m2)) -1 0
                                      0                       1  0
                                      0                       0  1))
          t-2-inv (if (zero? (.a22 m2))
                    identity-transformation
                    (Transformation. 1 0               0
                                     0 (/ 1 (.a22 m2)) 0
                                     0 0               1))
          t-1-inv (if (zero? (.a11 m2))
                    identity-transformation
                    (Transformation. (/ 1 (.a11 m2)) 0 0
                                     0               1 0
                                     0               0 1))
          ;; optimization: no need to transform m's further
          ;; ^Transformation m4 (->> m3
          ;;                        (apply-transform t-2-up-1 ,,,)
          ;;                        (apply-transform t-2-inv  ,,,)
          ;;                        (apply-transform t-1-inv  ,,,))
          ^Transformation source4 (->> source3
                                       (apply-transform t-2-up-1 ,,,)
                                       (apply-transform t-2-inv  ,,,)
                                       (apply-transform t-1-inv  ,,,))]
      source4)))



