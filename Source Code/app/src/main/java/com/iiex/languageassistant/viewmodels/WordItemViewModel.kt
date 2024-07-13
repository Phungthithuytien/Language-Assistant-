package com.iiex.languageassistant.viewmodels


import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.play.integrity.internal.w
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.data.model.Topic
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.UserRepository
import com.iiex.languageassistant.databinding.ActivityAddTopicBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class WordItemViewModel : ViewModel() {
    var itemList = mutableListOf<Word>()
    private val _itemListLiveData = MutableLiveData<List<Word>>()
    val itemListLiveData: LiveData<List<Word>>
        get() = _itemListLiveData


    private val topicRepository = TopicRepository()
    private val userRepository = UserRepository()
    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator



    private val _topic = MutableLiveData<Topic>()
    val topic: LiveData<Topic>
        get() = _topic

    var itemHeight: Int = 100

    private lateinit var binding: ActivityAddTopicBinding
    init {
        _topic.value = Topic(
            isPublic = false,
            author = FirebaseAuth.getInstance().uid
        )
        _itemListLiveData.value = itemList
    }
    fun setBinding(binding: ActivityAddTopicBinding){
        this.binding = binding
    }


    fun setTopic(topicID: String) {
        topicRepository.getByID(topicID){
                topic ->
            _topic.value = topic
        }

    }

    fun setWord( topicID: String) {
        topicRepository.loadWords(topicID) {words->
            _itemListLiveData.value = words
            itemList = words as MutableList<Word>
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addItem() {
        val item = Word(id = randomString(7) + itemList.size, english = "", vietnamese =  "")
        itemList.add(item)
        _itemListLiveData.value = itemList
        binding.recyclerViewWord.layoutParams.height = itemHeight*itemList.size

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addItem(english: String,vietnamese:String ) {
        val item = Word(id = randomString(7) + itemList.size, english = english, vietnamese =  vietnamese)
        itemList.add(item)
        _itemListLiveData.value = itemList
        binding.recyclerViewWord.layoutParams.height = itemHeight*itemList.size
    }
    fun deleteItem(position: Int) {
        if (position < itemList.size) {
            itemList.removeAt(position)
            _itemListLiveData.value = itemList
        }
        binding.recyclerViewWord.layoutParams.height = itemHeight*itemList.size
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun saveTopic(): Boolean {
        if (itemList.isEmpty()) {
            _loadingIndicator.value = false
            return false
        }

        _loadingIndicator.value = true

        if (topic.value?.id != null){
            topicRepository.update(topic.value!!) { success ->
                if (success) {
                    topicRepository.updateWord(itemList,topic.value!!) { success ->
                        _loadingIndicator.postValue(false)
                    }
                } else {
                    _loadingIndicator.postValue(false)
                }
            }
        }else{
            topic.value?.let {
                topicRepository.add(it) { success, addedTopic ->
                    if (success && addedTopic != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            _topic.postValue(addedTopic)
                            topicRepository.addWord(itemList, addedTopic) { success ->
                                _loadingIndicator.postValue(false)
                            }
                        }
                    } else {
                        _loadingIndicator.postValue(false)
                    }
                }
            }
        }
        return true
    }

    fun updateTopic(onComplete: (Boolean)->Unit) {
        if (_itemListLiveData.value?.isEmpty() == true) {
            _loadingIndicator.value = false
            return
        }
        _loadingIndicator.value = true
        if (topic.value?.id != null){
            topicRepository.update(topic.value!!) { success ->
                if (success) {
                    _itemListLiveData.value?.let {
                        topicRepository.updateWord(it,topic.value!!) { success ->
                            _loadingIndicator.postValue(false)
                            onComplete(true)
                        }
                    }
                } else {
                    _loadingIndicator.postValue(false)
                    onComplete(false)
                }
            }
        }
        return
    }
    fun createTopic(onComplete: (Boolean)->Unit){
        _loadingIndicator.value = true
        if (_itemListLiveData.value?.isEmpty() == true) {
            _loadingIndicator.value = false
            return
        }
        topic.value?.let {
            topicRepository.add(it) { success, addedTopic ->
                if (success && addedTopic != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        _topic.postValue(addedTopic)
                        _itemListLiveData.value?.let { it1 ->
                            topicRepository.addWord(it1, addedTopic) { success ->
                                _loadingIndicator.postValue(false)
                                onComplete(true)
                            }
                        }
                    }
                } else {
                    _loadingIndicator.postValue(false)
                    onComplete(false)
                }
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun readCSVFile(uri: Uri, contentResolver: ContentResolver) {
        val inputStream = contentResolver.openInputStream(uri)
        val reader = BufferedReader(InputStreamReader(inputStream))
        try {
            itemList.clear()
            _itemListLiveData.value = itemList
            var line: String? = reader.readLine()
            while (line != null) {
                val tokens = line.split(",")
                addItem(tokens[0],tokens[1])
                line = reader.readLine()
            }

            reader.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun handleTopicWithUser(onComplete: (DataItem.Topic?)->Unit) {
        topic.value?.author?.let { author ->
            topic.value!!.id?.let { id ->
                userRepository.getUserTopic(author, id) { topicRef ->
                    onComplete(topicRef)
                }
            }
        }
    }


    fun randomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

}