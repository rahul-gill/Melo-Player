package meloplayer.app.interfaces

import android.view.View
import meloplayer.app.model.Genre

interface IGenreClickListener {
    fun onClickGenre(genre: Genre, view: View)
}