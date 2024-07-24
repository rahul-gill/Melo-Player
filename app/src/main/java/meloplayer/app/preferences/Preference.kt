package meloplayer.app.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking


private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

interface Preference<T> {
    fun setValue(value: T)
    val key: String
    val defaultValue: T
    val observableValue: Flow<T>
    val value: T
        get() = runBlocking {
            observableValue.firstOrNull() ?: defaultValue
        }

//    @Composable
//    fun asState() = observableValue.collectAsStateWithLifecycle(initialValue = value)
}

/**
 * Handle default values carefully, just like in enumPreference
 */
fun <T, BackingT> customPreference(
    context: Context,
    backingPref: Preference<BackingT>,
    defaultValue: T,
    serialize: (T) -> BackingT,
    deserialize: (BackingT) -> T
) = object : Preference<T> {

    override val key = backingPref.key
    override val defaultValue = defaultValue
    override val observableValue: Flow<T>
        get() = backingPref.observableValue.map(deserialize)

    override fun setValue(value: T) {
        backingPref.setValue(serialize(value))
    }
}

inline fun <reified T : Enum<T>> enumPreference(
    context: Context, key: String, defaultValue: T
) = customPreference(context,
    backingPref = IntPreference(context, key, Int.MAX_VALUE),
    defaultValue,
    serialize = { it.ordinal },
    deserialize = {
        if (it == Int.MAX_VALUE) {
            defaultValue
        } else {
            enumValues<T>().getOrNull(it) ?: defaultValue

        }
    })

class IntPreference(
    private val context: Context,

    override val key: String, override val defaultValue: Int
) : Preference<Int> {

    private val backingKey = intPreferencesKey(key)
    override fun setValue(value: Int) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[backingKey] = value
            }
        }
    }

    override val observableValue: Flow<Int>
        get() = context.dataStore.data.map { pref ->
            pref[backingKey] ?: defaultValue
        }
}

class BooleanPreference(
    private val context: Context,

    override val key: String, override val defaultValue: Boolean
) : Preference<Boolean> {

    private val backingKey = booleanPreferencesKey(key)
    override fun setValue(value: Boolean) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[backingKey] = value
            }
        }
    }

    override val observableValue: Flow<Boolean>
        get() = context.dataStore.data.map { pref ->
            pref[backingKey] ?: defaultValue
        }
}


class LongPreference(
    private val context: Context,

    override val key: String, override val defaultValue: Long
) : Preference<Long> {

    private val backingKey = longPreferencesKey(key)
    override fun setValue(value: Long) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[backingKey] = value
            }
        }
    }

    override val observableValue: Flow<Long>
        get() = context.dataStore.data.map { pref ->
            pref[backingKey] ?: defaultValue
        }
}

class StringPreference(
    private val context: Context,

    override val key: String, override val defaultValue: String
) : Preference<String> {

    private val backingKey = stringPreferencesKey(key)
    override fun setValue(value: String) {
        runBlocking {
            context.dataStore.edit { prefs ->
                prefs[backingKey] = value
            }
        }
    }

    override val observableValue: Flow<String>
        get() = context.dataStore.data.map { pref ->
            pref[backingKey] ?: defaultValue
        }
}