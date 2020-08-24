package com.arjun.firechat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject


abstract class BaseFragment : Fragment() {

    @Inject
    internal lateinit var mAuth: FirebaseAuth

    val currentUser by lazy { mAuth.currentUser }

    val currentUserId by lazy { currentUser?.uid }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    fun setActionBarTitle(mTitle: String = getString(R.string.app_name)) {
        val actionbar = (requireActivity() as AppCompatActivity).supportActionBar
        actionbar?.apply {
            title = mTitle
        }
    }

}