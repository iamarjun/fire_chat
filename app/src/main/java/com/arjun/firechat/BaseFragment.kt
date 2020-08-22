package com.arjun.firechat

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import timber.log.Timber


abstract class BaseFragment : Fragment() {

    private var mAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
    }

    override fun onStart() {
        super.onStart()

        val currentUser = mAuth?.currentUser

        currentUser?.let {

        } ?: run {
            mAuth?.signInAnonymously()
                ?.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Timber.d("signInAnonymously:success")
                        val user = mAuth?.currentUser
                        Toast.makeText(
                            requireContext(), "Authentication Success.",
                            Toast.LENGTH_SHORT
                        ).show()
                        Timber.d("User $user")
                    } else {
                        // If sign in fails, display a message to the user.
                        Timber.w("signInAnonymously:failure  ${task.exception}")
                        Toast.makeText(
                            requireContext(), "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
//                        updateUI(null)
                    }
                }
        }

    }
}