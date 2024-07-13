package com.iiex.languageassistant.viewmodels

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.Tasks
import com.iiex.languageassistant.data.model.Topic
import com.iiex.languageassistant.data.model.User
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.model.ref.LeaderBoard
import com.iiex.languageassistant.data.model.ref.UserTopicRef
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.UserRepository
import com.iiex.languageassistant.data.repository.WordRepository
import com.iiex.languageassistant.util.DateTimeUtil
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter

class TopicDetailPersonalViewModel : ViewModel() {


    private val userRepository = UserRepository()
    private val topicRepository = TopicRepository()
    private val wordRepository = WordRepository()

    // LiveData cho author, topic và topicRef
    private val _author = MutableLiveData<User>()
    val author: LiveData<User>
        get() = _author

    private val _topic = MutableLiveData<Topic>()
    val topic: LiveData<Topic>
        get() = _topic
    private val _word = MutableLiveData<List<Word>>()
    val word: LiveData<List<Word>>
        get() = _word

    private val _topicItem = MutableLiveData<DataItem.Topic>()
    val topicItem: LiveData<DataItem.Topic>
        get() = _topicItem

    private val _leaderBoard = MutableLiveData<List<RankItem>>()
    val leaderBoard: LiveData<List<RankItem>>
        get() = _leaderBoard

    private val _loadingIndicator = MutableLiveData<Boolean>()
    val loadingIndicator: LiveData<Boolean>
        get() = _loadingIndicator

    fun setAuthor(authorID: String) {
        userRepository.getUserById(userId = authorID) { user ->
            _author.value = user
        }
    }

    fun setTopic(topicID: String) {
        topicRepository.getByID(topicID) { topic ->
            if (topic != null)
                _topic.value = topic
        }
    }
    fun incrementViewCount(topicID: String){
        topicRepository.incrementViewCount(topicID){

        }
    }

    fun setTopicItem(userId: String, topicID: String) {
        _loadingIndicator.value = true
        userRepository.getUserTopic(userId, topicID) { topic ->
            if (topic != null){
                if (topic.wordCount == 0){
                    topic.wordCount = _topic.value?.wordCount ?: 0
                }
                _topicItem.value = topic
            }
            _loadingIndicator.value = false
        }
    }

    fun setWord(userId: String, topicID: String, wordStatus: WordStatus) {
        _loadingIndicator.value = true
        wordRepository.getAllByStatus(userId, topicID, wordStatus) {
            _word.value = it
            _loadingIndicator.value = false
        }
    }

    fun setWord(userId: String, wordStatus: WordStatus) {
        _loadingIndicator.value = true
        topic.value?.id?.let { topicID ->
            wordRepository.getAllByStatus(userId, topicID, wordStatus) {
                _word.value = it
                _loadingIndicator.value = false
            }
        }
    }

    fun setPublic(isPublic: Boolean) {
        _loadingIndicator.value = true
        topic.value?.id?.let {
            topicRepository.setPublic(it, isPublic) {
                _loadingIndicator.postValue(false)
            }
        }
    }

    data class RankItem(
        val avatarUrl: String,
        val name: String,
        val time: String,
        val date: String,
        val top: String
    )


    fun generateRankItems() {
        topic.value?.id?.let { topicId ->
            topicRepository.getLeaderBoard(topicId) { leaderBoardList ->
                _leaderBoard.value = leaderBoardList
            }
        }
    }


    fun formatTimeDuration(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "${minutes}p${remainingSeconds}s"
    }


    fun mark(userId: String, wordId: String, wordStatus: Boolean) {
        topic.value?.id?.let { topic ->
            wordRepository.mark(
                topic_id = topic,
                userId = userId,
                wordId = wordId,
                newStatus = wordStatus
            ) {

            }
        }
    }

    fun deleteTopic(onComolete: (Boolean)->Unit): Unit {
        _loadingIndicator.value = true
        topic.value?.let {
            it.id?.let { it1 ->
                topicRepository.delete(it1){
                    onComolete(true)
                    _loadingIndicator.postValue(false)
                }
            }
        }
    }

    fun exportCSVFile(uri: Uri, contentResolver: ContentResolver?, words: LiveData<List<Word>>, onComplete: (Boolean) -> Unit)  {
        try {
            val outputStream = contentResolver?.openOutputStream(uri)
            val writer = BufferedWriter(OutputStreamWriter(outputStream))
            for (word in words.value!!) {
                val studentData = StringBuilder()
                studentData.append("${word.english},${word.vietnamese}")
                writer.write(studentData.toString())
                writer.newLine()
            }
            writer.close()
            onComplete(true)
        } catch (e: IOException) {
            e.printStackTrace()
            onComplete(false)
        }
    }
}