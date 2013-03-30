
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
              :implements [android.view.SurfaceHolder$Callback]
              :state drawer_state
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
            TimeUnit]))

(defn- log
  ([msg] (android.util.Log/d "IndependentDrawer" msg))
  ([msg & args] (log (apply format msg args))))



(defrecord DrawerState [surface-available?
                        ^Bitmap drawing-bitmap
                        ^Canvas drawing-canvas
                        ^Thread drawing-thread
                        ^ConcurrentLinkedQueue message-queue])

(defn -drawer-init
  "Initialize drawer state and pass arguments up to super-constructor."
  ([^Context context]
     [[context]
      (DrawerState. (atom nil)
                    (atom nil :meta {:tag Bitmap})
                    (atom nil :meta {:tag Canvas})
                    (atom nil :meta {:tag Thread})
                    (ConcurrentLinkedQueue.))])
  ([^Context context ^AttributeSet attrs]
     [[context attrs]
      (DrawerState. (atom nil)
                    (atom nil :meta {:tag Bitmap})
                    (atom nil :meta {:tag Canvas})
                    (atom nil :meta {:tag Thread})
                    (ConcurrentLinkedQueue.))])
  ([^Context context ^AttributeSet attrs defStyle]
     [[context attrs defStyle]
      (DrawerState. (atom nil)
                    (atom nil :meta {:tag Bitmap})
                    (atom nil :meta {:tag Canvas})
                    (atom nil :meta {:tag Thread})
                    (ConcurrentLinkedQueue.))]))

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



(defn -surfaceChanged [^android.clojure.IndependentDrawer this
                       ^SurfaceHolder holder
                       format
                       width
                       height]
  (reset! (.drawing-bitmap ^DrawerState (.drawer_state this))
          (if-let [orig-bmap ^Bitmap @(.drawing-bitmap ^DrawerState (.drawer_state this))]
            (Bitmap/createScaledBitmap orig-bmap width height true)
            (Bitmap/createBitmap width height android.graphics.Bitmap$Config/ARGB_8888)))
  (reset! (.drawing-canvas ^DrawerState (.drawer_state this))
          (Canvas. @(.drawing-bitmap ^DrawerState (.drawer_state this)))))


(declare make-drawing-thread)

(defn -surfaceCreated [^android.clojure.IndependentDrawer this
                       ^SurfaceHolder holder]
  (reset! (.surface-available? ^DrawerState (.drawer_state this)) true)
  (let [thread ^Thread (make-drawing-thread this)]
    (reset! (.drawing-thread ^DrawerState (.drawer_state this))
            thread)
    (.start thread)))

(defn -surfaceDestroyed [^android.clojure.IndependentDrawer this
                         ^SurfaceHolder holder]
  (reset! (.surface-available? ^DrawerState (.drawer_state this)) false)
  (loop []
    (when (not (try
                 (.join ^Thread @(.drawing-thread ^DrawerState (.drawer_state this)))
                 true
                 (catch InterruptedException e
                   false)))
      (recur))))


(defmacro with-canvas
  "action will be executed with canvas-var bound to drawing canvas whole
contents is stored in drawing bitmap and will be preserved between
different actions.

after-action will be executed with canvas-var bounded to the
resulting canvas with drawing-bitmap drawn on. Content of this canvas will
not be preserved."
  ([canvas-var surface-view action after-action]
     `(with-canvas ~canvas-var ~surface-view ~action ~after-action nil))
  ([canvas-var surface-view action after-action post-action]
     `(if-let [surface-canvas# ^Canvas (.. ~surface-view (getHolder) (lockCanvas))]
        (try
          (let [~canvas-var ^Canvas @(.drawing-canvas ^DrawerState (.drawer_state ~surface-view))]
            ~action)
          (.drawBitmap surface-canvas#
                       ^Bitmap @(.drawing-bitmap ^DrawerState
                                                 (.drawer_state ~surface-view))
                       0.0
                       0.0
                       nil)
          (let [~canvas-var ^Canvas surface-canvas#]
            ~after-action)
          (finally
            ~post-action
            (.. ~surface-view (getHolder) (unlockCanvasAndPost surface-canvas#))))
        (log "Unexpected error: lockCanvas returned nil"))))

(defmacro call-on-func-or-func-seq [f func-or-coll]
  `(when ~func-or-coll
     (if (seq? ~func-or-coll)
       (dorun (map ~f ~func-or-coll))
       (~f ~func-or-coll))))

(defn- ^Thread make-drawing-thread [^android.clojure.IndependentDrawer this]
  (let [msg-queue ^ConcurrentLinkedQueue (.message-queue ^DrawerState (.drawer_state this))]
    (Thread.
     (fn []
       (log "Drawing thread's born")
       (loop []
         (when @(.surface-available? ^DrawerState (.drawer_state this))
           (if-let [msg (.peek msg-queue)]
             (case (msg :type)
               :plain
               (let [{:keys [actions after-actions]} msg]
                 (with-canvas canvas this
                   (call-on-func-or-func-seq #(% canvas) actions)
                   (call-on-func-or-func-seq #(% canvas) after-actions)
                   (.poll msg-queue)))
               :animation
               ;; if type is :animation then anim-actions and anim-after-actions are
               ;; sequences of functions of two argumetns:
               ;; canvas and $$ t \in \left[ 0, 1 \right] $$
               (let [{:keys [anim-actions anim-after-actions duration pause-time]}
                     msg
                     start-time (System/currentTimeMillis)]
                 (with-canvas canvas this
                   (call-on-func-or-func-seq #(% canvas 0) anim-actions)
                   (call-on-func-or-func-seq #(% canvas 0) anim-after-actions))
                 (Thread/sleep pause-time 0)
                 (loop []
                   (let [curr-time (System/currentTimeMillis)
                         diff (- curr-time start-time)]
                     (assert (<= 0 diff))
                     (when (< diff duration)
                       (with-canvas canvas this
                         (call-on-func-or-func-seq #(% canvas (/ diff duration))
                                                   anim-actions)
                         (call-on-func-or-func-seq #(% canvas (/ diff duration))
                                                   anim-after-actions))
                       (Thread/sleep pause-time 0)
                       (recur))))
                 (with-canvas canvas this
                   (call-on-func-or-func-seq #(% canvas 1) anim-actions)
                   (call-on-func-or-func-seq #(% canvas 1) anim-after-actions))
                 (Thread/sleep pause-time 0)
                 ;; completed action or not - remove it anyway
                 (.poll msg-queue)))
             (Thread/sleep 100 0))
           (recur)))
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
           (contains? #{:plain :animation} (msg :type)))
    (.add ^ConcurrentLinkedQueue
          (.message-queue ^DrawerState
                          (.drawer_state drawer))
          msg)
    (log "Error: attempt to send invalid message: %s" msg)))

