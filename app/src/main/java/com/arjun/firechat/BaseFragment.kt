package com.arjun.firechat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject


abstract class BaseFragment : Fragment() {

    @Inject
    internal lateinit var mAuth: FirebaseAuth

    val currentUser by lazy { mAuth.currentUser }

    val currentUserId by lazy { currentUser?.uid }

    val actionbar by lazy { (requireActivity() as AppCompatActivity).supportActionBar }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setActionBarTitle()
    }

    fun setActionBarTitle(mTitle: String = getString(R.string.app_name)) {
        actionbar?.apply {
            title = mTitle
        }
    }

}