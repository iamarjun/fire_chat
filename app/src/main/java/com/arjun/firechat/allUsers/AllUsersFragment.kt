package com.arjun.firechat.allUsers

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.databinding.FragmentAllUsersBinding
import com.arjun.firechat.model.User
import com.arjun.firechat.userList.UserListFragmentDirections
import com.arjun.firechat.userList.UsersListAdapter
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.viewBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class AllUsersFragment : BaseFragment() {

    private val binding: FragmentAllUsersBinding by viewBinding(FragmentAllUsersBinding::bind)
    private val viewModel: MainViewModel by activityViewModels()

    private val userAdapter: UsersListAdapter by lazy {
        UsersListAdapter(object :
            UsersListAdapter.Interaction {
            override fun onItemSelected(position: Int, item: User) {
                val action =
                    UserListFragmentDirections.actionUserListFragmentToChatFragment(item)
                findNavController().navigate(action)
            }

        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("AllUsersFragment")
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_all_users, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setActionBarTitle("All Users")

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

    }

}