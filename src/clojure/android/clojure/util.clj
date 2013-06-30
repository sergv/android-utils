(ns android.clojure.util
  (:import [android.R]
           [android.app Activity]
           [android.view View MotionEvent]

           [android.clojure AndroidUtils]

           [java.util.Timer]
           [java.util.TimerTask]))


(defn make-ui-dimmer
  "Note: care should be taken to call this on activity's ui thread."
  [^View view]
  (AndroidUtils/make_ui_dimmer view))

(defn make-double-tap-handler
  ([f]
     (make-double-tap-handler f 500))
  ([f recognition-time]
     (let [last-time (atom nil)]
       (reify android.view.View$OnTouchListener
         (^boolean onTouch [this
                            ^View view
                            ^MotionEvent event]
           (cond (= MotionEvent/ACTION_DOWN (.getActionMasked event))
                 (let [current-time (System/currentTimeMillis)]
                   (if @last-time
                     (if (< (java.lang.Math/abs ^long (- @last-time
                                                         current-time))
                            recognition-time)
                       (do (f)
                           (reset! last-time nil)
                           true)
                       (do (reset! last-time current-time)
                           false))
                     (do (reset! last-time current-time)
                         false)))
                 :else
                 false))))))

(defmacro defrecord* [record-name fields & defrecord-opts]
  (let [field-accessors
        (map (fn [field-name]
               (let [type (:tag (meta field-name))
                     record-var (with-meta (gensym "var")
                                  {:tag record-name})
                     field-ref (symbol (str "." field-name))]
                 `(defn ~field-name
                    ~(with-meta
                       (vector record-var)
                       {:tag type
                        ;; doesn't work yet
                        ;; :inline
                        ;; (with-meta (eval `(fn [var#] (list (quote ~field-ref) var#)))
                        ;;   {:tag type})
                        })
                    (~field-ref ~record-var))))
             fields)]
    `(do
       (defrecord ~record-name ~fields ~@defrecord-opts)
       ~@field-accessors)))

(defmacro android-resource
  ([id] `(resource :id ~id))
  ([resource-type resource-name]
     `(. ~(case resource-type
            :anim         android.R$anim
            ;; :animator     android.R$animator
            :array        android.R$array
            :attr         android.R$attr
            :bool         android.R$bool
            :color        android.R$color
            :dimen        android.R$dimen
            :drawable     android.R$drawable
            ;; :fraction     android.R$fraction
            :id           android.R$id
            :integer      android.R$integer
            ;; :interpolator android.R$interpolator
            :layout       android.R$layout
            ;; :menu         android.R$menu
            ;; :mipmap       android.R$mipmap
            :plurals      android.R$plurals
            :raw          android.R$raw
            :string       android.R$string
            :style        android.R$style
            :xml          android.R$xml
            (throw (RuntimeException.
                    (str "invalid android resource type: " resource-type))))
         ~(symbol (name resource-name)))))

;; (defn make-async-task [^Activity activity
;;                        on-start
;;                        update
;;                        on-end]
;;   (let [run-on-ui-thread (fn [action]
;;                            (. activity runOnUiThread action))]
;;     (fn []
;;       (run-on-ui-thread on-start)
;;       (doseq [i (take 10 (range))]
;;         (Thread/sleep 25 0)
;;         (run-on-ui-thread update))
;;       (run-on-ui-thread on-end))))
