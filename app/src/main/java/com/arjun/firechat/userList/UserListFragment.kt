package com.arjun.firechat.userList

import android.os.Bundle
import android.view.*
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
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class UserListFragment : BaseFragment() {

    private val userAdapter: UsersListAdapter by lazy {
        UsersListAdapter(object :
            UsersListAdapter.Interaction {
            override fun onItemSelected(position: Int, item: User) {
                val action =
                    UserListFragmentDirections.actionUserListFragmentToChatFragment(item)
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

        setActionBarTitle("My Chats")

        currentUserId?.let {
            viewModel.setRegistrationToken(it)
            viewModel.setUserPresence(it)
            viewModel.fetchAllUsers(it)
        }


        viewModel.allUsers.observe(viewLifecycleOwner) {

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

        binding.fab.setOnClickListener {
            val action = UserListFragmentDirections.actionUserListFragmentToAllUsersFragment();
            requireView().findNavController().navigate(action)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.settings) {
            val action = UserListFragmentDirections.actionUserListFragmentToSettingsFragment()
            requireView().findNavController().navigate(action)
            return true
        }

        return super.onOptionsItemSelected(item)
    }


}