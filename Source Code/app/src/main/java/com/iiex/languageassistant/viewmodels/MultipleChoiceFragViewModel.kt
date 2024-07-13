package com.iiex.languageassistant.viewmodels


import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.LeaderBoard
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.WordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.time.LocalDateTime
import java.time.ZoneOffset


@Parcelize
data class Question(
    val id: String,
    val questionText: String,
    val correct: String,
    val tts: String,
    var isMarked: Boolean,
    var isChoice: Boolean,
    var isCorrect: Boolean,
    var choice: Int,
    val options: List<Option>
) : Parcelable {
}

@Parcelize
data class Option(
    val optionText: String,
    val isTrue: Boolean
) : Parcelable {
}

class MultipleChoiceFragViewModel : ViewModel() {

    private val _word = MutableLiveData<List<Word>>()
    val word: LiveData<List<Word>>
        get() = _word
    private val wordRepository = WordRepository()
    private val topicRepository = TopicRepository()
    private var topicID: String = ""

    fun setTopic(userID: String, topicID: String) {
        this.topicID = topicID
        wordRepository.getAllByStatus(userID, topicID, WordStatus.ALL) { listWords ->
            _word.value = listWords
        }
    }

    fun getList(askInVietnamese: Boolean): List<Question> {
        return word.value?.let { createQuestions(it,askInVietnamese) } ?: emptyList()
    }

    private fun createQuestions(words: List<Word>, askInVietnamese: Boolean): List<Question> {
        return words.mapNotNull { word ->
            // Ensure we don't create a question with null values
            val vietnamese = word.vietnamese ?: return@mapNotNull null
            val english = word.english ?: return@mapNotNull null

            // Generate three random incorrect options
            val incorrectOptions = words.asSequence()
                .filter { it.vietnamese != vietnamese && it.english != english }
                .mapNotNull { if (askInVietnamese) it.english else it.vietnamese }
                .shuffled()
                .take(3)
                .map { Option(it, false) }
                .toList()

            val correctAnswer = if (askInVietnamese) english else vietnamese
            val correctOption = Option(correctAnswer, true)

            val options = (incorrectOptions + correctOption).shuffled()
            val questionText = if (askInVietnamese) vietnamese else english

            // Create the question
            word.id?.let {
                Question(
                    id = it,
                    questionText = questionText,
                    correct = correctAnswer,
                    isMarked = word.isMarked,
                    isChoice = false,
                    tts = english,
                    isCorrect = false,
                    choice = -1, // Assuming default choice index is -1 for unselected
                    options = options
                )
            }
        }.shuffled()
    }


    fun updateWordStatusQuestion(userID: String, topicID: String, listWords: List<Question>) {
        viewModelScope.launch {
            for (word in listWords) {
                val status = if (word.isCorrect) WordStatus.MASTERED else WordStatus.LEARNED
                updateWordStatusAsync(topicID, word.id, userID, status)
            }
        }
        val counter = listWords.count { it.isCorrect }
        updateUsertopicWordLearned(userID,topicID,counter)
    }

    fun updateWordStatusWordType(
        userID: String,
        topicID: String,
        listWords: List<WordTypeViewModel.WordType>
    ) {
        viewModelScope.launch {
            for (word in listWords) {
                val status = if (word.isCorrect) WordStatus.MASTERED else WordStatus.LEARNED
                updateWordStatusAsync(topicID, word.id, userID, status)
            }
        }
        val counter = listWords.count { it.isCorrect }
        updateUsertopicWordLearned(userID,topicID,counter)
    }

    private suspend fun updateWordStatusAsync(
        topicID: String,
        wordID: String,
        userID: String,
        status: WordStatus
    ) {
        withContext(Dispatchers.IO) {
            wordRepository.updateWordStatusForUser(topicID, wordID, userID, status)
            { result ->
                if (result) {
                    Log.i("wordRepository", "updateWordStatus: OK!")
                }
            }
        }
    }

    private fun updateUsertopicWordLearned(userID: String,topicID: String,counter:Int){
        topicRepository.updateUsertopicWordLearned(userID,topicID,counter){
            Log.i("wordRepository", "updateUsertopic: OK!")
        }
    }



    fun addLeaderBoard(topicID: String, userID: String, score: Int, started: Long) {
        val now = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
        val leaderBoard = LeaderBoard(
            FirebaseFirestore.getInstance().collection("users").document(userID),
            score,
            now - started,
            now
        )
        topicRepository.addLeaderBoard(topicID, leaderBoard) {
        }
    }

    fun mark(userId: String, topicID: String, wordId: String, wordStatus: Boolean) {
        wordRepository.mark(
            topic_id = topicID,
            userId = userId,
            wordId = wordId,
            newStatus = wordStatus
        ) {

        }
    }
}
