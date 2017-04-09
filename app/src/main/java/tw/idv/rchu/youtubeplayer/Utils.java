package tw.idv.rchu.youtubeplayer;

import android.os.Build;
import android.widget.ImageButton;

import java.util.Formatter;
import java.util.Locale;

/**
 * Class containing some static utility methods.
 */
class Utils {
    static final int ALPHA_ENABLE = 255;
    static final int ALPHA_DISABLE = 77;

    static boolean hasKitkat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    static String stringForTime(int timeMs) {
        StringBuilder formatBuilder = new StringBuilder();
        Formatter formatter = new Formatter(formatBuilder, Locale.getDefault());

        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;
        formatBuilder.setLength(0);
        if (hours > 0) {
            return formatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return formatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    static void updateButtonStatus(ImageButton button, boolean enabled) {
        if (button.isEnabled() != enabled) {
            button.setEnabled(enabled);
            button.setClickable(enabled);

            button.setImageAlpha(enabled ? ALPHA_ENABLE : ALPHA_DISABLE);
        }
    }
}
