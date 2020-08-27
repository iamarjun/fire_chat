package com.arjun.firechat

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    internal lateinit var mAuth: FirebaseAuth

    private val viewModel: MainViewModel by viewModels()
    private val currentUserId by lazy { mAuth.currentUser?.uid }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

    }

    override fun onStart() {
        super.onStart()

        currentUserId?.let { viewModel.setAsOnline(it) }
    }

    override fun onStop() {
        super.onStop()

        currentUserId?.let { viewModel.setAsOffline(it) }
    }

    companion object {

        private const val NOTIFICATION_CHAT_MSG = "NOTIFICATION_CHAT_MSG"

        fun createNotificationIntentForChatMessage(
            context: Context
        ): PendingIntent {
            val resultIntent = createLaunchFreshIntent(context)
            resultIntent.action = NOTIFICATION_CHAT_MSG
            return PendingIntent.getActivity(
                context,
                FireNotificationManager.NOTIFICATION_ID_CHAT_MSG,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun createLaunchFreshIntent(context: Context): Intent {
            //MainActivity has launchMode="singleTop and by creating fresh launch intent? will recreate the activity and onUserLogin wii be called
            val resultIntent = Intent(context, MainActivity::class.java)
            resultIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            return resultIntent
        }

    }

}