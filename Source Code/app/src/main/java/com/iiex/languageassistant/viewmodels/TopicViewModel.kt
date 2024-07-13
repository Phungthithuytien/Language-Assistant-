package com.iiex.languageassistant.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.repository.UserRepository
import com.iiex.languageassistant.util.DateTimeUtil



@RequiresApi(Build.VERSION_CODES.O)
class TopicViewModel : ViewModel() {
  private val userRepository = UserRepository()

  private val _dataListStored = MutableLiveData<List<DataItem>>()
  private val _dataList = MutableLiveData<List<DataItem>>()
  val dataList: LiveData<List<DataItem>> get() = _dataList

  private val _loadingIndicator = MutableLiveData<Boolean>()
  val loadingIndicator: LiveData<Boolean>
    get() = _loadingIndicator

  private var startAfterDocument: DocumentSnapshot? = null
  private var hasMoreData: Boolean = false

  fun reload(){
    val user = FirebaseAuth.getInstance().currentUser
    user?.let {
      _loadingIndicator.value = true
      userRepository.getUserTopics(it.uid, null, 100).observeForever { result ->
        val dataList = result.first
        hasMoreData = result.second.first
        startAfterDocument = result.second.second
        _dataList.value = emptyList()
        val currentList = emptyList<DataItem>()
        val updatedList = currentList.toMutableList()
        val groupList = groupTopicsByLastAccessAndAddHeaders(dataList)
        updatedList.addAll(groupList)

        _dataList.value = updatedList
        _dataListStored.value = updatedList
        _loadingIndicator.postValue(false)
      }
    }
  }

  fun loadMoreData() {
    if(hasMoreData)
    {
      val user = FirebaseAuth.getInstance().currentUser
      user?.let {
        _loadingIndicator.value = true

        userRepository.getUserTopics(it.uid, startAfterDocument, 5).observeForever { result ->
          val dataList = result.first
          hasMoreData = result.second.first
          startAfterDocument = result.second.second

          val currentList = _dataList.value ?: emptyList()
          val updatedList = currentList.toMutableList()
          val groupList = groupTopicsByLastAccessAndAddHeaders(dataList)
          updatedList.addAll(groupList)
          _dataList.value = updatedList
          _dataListStored.value = updatedList
          _loadingIndicator.postValue(false)
        }
      }
    }
  }

  var currentDate: String? = null
  var currentHeader: DataItem.Header? = null
  private fun groupTopicsByLastAccessAndAddHeaders(topicList: List<DataItem.Topic>): List<DataItem> {
    val groupedItems = mutableListOf<DataItem>()

    for (topic in topicList) {
      val topicLastAccess = topic.lastAccess ?: 0L
      val topicDate = DateTimeUtil.getDateFromTimestamp(topicLastAccess)

      if(_dataList.value?.contains(topic) != true){
        if (currentDate != topicDate) {
          currentDate = topicDate
          currentHeader = DataItem.Header(currentDate!!)
          groupedItems.add(currentHeader!!)
        }
        groupedItems.add(topic)
      }else{
        val e = _dataList.value
      }
    }

    return groupedItems
  }

  fun filter(query: String?) {
    if (query != null) {
      if (query.isBlank()||query.isEmpty()){
        _dataList.value = _dataListStored.value
      }else{
        val filteredList = _dataListStored.value?.filter { dataItem ->
          if (dataItem is DataItem.Topic) {
            dataItem.topicTitle?.contains(query, true) ?: false
          } else {
            false
          }
        }
        _dataList.value = filteredList?: listOf()
      }
    }else
    {
      _dataList.value = _dataListStored.value
    }
  }

}