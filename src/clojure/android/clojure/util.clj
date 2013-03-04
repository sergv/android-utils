(ns android.clojure.util
  (:import [android.app.Activity]
           [android.view.View]

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




