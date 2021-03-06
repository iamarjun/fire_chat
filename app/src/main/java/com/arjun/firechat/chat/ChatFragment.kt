package com.arjun.firechat.chat

import android.Manifest
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.blockUnblock.BlockUnblockFragment
import com.arjun.firechat.databinding.FragmentChatBinding
import com.arjun.firechat.model.User
import com.arjun.firechat.util.EventObserver
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
    private var endOfRecyclerView = true
    private val chatUser: User by lazy { args.chatUser }

    private var isBlocked = false
    private lateinit var chatAdapter: ChatAdapter

    private val getPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            permission.values.forEach {
                if (!it) {
                    Snackbar.make(
                        requireView(),
                        "Please grant necessary permissions",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    return@registerForActivityResult
                }
            }

            getContent.launch("image/*")
        }

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) {
        Timber.d("uri: $it")

        it?.let { uri ->
            viewModel.sendMediaMessage(currentUserId!!, chatUser.id, uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

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

        val currentUserId = this.currentUserId!!
        val chatUserId = chatUser.id

        viewModel.getChatUserBlockedUsers(chatUserId)

        viewModel.myBlockedUserIds.value?.let {
            isBlocked = it.contains(chatUserId)
            binding.messagingPanel.isVisible = !isBlocked
        }

        viewModel.chatUserBlockedUserIds.value?.let {
            if (it.contains(currentUserId)) {
                findNavController().popBackStack(R.id.userListFragment, true)
                Toast.makeText(requireActivity(), "You're blocked by this user.", Toast.LENGTH_LONG)
                    .show()
            }
        }

        chatAdapter = ChatAdapter(currentUserId)

        chatAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.chatMessages.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    endOfRecyclerView = !recyclerView.canScrollVertically(1)
                }
            })
        }

        viewModel.chatInit(currentUserId, chatUserId)

        viewModel.getChatUserPresence(chatUserId)

        viewModel.monitorChatUserStatusWithMe(currentUserId, chatUserId)

        binding.send.setOnClickListener {

            val message = binding.message.text.toString()

            if (!TextUtils.isEmpty(message)) {

                val chatUserStatus = viewModel.isChatUserOnline.value

                chatUserStatus?.let {
                    if (!it)
                        viewModel.sendNotification(currentUserId, chatUserId, message)
                }

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
                        if (endOfRecyclerView)
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

        viewModel.lastUpdatedMessageIndex.observe(viewLifecycleOwner) {
            chatAdapter.notifyItemChanged(it)
        }

        viewModel.chatUserStatus.observe(viewLifecycleOwner) {

            when (it) {

                is Resource.Success -> {
                    it.data?.let { status ->
                        setActionBarSubtitle(status)
                    }
                }

                is Resource.Error -> {
                    it.message?.let { errorMessage ->
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.add.setOnClickListener {
            getPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        viewModel.popBack.observe(viewLifecycleOwner, EventObserver {
            findNavController().popBackStack(R.id.userListFragment, true)
        })

    }

    override fun onStart() {
        super.onStart()

        viewModel.updateMyStatusWithChatUser(currentUserId, chatUser.id, true)
    }

    override fun onStop() {
        super.onStop()

        viewModel.updateMyStatusWithChatUser(currentUserId, chatUser.id, false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (isBlocked)
            inflater.inflate(R.menu.unblock, menu)
        else
            inflater.inflate(R.menu.block, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        showDialog()
        return super.onOptionsItemSelected(item)
    }

    private fun showDialog() {
        val newFragment: DialogFragment =
            BlockUnblockFragment.newInstance(currentUserId!!, chatUser, isBlocked)
        newFragment.show(childFragmentManager, "dialog")
    }

}