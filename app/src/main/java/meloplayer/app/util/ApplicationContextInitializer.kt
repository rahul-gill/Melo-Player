package meloplayer.app.util


import android.content.Context
import androidx.startup.Initializer
import timber.log.Timber


private var appContext: Context? = null

val applicationContextGlobal
    get() = appContext!!


internal class ApplicationContextInitializer : Initializer<Context> {
    override fun create(context: Context): Context {
        context.applicationContext.also { appContext = it }
        Timber.i("init done")
        return context.applicationContext
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}