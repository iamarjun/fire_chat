package com.arjun.firechat

import android.net.Uri
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arjun.firechat.model.Message
import com.arjun.firechat.model.User
import com.arjun.firechat.util.FileUtils
import com.arjun.firechat.util.Resource
import com.arjun.firechat.util.TimeSince
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel @ViewModelInject constructor(
    mDatabase: FirebaseDatabase,
    mStorage: FirebaseStorage,
    private val fileUtils: FileUtils
) : ViewModel() {

    private val rootRef = mDatabase.reference
    private val userRef = rootRef.child("users")
    private val chatRef = rootRef.child("chat")
    private val messageRef = rootRef.child("messages")
    private val notificationRef = rootRef.child("notification")

    private var lastSentMediaUri: String? = null
    private var lastSentMediaMessage: Message? = null

    private val rootStorageRef = mStorage.reference
    private val userProfilePicturesRef = rootStorageRef.child("profilePictures")
    private val mediaRef = rootStorageRef.child("media")

    private val _allUsers by lazy { MutableLiveData<Resource<List<User>>>() }
    private val _currentUser by lazy { MutableLiveData<Resource<User>>() }
    private val _addNewUser by lazy { MutableLiveData<Resource<Unit>>() }
    private val _allMessages by lazy { MutableLiveData<Resource<List<Message>>>() }
    private val _lastUpdatedMessageIndex by lazy { MutableLiveData<Int>() }
    private val _chatUserStatus by lazy { MutableLiveData<Resource<String>>() }
    private val _isChatUserOnline by lazy { MutableLiveData<Boolean>(false) }
    private val _pictureUploadStatus by lazy { MutableLiveData<Resource<Unit>>() }

    val allUser: LiveData<Resource<List<User>>>
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

    val pictureUploadStatus: LiveData<Resource<Unit>>
        get() = _pictureUploadStatus

    fun addNewUser(name: String, user: FirebaseUser?) {

        _addNewUser.value = Resource.Loading()

        user?.let {
            val db = userRef.child(it.uid)

            val userMap = hashMapOf("name" to name)

            db.setValue(userMap).addOnCompleteListener {
                _addNewUser.value = Resource.Success(Unit)

            }.addOnFailureListener { e ->

                e.message?.let { message ->
                    _addNewUser.value = Resource.Error(message)
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

                _currentUser.value = Resource.Success(users.first { it.id == currentUserId })
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
                        "timestamp" to ServerValue.TIMESTAMP
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

                    _isChatUserOnline.value = online

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

            _pictureUploadStatus.postValue(Resource.Loading())

            val file = fileUtils.getFile(uri)

            val pictureRef = userProfilePicturesRef.child(file?.name.toString())

            pictureRef.putFile(uri).addOnCompleteListener {

                if (it.isSuccessful) {

                    pictureRef.downloadUrl.addOnSuccessListener { profileUri ->
                        Timber.d(it.toString())

                        userRef.child(currentUserId).child("image").setValue(profileUri.toString())
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    _pictureUploadStatus.postValue(Resource.Success(Unit))
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

}