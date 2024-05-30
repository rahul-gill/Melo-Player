package meloplayer.app.extensions

import androidx.core.view.WindowInsetsCompat
import meloplayer.app.util.PreferenceUtil
import meloplayer.app.util.RetroUtil

fun WindowInsetsCompat?.getBottomInsets(): Int {
    return if (PreferenceUtil.isFullScreenMode) {
        return 0
    } else {
        this?.getInsets(WindowInsetsCompat.Type.systemBars())?.bottom ?: RetroUtil.navigationBarHeight
    }
}
