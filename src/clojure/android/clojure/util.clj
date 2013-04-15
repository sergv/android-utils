(ns android.clojure.util
  (:import [android.app Activity]
           [android.view View MotionEvent]

           [java.util.Timer]
           [java.util.TimerTask]))


(defn make-ui-dimmer [^Activity activity
                      ^View view]
  (let [dim-ui (fn []
                 (.setSystemUiVisibility view
                                         View/SYSTEM_UI_FLAG_LOW_PROFILE))
        timer (java.util.Timer.)]
    (dim-ui)
    (. view
       setOnSystemUiVisibilityChangeListener
       (reify
         android.view.View$OnSystemUiVisibilityChangeListener
         (onSystemUiVisibilityChange [this global-visibility-flags]
           (when (= 0 (bit-and global-visibility-flags
                               View/SYSTEM_UI_FLAG_LOW_PROFILE))
             (. timer
                schedule
                (proxy [java.util.TimerTask] []
                  (run []
                    (.runOnUiThread activity dim-ui)))
                1000)))))))


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

