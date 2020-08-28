package com.arjun.firechat.blockUnblock

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.model.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class BlockUnblockFragment : DialogFragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private val currentUserId: String? by lazy { arguments?.getString("currentUserId") }
    private val chatUser: User? by lazy { arguments?.getParcelable("user") }
    private val isBlocked: Boolean by lazy { arguments?.getBoolean("status") ?: false }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val title = if (isBlocked)
            "Are you sure you want to unblock ${chatUser?.name}?"
        else
            "Are you sure you want to block ${chatUser?.name}?"


        val builder = MaterialAlertDialogBuilder(requireContext())
        return builder
            .setMessage(title)
            .setNegativeButton("No") { _, _ ->
                dismiss()
            }
            .setPositiveButton("Yes") { _, _ ->
                if (isBlocked)
                    viewModel.unblockUser(currentUserId!!, chatUser?.id!!)
                else
                    viewModel.blockUser(currentUserId!!, chatUser?.id!!)
            }
            .create()
    }

    companion object {
        fun newInstance(
            currentUserId: String,
            chatUser: User,
            isBlocked: Boolean
        ): BlockUnblockFragment {
            val frag = BlockUnblockFragment()
            val args = Bundle()
            args.putString("currentUserId", currentUserId)
            args.putParcelable("user", chatUser)
            args.putBoolean("status", isBlocked)
            frag.arguments = args
            return frag
        }
    }
}