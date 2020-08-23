package com.arjun.firechat.signIn

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import com.afollestad.materialdialogs.DialogBehavior
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.ModalDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.lifecycle.lifecycleOwner
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.databinding.FragmentSignInBinding
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_sign_in_bottom_sheet.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SignInFragment : BaseFragment() {

    private val binding: FragmentSignInBinding by viewBinding(FragmentSignInBinding::bind)
    private val viewModel: MainViewModel by activityViewModels()

    @Inject
    internal lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_sign_in, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val currentUser = mAuth.currentUser

        currentUser?.let {

            navigateToUserListFragment()


        } ?: run {

            showSignInDialog(BottomSheet(LayoutMode.WRAP_CONTENT))

        }


        viewModel.currentUser.observe(viewLifecycleOwner) {

            binding.loader.isVisible = it is Resource.Loading

            when (it) {

                is Resource.Success -> {
                    navigateToUserListFragment()
                }

                is Resource.Error -> {
                    it.message?.let { errorMessage ->
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }

                    showSignInDialog(BottomSheet(LayoutMode.WRAP_CONTENT))
                }
            }
        }

    }

    private fun anonymousSignIn(name: String) {
        mAuth.signInAnonymously()
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Timber.d("signInAnonymously:success")
                    val user = mAuth.currentUser

                    viewModel.addNewUser(name, user)

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

            dialog.dismiss()

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