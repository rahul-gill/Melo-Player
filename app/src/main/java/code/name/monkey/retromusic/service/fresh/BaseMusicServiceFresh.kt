package code.name.monkey.retromusic.service.fresh

import androidx.media3.session.MediaLibraryService

abstract class BaseMusicServiceFresh : MediaLibraryService() {

    override fun onCreate() {
        super.onCreate()
    }
}

/**

isBinded
isStarted

onCreate --> either onStartCommand or onBind
onDestroy

    startService                                                        bindService
        │                                                                   │
        │                                                                   │
        ↓                                                                   ↓
     onCreate-------------------------callbackOnCreate----------------  onCreate
        │                                                                   │
        ↓                                                                   ↓
  onStartCommand---callbackOnStartCommand+serviceStarted=true            onBind---callbackOnBind+serviceBinded=true
        ┃

        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        ┃  Now here we don't know if service is unbinded?, stopService called?,    ┃
        ┃  stopSelf called?  Either we can ignore why the service is running       ┃
        ┃  Or we can make a wrapper to check if binding is keeping service running ┃
        ┃  or startService. Is it useful? Original code is doing stopSelf on unbind┃
        ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

                            onDestroy------callbackOnDestroy


So either it could be like this:
State: created, destroyed
Callbacks: onCreate, onDestroy, onStartCommand, onBind, onUnBind

Or it could be like this:
State: created, runningWith(startCommand: boolean)

 *
 */