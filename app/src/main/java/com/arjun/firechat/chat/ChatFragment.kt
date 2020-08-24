package com.arjun.firechat.chat

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.databinding.FragmentChatBinding
import com.arjun.firechat.model.User
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.viewBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber


@AndroidEntryPoint
class ChatFragment : BaseFragment() {

    private val binding: FragmentChatBinding by viewBinding(FragmentChatBinding::bind)
    private val viewModel: MainViewModel by activityViewModels()
    private val args: ChatFragmentArgs by navArgs()

    private val chatUser: User by lazy { args.chatUser }

    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d(chatUser.id)

        setActionBarTitle(chatUser.name)

        val currentUserId = mAuth.currentUser?.uid!!
        val chatUserId = chatUser.id

        chatAdapter = ChatAdapter(currentUserId)

        binding.chatMessages.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }

        viewModel.chatInit(currentUserId, chatUserId)

        viewModel.getChatUserPresence(chatUserId)

        binding.send.setOnClickListener {

            val message = binding.message.text.toString()

            if (!TextUtils.isEmpty(message)) {

                viewModel.sendMessage(currentUserId, chatUserId, message)

                binding.message.text?.clear()

            }
        }

        viewModel.loadMessages(currentUserId, chatUserId)

        viewModel.allMessage.observe(viewLifecycleOwner) {

            when (it) {

                is Resource.Success -> {
                    it.data?.let { messages ->
                        chatAdapter.submitList(messages)
                        chatAdapter.notifyItemInserted(messages.size - 1)
                        binding.chatMessages.scrollToPosition(messages.size - 1)
                    }
                }

                is Resource.Error -> {
                    it.message?.let { errorMessage ->
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        viewModel.chatUserStatus.observe(viewLifecycleOwner) {

            when (it) {

                is Resource.Success -> {
                    it.data?.let { status ->
                        actionbar?.subtitle = status
                    }
                }

                is Resource.Error -> {
                    it.message?.let { errorMessage ->
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

}