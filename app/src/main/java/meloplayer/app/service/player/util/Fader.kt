package meloplayer.app.service.player.util

import java.util.Timer
import kotlin.math.max
import kotlin.math.min

class Fader(
    val from: Float,
    val to: Float,
    val duration: Int,
    val interval: Int = DEFAULT_INTERVAL,
    val onUpdate: (Float) -> Unit,
    val onFinish: (Boolean) -> Unit,
) {


    private var timer: Timer? = null
    private var ended = false

    fun start() {
        val increments =
            (to - from) * (interval.toFloat() / duration)
        var volume = from
        val isReverse = to < from
        timer = kotlin.concurrent.timer(period = interval.toLong()) {
            if (volume != to) {
                onUpdate(volume)
                volume = if (isReverse) max(to, volume + increments)
                else min(to, volume + increments)
            } else {
                ended = true
                onFinish(true)
                destroy()
            }
        }
    }

    fun stop() {
        if (!ended) onFinish(false)
        destroy()
    }

    private fun destroy() {
        timer?.cancel()
        timer = null
    }

    companion object {
        private const val DEFAULT_INTERVAL = 50

    }
}