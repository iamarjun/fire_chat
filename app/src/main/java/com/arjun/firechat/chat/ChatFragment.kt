package com.arjun.firechat.chat

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.databinding.FragmentChatBinding
import com.arjun.firechat.util.viewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : BaseFragment() {

    @Inject
    internal lateinit var mAuth: FirebaseAuth

    @Inject
    internal lateinit var mDatabase: FirebaseDatabase

    private val binding: FragmentChatBinding by viewBinding(FragmentChatBinding::bind)
    private val viewModel: MainViewModel by activityViewModels()

    private val args: ChatFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d(args.userId)

        val currentUserId = mAuth.currentUser?.uid!!
        val chatUserId = args.userId

        viewModel.chatInit(currentUserId, chatUserId)

        binding.send.setOnClickListener {

            val message = binding.message.text.toString()

            if (!TextUtils.isEmpty(message)) {

                viewModel.sendMessage(currentUserId, chatUserId, message)

            }
        }

    }


}