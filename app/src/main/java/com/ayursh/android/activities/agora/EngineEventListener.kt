package com.ayursh.android.activities.agora

import io.agora.rtc2.IRtcEngineEventHandler
import java.util.*

class EngineEventListener : IRtcEngineEventHandler() {
    private val mListeners: MutableList<IEventListener> = ArrayList()
    fun registerEventListener(listener: IEventListener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener)
        }
    }

    fun removeEventListener(listener: IEventListener) {
        mListeners.remove(listener)
    }

    override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
        val size = mListeners.size
        if (size > 0) {
            mListeners[size - 1].onJoinChannelSuccess(channel, uid, elapsed)
        }
    }

    override fun onUserJoined(uid: Int, elapsed: Int) {
        val size = mListeners.size
        if (size > 0) {
            mListeners[size - 1].onUserJoined(uid, elapsed)
        }
    }

    override fun onUserOffline(uid: Int, reason: Int) {
        val size = mListeners.size
        if (size > 0) {
            mListeners[size - 1].onUserOffline(uid, reason)
        }
    }

    override fun onConnectionStateChanged(status: Int, reason: Int) {
        val size = mListeners.size
        if (size > 0) {
            mListeners[size - 1].onConnectionStateChanged(status, reason)
        }
    }


}