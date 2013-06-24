
;; date:   Saturday, 30 March 2013
;; author: Sergey Vinokurov
;; email:  serg.foo@gmail.com

(ns android.clojure.IndependentDrawer
  "Independent drawer class providing Surface and separate thread that
can receive drawing commands and \"execute\" them on the Surface.

Drawer maintains separate bitmap where most of drawing commands will be
executed on this bitmap and only after the bitmap will be drawn on
the surface. This, contents of the bitmap will be preserved between
drawing commands invokations.

Other type of drawing commands is commands that will be executed
on the Surface and whose results will not be preserved."
  (:gen-class :main no
              :name android.clojure.IndependentDrawer
              :extends android.view.SurfaceView
              :implements [clojure.lang.IDeref
                           android.view.SurfaceHolder$Callback]
              :state state
              :init drawer-init
              :post-init drawer-post-init)
  (:import [android.content Context]
           [android.graphics Bitmap Canvas Color Paint Rect]
           [android.view MotionEvent SurfaceHolder SurfaceView View Window]
           [android.util AttributeSet]

           [android.util.Log]

           [java.util.concurrent
            ConcurrentLinkedQueue
            LinkedBlockingQueue
            ThreadPoolExecutor
            TimeUnit])
  (:use [clojure.test :only (function?)]
        [android.clojure.util :only (defrecord*)]))

(defn- log
  ([msg] (android.util.Log/d "IndependentDrawer" msg))
  ([msg & args] (log (apply format msg args))))



(defrecord* DrawerState [surface-available?
                         ^Bitmap drawing-bitmap
                         ^Canvas drawing-canvas
                         ^Thread drawing-thread
                         ^ConcurrentLinkedQueue message-queue])

