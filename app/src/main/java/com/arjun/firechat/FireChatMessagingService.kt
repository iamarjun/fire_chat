package com.arjun.firechat

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FireChatMessagingService : FirebaseMessagingService() {

    @Inject
    internal lateinit var notificationManager: FireNotificationManager


    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Timber.d("Refreshed token: $p0")
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

        Timber.d("Notification Payload: $p0")

        notificationManager.notifyChatMessageReceived("hahahahahhahaha", "")

    }

}