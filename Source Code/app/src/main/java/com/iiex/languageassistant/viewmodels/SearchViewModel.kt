package com.iiex.languageassistant.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.repository.TopicRepository

class SearchViewModel : ViewModel() {
    private val topicRepository = TopicRepository()


    private val _topics = MutableLiveData<List<DataItem.Topic>>()
    val topics: LiveData<List<DataItem.Topic>> get() = _topics

    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator

    private var query: String = ""

   fun initFilter(query: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            _loadingIndicator.value = true
            this.query = query
            topicRepository.filterByTopicTitle(query).observeForever {result ->
                _topics.postValue(result)
                _loadingIndicator.postValue(false)
            }
        }
    }

}