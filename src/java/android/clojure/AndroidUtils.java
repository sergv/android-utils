package android.clojure;

import android.os.Build;
import android.os.Handler;
import android.view.View;

public class AndroidUtils {

public static class SystemUiDimmer implements View.OnSystemUiVisibilityChangeListener {
    private final Handler h;
    private final View parent;
    // Timer timer;
    private final Runnable dim_ui_action;

    SystemUiDimmer(View parent) {
        h = new Handler();
        this.parent = parent;
        // timer = new Timer();
        dim_ui_action = new Runnable() {
            @Override
            public void run() {
                int visibility = SystemUiDimmer.this.parent.getSystemUiVisibility();
                int new_visibility = visibility | View.SYSTEM_UI_FLAG_LOW_PROFILE;
                SystemUiDimmer.this.parent.setSystemUiVisibility(new_visibility);
            }
        };
    }

    /* As soon as system ui changes it's state this "lambda" schedules it's
     * dimming out in 1 second */
    @Override
    public void onSystemUiVisibilityChange(int global_visibility_flag) {
        /* if system ui is not presently in low-profile state */
        if (0 == (global_visibility_flag & View.SYSTEM_UI_FLAG_LOW_PROFILE)) {
            h.postDelayed(dim_ui_action, 1000);
        }
    }
}

public static void make_ui_dimmer(View v) {
      // android < 14 does not support View/SYSTEM_UI_FLAG_LOW_PROFILE
      // android < 11 does not support system ui visibility machinery
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        v.setOnSystemUiVisibilityChangeListener(new SystemUiDimmer(v));
    }
}

}

