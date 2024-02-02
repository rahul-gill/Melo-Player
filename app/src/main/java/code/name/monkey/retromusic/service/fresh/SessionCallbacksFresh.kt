package code.name.monkey.retromusic.service.fresh

import android.content.Context
import androidx.media.MediaBrowserServiceCompat
import androidx.media3.common.MediaItem
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.auto.AutoMediaIDHelper
import code.name.monkey.retromusic.auto.AutoMusicProvider
import code.name.monkey.retromusic.auto.fresh.AutoMusicProviderFresh
import code.name.monkey.retromusic.util.PackageValidator
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import org.koin.java.KoinJavaComponent

class SessionCallbacksFresh(
    val context: Context
) : MediaLibraryService.MediaLibrarySession.Callback {
    private val packageValidator by lazy {
        PackageValidator(context, R.xml.allowed_media_browser_callers)
    }
    private val storage: PersistentStorageFresh by lazy {
        PersistentStorageFresh.getInstance(context)
    }
    private val musicProvider: AutoMusicProviderFresh =
        KoinJavaComponent.get(AutoMusicProviderFresh::class.java)

    override fun onGetLibraryRoot(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        // Check origin to ensure we're not allowing any arbitrary app to browse app contents
        return if (!packageValidator.isKnownCaller(browser.packageName, browser.uid)) {
            // Request from an untrusted package: return an empty browser root
            Futures.immediateFuture(
                LibraryResult.ofItem(
                    MediaItem.Builder().setMediaId(AutoMediaIDHelper.MEDIA_ID_EMPTY_ROOT).build(),
                    null
                )
            )
        } else {
            /**
             * By default return the browsable root. Treat the EXTRA_RECENT flag as a special case
             * and return the recent root instead.
             */
            val isRecentRequest =
                browser.connectionHints.getBoolean(MediaBrowserServiceCompat.BrowserRoot.EXTRA_RECENT)
            val browserRootPath =
                if (isRecentRequest) AutoMediaIDHelper.RECENT_ROOT else AutoMediaIDHelper.MEDIA_ID_ROOT
            MediaBrowserServiceCompat.BrowserRoot(browserRootPath, null)
            Futures.immediateFuture(
                LibraryResult.ofItem(
                    MediaItem.Builder().setMediaId(browserRootPath).build(), null
                )
            )
        }
    }


    override fun onGetChildren(
        session: MediaLibraryService.MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val itemList = if (parentId == AutoMediaIDHelper.RECENT_ROOT) {
            listOf(storage.recentSong())
        } else {
            musicProvider.getChildren(parentId, context.resources)
        }
        return Futures.immediateFuture(LibraryResult.ofItemList(itemList, null))
    }
}