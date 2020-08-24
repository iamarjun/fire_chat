package com.arjun.firechat.userList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.databinding.FragmentUserListBinding
import com.arjun.firechat.model.User
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.viewBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class UserListFragment : BaseFragment() {

    @Inject
    internal lateinit var mAuth: FirebaseAuth

    private val currentUserId by lazy { mAuth.currentUser?.uid!! }

    private val userAdapter: UsersListAdapter by lazy {
        UsersListAdapter(object :
            UsersListAdapter.Interaction {
            override fun onItemSelected(position: Int, item: User) {
                val action =
                    UserListFragmentDirections.actionUserListFragmentToChatFragment(item.id)
                requireView().findNavController().navigate(action)
            }

        })
    }

    private val binding: FragmentUserListBinding by viewBinding(FragmentUserListBinding::bind)
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("UserListFragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.setUserPresence(currentUserId)

        viewModel.fetchAllUsers(currentUserId)

        viewModel.allUser.observe(viewLifecycleOwner) {

            binding.loader.isVisible = it is Resource.Loading

            when (it) {

                is Resource.Success -> {
                    it.data?.let { users -> userAdapter.submitList(users) }
                }
                is Resource.Error -> {
                    it.message?.let { errorMessage ->
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.userList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

    }


}