package com.arjun.firechat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
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


}