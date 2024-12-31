package com.ayursh.android.models

import android.content.Intent
import com.ayursh.android.activities.agora.ChatActivity

class MessageModel(
    var from_uid: String,
    var to_uid: String,
    var message: String,
    var type: String,
    var sendAt: Long,
    var isDeleted: Boolean,
    var isShown: Boolean,
    var intent: Intent?
) {

    constructor() : this("", "", "", "", 0, false,false, ChatActivity.intentchat) {

    }
}