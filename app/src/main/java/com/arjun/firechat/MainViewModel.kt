package com.arjun.firechat

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arjun.firechat.model.User
import com.arjun.firechat.util.Resource
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import timber.log.Timber

class MainViewModel @ViewModelInject constructor(
    private val database: FirebaseDatabase
) : ViewModel() {

    private val _allUsers by lazy { MutableLiveData<Resource<List<User>>>() }
    private val _currentUser by lazy { MutableLiveData<Resource<Unit>>() }

    val allUser: LiveData<Resource<List<User>>>
        get() = _allUsers

    val currentUser: LiveData<Resource<Unit>>
        get() = _currentUser

    private var userDb: DatabaseReference = database.getReference("users")


    fun addNewUser(name: String, user: FirebaseUser?) {

        _currentUser.value = Resource.Loading()

        user?.let {
            val db = userDb.child(it.uid)

            val userMap = hashMapOf("name" to name)

            db.setValue(userMap).addOnCompleteListener {
                _currentUser.value = Resource.Success(Unit)

            }.addOnFailureListener { e ->

                e.message?.let { message ->
                    _currentUser.value = Resource.Error(message)
                }

            }

        }
    }

    fun fetchAllUsers() {

        _allUsers.value = Resource.Loading()

        userDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Timber.d("Get all users")

                val users = mutableListOf<User>()

                if (snapshot.exists())
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        user?.id = userSnapshot.key!!
                        user?.let { users.add(it) }
                    }

                _allUsers.value = Resource.Success(users)

            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException())

                _allUsers.value = Resource.Error(error.details)
            }

        })

    }


}