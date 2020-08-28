package com.arjun.firechat

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjun.firechat.model.Message
import com.arjun.firechat.model.User
import com.arjun.firechat.util.Event
import com.arjun.firechat.util.FileUtils
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.TimeSince
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel @ViewModelInject constructor(
    mDatabase: FirebaseDatabase,
    mStorage: FirebaseStorage,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val rootRef by lazy { mDatabase.reference }
    private val userRef by lazy { rootRef.child("users") }
    private val chatRef by lazy { rootRef.child("chat") }
    private val messageRef by lazy { rootRef.child("messages") }
    private val notificationRef by lazy { rootRef.child("notification") }

    private val rootStorageRef by lazy { mStorage.reference }
    private val userProfilePicturesRef by lazy { rootStorageRef.child("profilePictures") }
    private val mediaRef by lazy { rootStorageRef.child("media") }

    private var lastSentMediaUri: String? = null
    private var lastSentMediaMessage: Message? = null

    private val _allUsers by lazy { MutableLiveData<Resource<List<User>>>() }
    private val _currentUser by lazy { MutableLiveData<Resource<User>>() }
    private val _addNewUser by lazy { MutableLiveData<Resource<Unit>>() }
    private val _allMessages by lazy { MutableLiveData<Resource<List<Message>>>() }
    private val _lastUpdatedMessageIndex by lazy { MutableLiveData<Int>() }
    private val _chatUserStatus by lazy { MutableLiveData<Resource<String>>() }
    private val _isChatUserOnline by lazy { MutableLiveData(false) }
    private val _pictureUploadStatus by lazy { MutableLiveData<Event<Unit>>() }
    private val _popBack by lazy { MutableLiveData<Event<Unit>>() }
    private val _myBlockedUserIds by lazy { MutableLiveData<List<String>>() }
    private val _chatUserBlockedUserIds by lazy { MutableLiveData<List<String>>() }

    val allUsers: LiveData<Resource<List<User>>>
        get() = _allUsers

    val currentUser: LiveData<Resource<User>>
        get() = _currentUser

    val addNewUser: LiveData<Resource<Unit>>
        get() = _addNewUser

    val allMessage: LiveData<Resource<List<Message>>>
        get() = _allMessages

    val lastUpdatedMessageIndex: LiveData<Int>
        get() = _lastUpdatedMessageIndex

    val chatUserStatus: LiveData<Resource<String>>
        get() = _chatUserStatus

    val isChatUserOnline: LiveData<Boolean>
        get() = _isChatUserOnline

    val pictureUploadStatus: LiveData<Event<Unit>>
        get() = _pictureUploadStatus

    val myBlockedUserIds: LiveData<List<String>>
        get() = _myBlockedUserIds

    val chatUserBlockedUserIds: LiveData<List<String>>
        get() = _chatUserBlockedUserIds

    val popBack: LiveData<Event<Unit>>
        get() = _popBack

    fun addNewUser(name: String, user: FirebaseUser?) {

        _addNewUser.value = Resource.Loading()

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w("getInstanceId failed:  ${task.exception}")
                    task.exception?.message?.let { _addNewUser.value = Resource.Error(it) }
                    return@addOnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token
                Timber.d(token)

                user?.let {
                    val db = userRef.child(it.uid)

                    val userMap = hashMapOf<String, Any?>(
                        "name" to name,
                        "token" to token,
                        "online" to true,
                        "lastSeen" to ServerValue.TIMESTAMP
                    )

                    db.setValue(userMap).addOnCompleteListener {
                        _addNewUser.value = Resource.Success(Unit)

                    }.addOnFailureListener { e ->

                        e.message?.let { message ->
                            _addNewUser.value = Resource.Error(message)
                        }

                    }

                }

            }
    }

    fun fetchAllUsers(currentUserId: String) {

        _allUsers.value = Resource.Loading()
        _currentUser.value = Resource.Loading()

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

                val currentUser = users.first { it.id == currentUserId }
                _currentUser.value = Resource.Success(currentUser)
                _allUsers.value = Resource.Success(users.filter { it.id != currentUserId })

            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e(error.toException())

                _allUsers.value = Resource.Error(error.details)
                _currentUser.value = Resource.Error(error.details)
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
                        "timestamp" to ServerValue.TIMESTAMP,
                        "online" to true
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
            "timestamp" to ServerValue.TIMESTAMP,
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

                message?.let {
                    messages.add(it)
                }

                lastSentMediaMessage = messages.find { it.mediaUrl == lastSentMediaUri }

                _allMessages.value = Resource.Success(messages)

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                val updatedMessage = snapshot.getValue(Message::class.java)
                updatedMessage?.id = snapshot.key!!

                messages.forEach {
                    if (it.id == snapshot.key!!) {
                        it.apply {
                            mediaUrl = updatedMessage?.mediaUrl!!
                        }
                        _lastUpdatedMessageIndex.value = messages.indexOf(it)
                    }
                }

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

    fun getChatUserPresence(chatUserId: String) {

        _chatUserStatus.value = Resource.Loading()

        userRef.child(chatUserId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    val online = snapshot.child("online").value as Boolean?
                    val lastSeen = snapshot.child("lastSeen").value as Long?

                    online?.let {
                        if (it) {
                            _chatUserStatus.value = Resource.Success("online")
                        } else {
                            _chatUserStatus.value =
                                Resource.Success(TimeSince.getTimeAgo(lastSeen ?: 0))
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.d(error.details)
                _chatUserStatus.value = Resource.Error(error.details)
            }

        })
    }

    fun setRegistrationToken(currentUserId: String) {

        val currentUserRef = userRef.child(currentUserId)

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w("getInstanceId failed:  ${task.exception}")
                    task.exception?.message?.let { _addNewUser.value = Resource.Error(it) }
                    return@addOnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token
                Timber.d(token)
                currentUserRef.child("token").setValue(token)
            }
    }

    fun setUserPresence(currentUserId: String) {

        val currentUserRef = userRef.child(currentUserId)

        currentUserRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {

                if (snapshot.exists()) {
                    currentUserRef.child("online").onDisconnect().setValue(false)
                    currentUserRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
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
        currentUserRef.child("lastSeen").setValue(ServerValue.TIMESTAMP)

    }

    fun uploadAndUpdateProfilePicture(currentUserId: String, uri: Uri) {

        viewModelScope.launch(Dispatchers.IO) {

            val file = fileUtils.getFile(uri)

            val pictureRef = userProfilePicturesRef.child(file?.name.toString())

            pictureRef.putFile(uri).addOnCompleteListener {

                if (it.isSuccessful) {

                    pictureRef.downloadUrl.addOnSuccessListener { profileUri ->
                        Timber.d(it.toString())

                        userRef.child(currentUserId).child("image").setValue(profileUri.toString())
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    _pictureUploadStatus.postValue(Event(Unit))
                                } else
                                    Timber.d(it.exception)
                            }

                    }

                } else
                    Timber.d(it.exception)

            }


        }
    }

    private fun uploadMediaMessage(currentUserId: String, chatUserId: String, uri: Uri) {

        viewModelScope.launch(Dispatchers.IO) {

            val file = fileUtils.getFile(uri)

            val pictureRef = mediaRef.child(file?.name.toString())

            pictureRef.putFile(uri).addOnCompleteListener {

                if (it.isSuccessful) {

                    pictureRef.downloadUrl.addOnSuccessListener { mediaUri ->
                        Timber.d(mediaUri.toString())

                        lastSentMediaMessage?.id?.let { messageId ->
                            updateMediaMessage(currentUserId, chatUserId, messageId, mediaUri)
                        }
                    }

                } else
                    Timber.d(it.exception)

            }


        }

    }

    fun sendMediaMessage(currentUserId: String, chatUserId: String, uri: Uri) {

        lastSentMediaUri = uri.toString()

        val currentUserRef = "messages/$currentUserId/$chatUserId"
        val chatUserRef = "messages/$chatUserId/$currentUserId"

        val push = messageRef.child(currentUserId)
            .child(chatUserId)
            .push()

        val pushKey = push.key

        val messageMap = hashMapOf<String, Any>(
            "mediaUrl" to uri.toString(),
            "seen" to false,
            "type" to "media",
            "from" to currentUserId,
            "timestamp" to ServerValue.TIMESTAMP,
        )


        val messageUserMap = hashMapOf<String, Any>(
            "$currentUserRef/$pushKey" to messageMap,
            "$chatUserRef/$pushKey" to messageMap
        )

        rootRef.updateChildren(
            messageUserMap
        ) { error, ref ->

            uploadMediaMessage(currentUserId, chatUserId, uri)

            error?.let {
                Timber.d("Chat Log: ${it.details}")
            }
        }
    }

    private fun updateMediaMessage(
        currentUserId: String,
        chatUserId: String,
        messageId: String,
        uri: Uri
    ) {
        val currentUserMediaMessageRef = "messages/$currentUserId/$chatUserId/$messageId"
        val chatUserMediaMessageRef = "messages/$chatUserId/$currentUserId/$messageId"

        rootRef.child(currentUserMediaMessageRef).child("mediaUrl").setValue(uri.toString())
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Timber.d("Success updating current user's media message with the firebase media url")
                    resetLastMediaAssets()
                } else
                    Timber.d(it.exception)
            }

        rootRef.child(chatUserMediaMessageRef).child("mediaUrl").setValue(uri.toString())
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Timber.d("Success updating chat user's media message with the firebase media url")
                    resetLastMediaAssets()
                } else
                    Timber.d(it.exception)
            }
    }

    private fun resetLastMediaAssets() {
        lastSentMediaMessage = null
        lastSentMediaUri = null
    }

    fun sendNotification(currentUserId: String, chatUserId: String, message: String) {

        val notificationMap = hashMapOf<String, Any>(
            "from" to currentUserId,
            "type" to "message",
            "message" to message
        )

        notificationRef.child(chatUserId).push().setValue(notificationMap).addOnSuccessListener {
            Timber.d("$it")
        }
    }

    fun updateMyStatusWithChatUser(currentUserId: String?, chatUserId: String, status: Boolean) {
        chatRef.child(currentUserId!!).child(chatUserId).child("online").setValue(status)
    }

    fun monitorChatUserStatusWithMe(currentUserId: String, chatUserId: String) {
        chatRef.child(chatUserId).child(currentUserId).child("online")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Timber.d("$snapshot")
                    _isChatUserOnline.value = snapshot.value as Boolean? ?: false

                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e(error.details)
                }

            })
    }

    fun blockUser(currentUserId: String, chatUserId: String) {
        val blockUserRef = userRef.child(currentUserId).child("blocked").child(chatUserId)

        val blockUserMap = hashMapOf<String, Any>(
            "id" to chatUserId
        )

        blockUserRef.setValue(blockUserMap).addOnSuccessListener {
            _popBack.value = Event(Unit)
            Timber.d("$it")
        }

    }

    fun getMyBlockedUsers(currentUserId: String) {
        val blockUserRef = userRef.child(currentUserId).child("blocked")

        blockUserRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Timber.d("My blocked users: $snapshot")

                val myBlockedUserIds = mutableListOf<String>()

                if (snapshot.exists())
                    for (userSnapshot in snapshot.children) {
                        Timber.d("${userSnapshot.key}")
                        myBlockedUserIds.add(userSnapshot.key!!)
                    }

                _myBlockedUserIds.value = myBlockedUserIds

            }

            override fun onCancelled(error: DatabaseError) {
                Timber.d(error.details)
            }

        })

    }

    fun getChatUserBlockedUsers(chatUserId: String) {
        val blockUserRef = userRef.child(chatUserId).child("blocked")

        blockUserRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Timber.d("Chat user blocked users: $snapshot")

                val chatUsersBlockedUserIds = mutableListOf<String>()
                if (snapshot.exists())
                    for (userSnapshot in snapshot.children) {
                        Timber.d("${userSnapshot.key}")
                        chatUsersBlockedUserIds.add(userSnapshot.key!!)
                    }

                _chatUserBlockedUserIds.value = chatUsersBlockedUserIds
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.d(error.details)
            }

        })
    }

    fun unblockUser(currentUserId: String, id: String) {

        userRef.child(currentUserId).child("blocked").orderByChild("id").equalTo(id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists())
                        for (userSnapshot in snapshot.children) {
                            if (userSnapshot.exists())
                                userSnapshot.ref.removeValue().addOnSuccessListener {
                                    Timber.d("User unblocked")
                                    _popBack.value = Event(Unit)
                                }
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.d(error.details)
                }

            })

    }

}