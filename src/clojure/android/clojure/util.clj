(ns android.clojure.util
  (:import [android.app Activity]
           [android.view View MotionEvent]

           [java.util.Timer]
           [java.util.TimerTask]))



(defn make-ui-dimmer [^android.app.Activity activity
                      ^android.view.View view]
  (let [change-visibility (fn []
                            (. view
                               setSystemUiVisibility
                               android.view.View/SYSTEM_UI_FLAG_LOW_PROFILE))
        timer (new java.util.Timer)]
    (change-visibility)
    (. view
       setOnSystemUiVisibilityChangeListener
       (proxy [android.view.View$OnSystemUiVisibilityChangeListener] []
         (onSystemUiVisibilityChange [global-flags]
           (when (= 0 (bit-and global-flags
                               android.view.View/SYSTEM_UI_FLAG_LOW_PROFILE))
             (. timer
                schedule
                (proxy [java.util.TimerTask] []
                   (run []
                     (. activity runOnUiThread change-visibility)))
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




