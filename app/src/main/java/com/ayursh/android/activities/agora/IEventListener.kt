package com.ayursh.android.activities.agora



interface IEventListener {
    fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int)
    fun onUserJoined(uid: Int, elapsed: Int)
    fun onUserOffline(uid: Int, reason: Int)
    fun onConnectionStateChanged(status: Int, reason: Int)
    fun onPeersOnlineStatusChanged(map: Map<String?, Int?>?)
}