;; (defn surface-available?
;;   {:tag boolean
;;    ;; :inline ^boolean (fn [state] `(.surface-available? ^DrawerState ~state))
;;    }
;;   [^DrawerState state]
;;   (.surface-available? state))
;;
;; (defn drawing-bitmap
;;   ^{:tag Bitmap
;;     :inline ^Bitmap (fn [state] `(.drawing-bitmap ^DrawerState ~state))}
;;   [^DrawerState state]
;;   (.drawing-bitmap state))
;;
;; (defn drawing-canvas
;;   ^{:tag Canvas
;;     :inline ^Canvas (fn [state] `(.drawing-canvas ^DrawerState ~state))}
;;   [^DrawerState state]
;;   (.drawing-canvas state))
;;
;; (defn drawing-thread
;;   ^{:tag Thread
;;     :inline ^Thread (fn [state] `(.drawing-thread ^DrawerState ~state))}
;;   [^DrawerState state]
;;   (.drawing-thread state))
;;
;; (defn message-queue
;;   ^{:tag ConcurrentLinkedQueue
;;     :inline
;;     ^ConcurrentLinkedQueue (fn [state] `(.message-queue ^DrawerState ~state))}
;;   [^DrawerState state]
;;   (.message-queue state))


(defn -drawer-init
  "Initialize drawer state and pass arguments up to super-constructor."
  ([^Context context]
     [[context]
      (atom (map->DrawerState {:surface-available? false
                               :drawing-bitmap nil
                               :drawing-canvas nil
                               :drawing-thread nil
                               :message-queue (ConcurrentLinkedQueue.)}))])
  ([^Context context ^AttributeSet attrs]
     [[context attrs]
      (atom (map->DrawerState {:surface-available? false
                               :drawing-bitmap nil
                               :drawing-canvas nil
                               :drawing-thread nil
                               :message-queue (ConcurrentLinkedQueue.)}))])
  ([^Context context ^AttributeSet attrs defStyle]
     [[context attrs defStyle]
      (atom (map->DrawerState {:surface-available? false
                               :drawing-bitmap nil
                               :drawing-canvas nil
                               :drawing-thread nil
                               :message-queue (ConcurrentLinkedQueue.)}))]))

(defn -drawer-post-init
  "Register surface callback for managing surface lifecycle."
  ([^android.clojure.IndependentDrawer this
    ^Context context]
     (.. this (getHolder) (addCallback this)))
  ([^android.clojure.IndependentDrawer this
    ^Context context ^AttributeSet attrs]
     (-drawer-post-init this context))
  ([^android.clojure.IndependentDrawer this
    ^Context context ^AttributeSet attrs defStyle]
     (-drawer-post-init this context)))

(defn ^DrawerState -deref [^android.clojure.IndependentDrawer this]
  @(.state this))


(defn -surfaceChanged [^android.clojure.IndependentDrawer this
                       ^SurfaceHolder holder
                       format
                       width
                       height]
  (let [new-bitmap
        (if-let [orig-bmap (drawing-bitmap @this)]
          (Bitmap/createScaledBitmap orig-bmap width height true)
          (Bitmap/createBitmap width height android.graphics.Bitmap$Config/ARGB_8888))]
    (swap! (.state this)
           assoc
           :drawing-bitmap new-bitmap
           :drawing-canvas (Canvas. new-bitmap))))


(declare make-drawing-thread)

(defn -surfaceCreated [^android.clojure.IndependentDrawer this
                       ^SurfaceHolder holder]
  (swap! (.state this)
         assoc
         :surface-available? true)
  (let [thread ^Thread (make-drawing-thread this)]
    (swap! (.state this)
           assoc
           :drawing-thread thread)
    (.start thread)))

(defn -surfaceDestroyed [^android.clojure.IndependentDrawer this
                         ^SurfaceHolder holder]
  (swap! (.state this)
         assoc
         :surface-available? false)
  (while (not (try
                (.join (drawing-thread @this))
                true
                (catch InterruptedException e
                  false)))))


(defmacro ^{:private true} with-canvas
  "The action will be executed with canvas-var bound to drawing canvas
whose contents is stored in drawing bitmap and will be preserved between
different actions.

after-action will be executed with canvas-var bounded to the
resulting canvas with drawing-bitmap drawn on. Content of this canvas will
not be preserved."
  ([canvas-var surface-view action after-action]
     `(if-let [surface-canvas# ^Canvas (.. ~surface-view (getHolder) (lockCanvas))]
        (try
          (let [~canvas-var (drawing-canvas @~surface-view)]
            (assert ~canvas-var "error: drawing-canvas of surface view is nil")
            ~action)
          (.drawBitmap surface-canvas#
                       (drawing-bitmap @~surface-view)
                       0.0
                       0.0
                       nil)
          (let [~canvas-var ^Canvas surface-canvas#]
            (assert ~canvas-var)
            ~after-action)
          (finally
            (.. ~surface-view (getHolder) (unlockCanvasAndPost surface-canvas#))))
        ;; if it returned nil we have nothing to do
        (log "Drawing thread: lockCanvas returned nil"))))

(defmacro ^{:private true} call-on-func-or-func-seq [f func-or-coll]
  `(when ~func-or-coll
     (if (function? ~func-or-coll)
       (~f ~func-or-coll)
       (doseq [item# ~func-or-coll]
         (~f item#)))))

(defmacro ^{:private true} measure-frame-time [msg & body]
  `(let [frame-start-time# (System/currentTimeMillis)]
     ~@body
     (log ~msg (- (System/currentTimeMillis) frame-start-time#))))

(defn- ^Thread make-drawing-thread [^android.clojure.IndependentDrawer this]
  (let [msg-queue (message-queue @this)]
    (Thread.
     (fn []
       (log "Drawing thread's born")
       ;; Wait while surfaceChanged will set up drawing canvas for us.
       (while (not (drawing-canvas @this))
         (Thread/sleep 25 0))
       (log "Drawing thread's ready to draw")
       (while (surface-available? @this)
         ;; note: If exceptions happens when we're processing this message -
         ;; let it be, it's removed from queue here and there's no other
         ;; way around. If you try other ways around make sure they're
         ;; correct with respect to concurrent clear-drawing-queue invokations.
         (if-let [msg (.poll msg-queue)]
           (do
             (case (msg :type)
               :plain
               (let [{:keys [actions after-actions]} msg]
                 (measure-frame-time
                  "Drawing thread, plain frame took %s ms"
                  (with-canvas canvas this
                    (call-on-func-or-func-seq #(% canvas) actions)
                    (call-on-func-or-func-seq #(% canvas) after-actions))))
               :animation
               ;; If type is :animation then anim-actions and anim-after-actions are
               ;; sequences of functions of two arguments:
               ;; a canvas and $$ t \in \left[ 0, 1 \right] $$.
               (let [{:keys [anim-actions anim-after-actions duration pause-time]}
                     msg
                     start-time (System/currentTimeMillis)]
                 (when-not (= 0 duration)
                   (measure-frame-time
                    "Drawing thread, animation frame took %s ms"
                    (with-canvas canvas this
                      (call-on-func-or-func-seq #(% canvas 0) anim-actions)
                      (call-on-func-or-func-seq #(% canvas 0) anim-after-actions)))
                   (Thread/sleep pause-time 0)
                   (loop []
                     (let [curr-time (System/currentTimeMillis)
                           diff (- curr-time start-time)]
                       (assert (<= 0 diff))
                       (when (< diff duration)
                         (measure-frame-time
                          "Drawing thread, animation frame took %s ms"
                          (with-canvas canvas this
                            (call-on-func-or-func-seq #(% canvas (/ diff duration))
                                                      anim-actions)
                            (call-on-func-or-func-seq #(% canvas (/ diff duration))
                                                      anim-after-actions)))
                         (Thread/sleep pause-time 0)
                         (recur)))))
                 (measure-frame-time
                  "Drawing thread, animation frame took %s ms"
                  (with-canvas canvas this
                    (call-on-func-or-func-seq #(% canvas 1) anim-actions)
                    (call-on-func-or-func-seq #(% canvas 1) anim-after-actions)))
                 (Thread/sleep pause-time 0))))
           (Thread/sleep 100 0)))
       (log "Drawing thread dies"))
     (format "Independent drawer thread, %s" this))))


;;;; user-visible API

(defn send-drawing-command
  "Send drawing command to particular drawer. Drawing command is a map with
:type key one of :plain or :animation

If type is :plain then :actions and :after-actions keys may be provided.

If type is :animation then :anim-actions, :anim-after-actions, :duration and
:pause time keys may be provided.

:actions is a single function or a sequence of functions of one canvas
         argument that will be executed on intermediate bitmap and thus will
         be preserved across different invokations

:after-actions is a single function a sequence of functions of one canvas
               argument that will be executed on final Surface so that their
               results would not be preserved

:anim-actions and :anim-after-actions work just like :actions and :after-actions
but take two arguments: drawing canvas and time argument from [0, 1] real interval.

:duration animation duration in milliseconds

:pause amount of time to wait after execution of each animation command

"
  [^android.clojure.IndependentDrawer drawer
   msg]
  (if (and (contains? msg :type)
           (or (and (= :plain (msg :type))
                    (some #{:actions :after-actions} (keys msg)))
               (and (= :animation (msg :type))
                    (some #{:anim-actions :anim-after-actions} (keys msg)))))
    (.add (message-queue @drawer) msg)
    (log "Error: attempt to send invalid message: %s" msg)))

(defn clear-drawing-queue!
  "Remove all pending drawing commands from message queue."
  [^android.clojure.IndependentDrawer drawer]
  (.clear (message-queue @drawer)))

