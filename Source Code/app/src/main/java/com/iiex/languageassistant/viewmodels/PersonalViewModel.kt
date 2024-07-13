package com.iiex.languageassistant.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.storage.FirebaseStorage
import com.iiex.languageassistant.data.model.User
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.repository.UserRepository
import java.util.*

class PersonalViewModel : ViewModel() {
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user
    private val _topics = MutableLiveData<List<DataItem.Topic>>()
    val topics: LiveData<List<DataItem.Topic>> get() = _topics

    private val userRepository = UserRepository()


    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator

    private var startAfterDocument: DocumentSnapshot? = null
    private var hasMoreData: Boolean = false
    fun setAuthor(authorID: String) {
        userRepository.getUserById(userId = authorID) { user ->
            _user.value = user
        }
    }

    fun setTopics(authorID: String) {
        userRepository.getPersonalTopics(authorID, null, 100).observeForever { result ->
            val topics = result.first
            hasMoreData = result.second.first
            startAfterDocument = result.second.second
            _topics.postValue(topics)
        }
    }

    fun setAuthorTopics(authorID: String) {
        userRepository.getAuthorTopics(authorID, null, 100).observeForever { result ->
            val topics = result.first
            hasMoreData = result.second.first
            startAfterDocument = result.second.second
            _topics.postValue(topics)
        }
    }

    fun upLoadAvatar(imageUri: Uri) {
        _loadingIndicator.value = true
        FirebaseAuth.getInstance().uid?.let { userID ->
            val storageRef =
                FirebaseStorage.getInstance().reference.child("/Users/Avatars/${userID}")
            val uploadTask = storageRef.putFile(imageUri)
            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    userRepository.updateAvatar(userID, downloadUrl) {
                        _loadingIndicator.postValue(false)
                    }
                }.addOnFailureListener {
                        _loadingIndicator.postValue(false)
                    }
            }.addOnFailureListener {
                    _loadingIndicator.postValue(false)
                }
        }
    }

    fun loadMoreData() {
        if(hasMoreData)
        {
            _loadingIndicator.value = true
            _user.value?.id?.let {
                userRepository.getPersonalTopics(it, startAfterDocument, 5).observeForever { result ->
                    val topics = result.first
                    hasMoreData = result.second.first
                    startAfterDocument = result.second.second

                    val currentList = _topics.value ?: emptyList()
                    val updatedList = currentList.toMutableList()
                    updatedList.addAll(topics)
                    _topics.postValue(updatedList)
                    _loadingIndicator.postValue(false)
                }
            }
        }
    }
}