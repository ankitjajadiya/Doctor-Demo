package com.ayursh.android.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
        val service = Restarter()
        service.enqueueWork(p0, p1)
    }
}