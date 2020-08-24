package com.arjun.firechat.settings

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.arjun.firechat.BaseFragment
import com.arjun.firechat.GlideApp
import com.arjun.firechat.MainViewModel
import com.arjun.firechat.R
import com.arjun.firechat.databinding.FragmentSettingsBinding
import com.arjun.firechat.model.User
import com.arjun.firechat.util.FileUtils
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.viewBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : BaseFragment() {

    @Inject
    internal lateinit var fileUtils: FileUtils

    private val viewModel: MainViewModel by activityViewModels()
    private val binding: FragmentSettingsBinding by viewBinding(FragmentSettingsBinding::bind)

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
        Timber.d(it.toString())
        setProfilePicture(it)

        currentUserId?.let { uId ->
            viewModel.uploadAndUpdateProfilePicture(
                uId,
                it
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setActionBarTitle("Profile")

        viewModel.currentUser.observe(viewLifecycleOwner) {

            binding.loader.isVisible = it is Resource.Loading

            when (it) {

                is Resource.Success -> {
                    it.data?.let { user -> setUserProfile(user) }
                }
                is Resource.Error -> {
                    it.message?.let { errorMessage ->
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.changeProfilePicture.setOnClickListener {
            getPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

        viewModel.pictureUploadStatus.observe(viewLifecycleOwner) {

            when (it) {

                is Resource.Success -> {
                    Snackbar.make(requireView(), "Profile Picture Updated", Snackbar.LENGTH_SHORT)
                        .show()
                }
                is Resource.Error -> {
                    it.message?.let { errorMessage ->
                        Snackbar.make(requireView(), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }

    private fun setProfilePicture(uri: Uri) {
        GlideApp.with(this)
            .load(uri)
            .placeholder(R.drawable.avatar)
            .into(binding.profilePicture)
    }

    private fun setUserProfile(user: User) {

        binding.name.text = user.name

        GlideApp.with(this)
            .load(user.image)
            .placeholder(R.drawable.avatar)
            .into(binding.profilePicture)

    }
}