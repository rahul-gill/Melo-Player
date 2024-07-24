package meloplayer.app.preferences

import android.content.Context
import android.graphics.Color

const val DefaultTimeFormat = "hh:mm a"
const val DefaultDateFormat = "d MMM, yyyy"
val DefaultColorSchemeSeed = Color.GREEN


enum class ThemeConfig {
    FollowSystem, Dark, Light
}

enum class DarkThemeType { Dark, Black }

class PreferenceManager(
    context: Context
) {
    //Theming related
    val themeConfig = enumPreference(
        context = context, key = "theme_config", defaultValue = ThemeConfig.FollowSystem
    )
    val darkThemeType = enumPreference(
        context = context, key = "dark_theme_type", defaultValue = DarkThemeType.Dark
    )
    val followSystemColors = BooleanPreference(
        context = context, key = "follow_system_colors", defaultValue = false
    )
    val colorSchemeSeed = IntPreference(
        context = context, key = "color_scheme_type", defaultValue = DefaultColorSchemeSeed
    )

    //Home tabs to show and their order
    //Language
    //safSdCardUri
    //autoDownloadImagesPolicy
}