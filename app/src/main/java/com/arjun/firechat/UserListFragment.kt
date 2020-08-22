package com.arjun.firechat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.arjun.firechat.databinding.FragmentUserListBinding
import timber.log.Timber

class UserListFragment : BaseFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("UserListFragment")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentUserListBinding.inflate(layoutInflater, container, false).root
    }

    override fun onStart() {
        super.onStart()
    }

}