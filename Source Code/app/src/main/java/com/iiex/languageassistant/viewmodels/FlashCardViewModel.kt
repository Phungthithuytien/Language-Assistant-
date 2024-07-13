package com.iiex.languageassistant.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FlashCardViewModel : ViewModel() {
    private val wordRepository = WordRepository()
    private val topicRepository = TopicRepository()

    private val _word = MutableLiveData<List<Word>>()
    val word: LiveData<List<Word>>
        get() = _word

    private var topicID: String = ""

    fun setTopic(userID:String,topicID: String, isMarked: String) {
        this.topicID = topicID
        wordRepository.getAllByStatus(userID, topicID, WordStatus.ALL) { listWords ->
            if (isMarked.equals("marked")){
                _word.value = listWords.filter { word ->
                    word.BookmarkByUser[userID] == true
                }
            }else{
                _word.value = listWords
            }
        }
    }

    fun mark(currentUser: String, wordId: String?, newStatus: Boolean) {
        if (wordId != null) {
            wordRepository.mark(
                topic_id = topicID,
                userId = currentUser,
                wordId = wordId,
                newStatus = newStatus
            ) {

            }
        }
    }

}