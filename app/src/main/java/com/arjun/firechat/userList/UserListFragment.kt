package com.arjun.firechat.userList

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.databinding.FragmentUserListBinding
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.viewBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class UserListFragment : BaseFragment() {

    private val userAdapter: UsersListAdapter by lazy { UsersListAdapter() }
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

        viewModel.fetchAllUsers()

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

    override fun onStart() {
        super.onStart()
    }

}