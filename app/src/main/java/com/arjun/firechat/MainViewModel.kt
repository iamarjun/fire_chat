package com.arjun.firechat

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.arjun.firechat.model.Message
import com.arjun.firechat.model.User
import com.arjun.firechat.util.Resource
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import timber.log.Timber

class MainViewModel @ViewModelInject constructor(
    mDatabase: FirebaseDatabase
) : ViewModel() {

    private val _allUsers by lazy { MutableLiveData<Resource<List<User>>>() }
    private val _currentUser by lazy { MutableLiveData<Resource<Unit>>() }

    private val _allMessages by lazy { MutableLiveData<Resource<List<Message>>>() }

    val allUser: LiveData<Resource<List<User>>>
        get() = _allUsers

    val currentUser: LiveData<Resource<Unit>>
        get() = _currentUser

    val allMessage: LiveData<Resource<List<Message>>>
        get() = _allMessages

    private val rootRef = mDatabase.reference
    private val userRef = mDatabase.getReference("users")
    private val chatRef = mDatabase.getReference("chat")
    private val messageRef = mDatabase.getReference("messages")

    fun addNewUser(name: String, user: FirebaseUser?) {

        _currentUser.value = Resource.Loading()

        user?.let {
            val db = userRef.child(it.uid)

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

    fun fetchAllUsers(currentUserId: String) {

        _allUsers.value = Resource.Loading()

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Timber.d("Get all users")

                val users = mutableListOf<User>()

                if (snapshot.exists())
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        user?.id = userSnapshot.key!!
                        user?.let { users.add(it) }
                    }

                _allUsers.value = Resource.Success(users.filter { it.id != currentUserId })

            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException())

                _allUsers.value = Resource.Error(error.details)
            }

        })

    }

    fun chatInit(currentUserId: String, chatUserId: String) {
        chatRef.child(currentUserId).addValueEventListener(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.hasChild(chatUserId)) {
                    val chatMap = hashMapOf<String, Any>(
                        "seen" to false,
                        "timestamp" to System.currentTimeMillis()
                    )

                    val chatUserMap = hashMapOf<String, Any>(
                        "chat/$currentUserId/$chatUserId" to chatMap,
                        "chat/$chatUserId/$currentUserId" to chatMap,
                    )


                    rootRef.updateChildren(
                        chatUserMap
                    ) { error, ref ->

                        error?.let {
                            Timber.d("Chat Log: ${it.details}")
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.d("Chat Log: ${error.details}")
            }

        })
    }

    fun sendMessage(currentUserId: String, chatUserId: String, message: String) {
        val currentUserRef = "messages/$currentUserId/$chatUserId"
        val chatUserRef = "messages/$chatUserId/$currentUserId"

        val push = messageRef.child(currentUserId)
            .child(chatUserId)
            .push()

        val pushKey = push.key

        val messageMap = hashMapOf<String, Any>(
            "message" to message,
            "seen" to false,
            "type" to "text",
            "from" to currentUserId,
            "timestamp" to System.currentTimeMillis(),
        )


        val messageUserMap = hashMapOf<String, Any>(
            "$currentUserRef/$pushKey" to messageMap,
            "$chatUserRef/$pushKey" to messageMap
        )

        rootRef.updateChildren(
            messageUserMap
        ) { error, ref ->
            error?.let {
                Timber.d("Chat Log: ${it.details}")
            }
        }
    }

    fun loadMessages(currentUserId: String, chatUserId: String) {

        _allMessages.value = Resource.Loading()

        val messages = mutableListOf<Message>()

        val query = messageRef.child(currentUserId).child(chatUserId).limitToLast(100)


        query.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

                val message = snapshot.getValue(Message::class.java)

                message?.id = snapshot.key!!

                message?.let { messages.add(it) }

                _allMessages.value = Resource.Success(messages)

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onChildRemoved(snapshot: DataSnapshot) {

            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }

            override fun onCancelled(error: DatabaseError) {

                _allMessages.value = Resource.Error(error.details)

            }

        })
    }

    fun setUserPresence(currentUserId: String) {

        val currentUserRef = userRef.child(currentUserId)

        currentUserRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    currentUserRef.child("online").onDisconnect().setValue(false)
                }

            }

            override fun onCancelled(error: DatabaseError) {
                Timber.d(error.details)
            }

        })
    }

    fun setAsOnline(currentUserId: String) {

        val currentUserRef = userRef.child(currentUserId)
        currentUserRef.child("online").setValue(true)
    }

    fun setAsOffline(currentUserId: String) {

        val currentUserRef = userRef.child(currentUserId)
        currentUserRef.child("online").setValue(false)
    }

}