package com.iiex.languageassistant.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.UserRepository

class CommunityViewModel : ViewModel() {
    private val topicRepository = TopicRepository()


    private val _topTopic = MutableLiveData<List<DataItem>>()
    val topTopic: LiveData<List<DataItem>> get() = _topTopic

    private val _topics = MutableLiveData<List<DataItem.Topic>>()
    val topics: LiveData<List<DataItem.Topic>> get() = _topics

    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator

    private var startAfterDocument: DocumentSnapshot? = null
    private var hasMoreData: Boolean = false

    fun init() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            _loadingIndicator.value = true
            topicRepository.getTopTopics {
                _topTopic.postValue(it)
                _loadingIndicator.postValue(false)
            }

            topicRepository.getNewTopics(null,100).observeForever {result ->
                val topics = result.first
                hasMoreData = result.second.first
                startAfterDocument = result.second.second
                _topics.postValue(topics)
                _loadingIndicator.postValue(false)
            }
        }
    }

    fun loadMoreData() {
        if(hasMoreData)
        {
            _loadingIndicator.value = true
            topicRepository.getNewTopics(startAfterDocument,10).observeForever {result ->
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