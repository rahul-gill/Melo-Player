package meloplayer.app.model.smartplaylist

import androidx.annotation.DrawableRes
import meloplayer.app.R
import meloplayer.app.model.AbsCustomPlaylist

abstract class AbsSmartPlaylist(
    name: String,
    @DrawableRes val iconRes: Int = R.drawable.ic_queue_music
) : AbsCustomPlaylist(
    id = PlaylistIdGenerator(name, iconRes),
    name = name
)