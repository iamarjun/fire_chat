package com.arjun.firechat

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import java.util.*
import javax.inject.Inject

class FireNotificationManager @Inject constructor(private val applicationContext: Context) {

    private val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val random = Random(System.currentTimeMillis())

    enum class NotificationType {
        MESSAGE
    }

    init {
        createChannels()
    }

    fun notifyChatMessageReceived(notificationMessage: String, storeId: String?) {
        val builder = computeNotification(notificationMessage, NotificationType.MESSAGE)
        val chatIntent: PendingIntent = MainActivity.createNotificationIntentForChatMessage(
            applicationContext, storeId
        )
        builder.setContentIntent(chatIntent)
        builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
        val tag = NotificationType.MESSAGE.name + notificationMessage.hashCode()
        notificationManager.notify(tag, NOTIFICATION_ID_CHAT_MSG, builder.build())
    }

    fun dismissAllUnhandledNotifications() {
        notificationManager.cancelAll()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                NotificationType.MESSAGE.name,
                R.string.chat_message,
                NotificationManager.IMPORTANCE_HIGH
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        id: String,
        @StringRes descriptionId: Int,
        importance: Int
    ) {
        // The user-visible name of the channel.
        val name: CharSequence = applicationContext.getString(descriptionId)
        // The user-visible description of the channel.
        val description = applicationContext.getString(descriptionId)
        val mChannel = NotificationChannel(id, name, importance)
        // Configure the notification channel.
        mChannel.description = description
        // Sets the notification light color for notifications posted to this
        // channel, if the device supports this feature.
        mChannel.enableVibration(true)
        mChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager.createNotificationChannel(mChannel)
    }

    private fun computeNotification(
        notificationMessage: String,
        notificationType: NotificationType
    ): NotificationCompat.Builder {
        return NotificationCompat.Builder(
            applicationContext,
            applicationContext.getString(R.string.default_notification_channel_id)
        )
            .setSmallIcon(R.drawable.ic_fire)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.ic_fire
                )
            )
            .setAutoCancel(true)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(notificationMessage)
    }

    private fun generateRandomInt(): Int {
        return random.nextInt()
    }

    companion object {
        const val NOTIFICATION_ID_CHAT_MSG = 115
    }
}