package com.iiex.languageassistant.viewmodels

import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.iiex.languageassistant.data.model.Folder
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.repository.UserRepository
import com.iiex.languageassistant.util.DateTimeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FolderViewModel : ViewModel() {
    private val userRepository = UserRepository()

    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator

    private val _foldersStored = MutableLiveData<List<DataItem>>()
    private val _folders = MutableLiveData<List<DataItem>>()
    val folders: LiveData<List<DataItem>>
        get() = _folders

    private val _folder = MutableLiveData<DataItem.Folder>()
    val folder: LiveData<DataItem.Folder>
        get() = _folder

    private val _dataList = MutableLiveData<List<DataItem>>()
    val dataList: LiveData<List<DataItem>> get() = _dataList

    private var startAfterDocument: DocumentSnapshot? = null
    private var hasMoreData: Boolean = false
    fun setFolder(){
        FirebaseAuth.getInstance().currentUser?.let { user ->
            _loadingIndicator.value = true
            userRepository.getUserFolders(user.uid,null,100).observeForever {
                    result ->
                val dataList = result.first
                hasMoreData = result.second.first
                startAfterDocument = result.second.second
                _folders.value = mutableListOf()
                val currentList = _folders.value ?: mutableListOf()
                val updatedList = currentList.toMutableList()
                updatedList.addAll(groupTopicsByLastAccessAndAddHeaders(dataList))
                _folders.value = updatedList
                _foldersStored.value = updatedList
                _loadingIndicator.postValue(false)
            }
        }
    }

    fun initFolder(folderID:String){
        FirebaseAuth.getInstance().currentUser?.let { user ->
            _loadingIndicator.value = true
            userRepository.getFolderByID(user.uid,folderID) {
                    folder ->
                _folder.value = folder
                if (folder != null) {
                    userRepository.getUserTopicsByFolder(user.uid,folder,null,100).observeForever {
                            result ->
                        val dataList = result.first
                        hasMoreData = result.second.first
                        startAfterDocument = result.second.second
                        _dataList.value = mutableListOf()
                        val currentList = _dataList.value ?: mutableListOf()
                        val updatedList = currentList.toMutableList()
                        updatedList.addAll(dataList)
                        _dataList.value = updatedList
                        _loadingIndicator.postValue(false)
                    }
                }
                _loadingIndicator.postValue(false)
            }
        }
    }

    fun loadTopic(folderID:String){
        FirebaseAuth.getInstance().currentUser?.let { user ->
            _loadingIndicator.value = true
            userRepository.getFolderByID(user.uid,folderID) {
                    folder ->
                _folder.value = folder
                if (folder != null) {
                    userRepository.getUserTopics(user.uid,folder,null,100).observeForever {
                            result ->
                        val dataList = result.first
                        hasMoreData = result.second.first
                        startAfterDocument = result.second.second
                        _dataList.value = mutableListOf()
                        val currentList = _dataList.value ?: mutableListOf()
                        val updatedList = currentList.toMutableList()
                        updatedList.addAll(dataList)
                        _dataList.value = updatedList
                        _loadingIndicator.postValue(false)
                    }
                }
                _loadingIndicator.postValue(false)
            }
        }
    }
    fun loadMoreTopic() {
        if(hasMoreData)
        {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                _loadingIndicator.value = true
                userRepository.getUserTopics(it.uid,
                    _folder.value as DataItem.Folder, startAfterDocument, 5).observeForever { result ->
                    val dataList = result.first
                    hasMoreData = result.second.first
                    startAfterDocument = result.second.second
                    val currentList = _dataList.value ?: mutableListOf()
                    val updatedList = currentList.toMutableList()
                    updatedList.addAll(dataList)
                    _dataList.value = updatedList
                    _loadingIndicator.postValue(false)
                }
            }
        }
    }

    fun addToFolder(topicID:String, folderID:String, onComplete: (Boolean)->Unit){
        _loadingIndicator.value = true
        FirebaseAuth.getInstance().currentUser?.let { user ->
            userRepository.addIntoFolder(user.uid,topicID,folderID){
                _loadingIndicator.postValue(false)
                onComplete(it)
            }

        }
    }

    fun removeFormFolder(topicID:String, folderID:String, onComplete:(Boolean)->Unit){
        _loadingIndicator.value = true
        FirebaseAuth.getInstance().currentUser?.let { user ->
            userRepository.removeFromFolder(user.uid,topicID,folderID){
                _loadingIndicator.postValue(false)
                onComplete(it)
            }

        }
    }

    fun setFolder(topicID: String){
        FirebaseAuth.getInstance().currentUser?.let { user ->
            _loadingIndicator.value = true
            userRepository.getUserFolders(user.uid,topicID).observeForever {
                    result ->
                val dataList = result
                _folders.value = mutableListOf()
                val currentList = _folders.value ?: mutableListOf()
                val updatedList = currentList.toMutableList()
                updatedList.addAll(dataList)
                _folders.value = updatedList
                _loadingIndicator.postValue(false)
            }
        }
    }

    fun loadMoreTopicData() {
        if(hasMoreData)
        {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                _loadingIndicator.value = true
                userRepository.getUserTopicsByFolder(it.uid,
                    _folder.value as DataItem.Folder, startAfterDocument, 5).observeForever { result ->
                    val dataList = result.first
                    hasMoreData = result.second.first
                    startAfterDocument = result.second.second
                    val currentList = _dataList.value ?: mutableListOf()
                    val updatedList = currentList.toMutableList()
                    updatedList.addAll(dataList)
                    _dataList.value = updatedList
                    _loadingIndicator.postValue(false)
                }
            }
        }
    }

    fun loadMoreData() {
        if(hasMoreData)
        {
            val user = FirebaseAuth.getInstance().currentUser
            user?.let {
                _loadingIndicator.value = true
                userRepository.getUserFolders(it.uid, startAfterDocument, 5).observeForever { result ->
                    val dataList = result.first
                    hasMoreData = result.second.first
                    startAfterDocument = result.second.second
                    val currentList = _folders.value ?: mutableListOf()
                    val updatedList = currentList.toMutableList()
                    updatedList.addAll(groupTopicsByLastAccessAndAddHeaders(dataList))
                    _folders.postValue(updatedList)
                    _loadingIndicator.postValue(false)
                }
            }
        }
    }
    var currentDate: String? = null
    var currentHeader: DataItem.Header? = null
    private fun groupTopicsByLastAccessAndAddHeaders(topicList: List<DataItem.Folder>): List<DataItem> {
        val groupedItems = mutableListOf<DataItem>()

        for (topic in topicList) {
            val topicLastAccess = topic.updateTime ?: 0L
            val topicDate = DateTimeUtil.getDateFromTimestamp(topicLastAccess)

            if(_folders.value?.contains(topic) != true){
                if (currentDate != topicDate) {
                    currentDate = topicDate
                    currentHeader = DataItem.Header(currentDate!!)
                    groupedItems.add(currentHeader!!)
                }
                groupedItems.add(topic)
            }else{
                val e = _folders.value
            }
        }

        return groupedItems
    }


    fun deleteFolder(folderID: String, onComplete: (Boolean) -> Unit){
        FirebaseAuth.getInstance().currentUser?.let { user ->
            _loadingIndicator.value = true
            userRepository.deleteFolder(user.uid,folderID){
                onComplete(it)
            }
        }
    }

    fun editTitle(folderID: String,folderTitle: String,onComplete: (Boolean) -> Unit){
        FirebaseAuth.getInstance().currentUser?.let { user ->
            _loadingIndicator.value = true
            _folder.value?.title =folderTitle
            userRepository.editFolder(user.uid,folderTitle,folderID){
                onComplete(it)
            }
        }
    }

    fun filter(query: String?) {
        if (query != null) {
            if (query.isBlank()||query.isEmpty()){
                _folders.value = _foldersStored.value
            }else{
                val filteredList = _foldersStored.value?.filter { dataItem ->
                    if (dataItem is DataItem.Folder) {
                        dataItem.title?.contains(query, true) ?: false
                    } else {
                        false
                    }
                }
                _folders.value = filteredList?: listOf()
            }
        }else
        {
            _folders.value = _foldersStored.value
        }
    }
}