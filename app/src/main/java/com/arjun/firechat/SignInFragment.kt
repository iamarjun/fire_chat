package com.arjun.firechat

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.DialogBehavior
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.ModalDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.arjun.firechat.databinding.FragmentSignInBinding
import kotlinx.android.synthetic.main.fragment_sign_in_bottom_sheet.*
import timber.log.Timber

class SignInFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentSignInBinding.inflate(layoutInflater, container, false).root
    }

    override fun onStart() {
        super.onStart()

        val currentUser = mAuth?.currentUser

        currentUser?.let {

            navigateToUserListFragment()


        } ?: run {

            showSignInDialog(BottomSheet(LayoutMode.WRAP_CONTENT))

        }

    }

    private fun anonymousSignIn(name: String) {
        mAuth?.signInAnonymously()
            ?.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Timber.d("signInAnonymously:success")
                    val user = mAuth?.currentUser

                    user?.let {
                        val db = getUser(it.uid)

                        val userMap = hashMapOf("name" to name)

                        db?.setValue(userMap)

                        navigateToUserListFragment()
                    }

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
                }
            }
    }

    private fun navigateToUserListFragment() {
        val action =
            SignInFragmentDirections.actionSignInFragmentToUserListFragment()
        requireView().findNavController().navigate(action)
    }

    private fun showSignInDialog(dialogBehavior: DialogBehavior = ModalDialog) {
        val dialog = MaterialDialog(requireContext(), dialogBehavior).show {
            title(R.string.sign_in)
            customView(
                R.layout.fragment_sign_in_bottom_sheet,
                scrollable = true,
                horizontalPadding = true
            )
            cornerRadius(20f)
            lifecycleOwner(this@SignInFragment)
            debugMode(false)
        }

        val name = dialog.name
        val submit = dialog.btn_submit

        submit.setOnClickListener {
            if (!TextUtils.isEmpty(name.text))
                anonymousSignIn(name = name.text.toString())
            else
                Toast.makeText(
                    requireContext(), "Enter your name to continue",
                    Toast.LENGTH_SHORT
                ).show()

        }
    }

}