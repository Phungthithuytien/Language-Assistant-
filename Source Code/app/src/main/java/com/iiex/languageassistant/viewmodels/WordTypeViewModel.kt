package com.iiex.languageassistant.viewmodels

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.WordRepository
import kotlinx.parcelize.Parcelize

class WordTypeViewModel : ViewModel() {
    @Parcelize
    data class WordType(
        val id: String,
        val english : String,
        val vietnamese: String,
        var isMarked: Boolean,
        var isChoice: Boolean,
        var isCorrect: Boolean,
        var choice: String,
    ) : Parcelable {
    }
    private val _word = MutableLiveData<List<Word>>()
    val word: LiveData<List<Word>>
        get() = _word
    private val wordRepository = WordRepository()
    private val topicRepository = TopicRepository()
    private var topicID: String = ""

    fun setTopic(userID:String,topicID: String) {
        this.topicID = topicID
        wordRepository.getAllByStatus(userID, topicID, WordStatus.ALL) { listWords ->
            _word.value = listWords
        }
    }

    fun getList(): List<WordType> {
        return word.value?.let { createQuestions(it) } ?: emptyList()
    }

    private fun createQuestions(words: List<Word>): List<WordType> {
        return words.mapNotNull { word ->
            // Ensure we don't create a question with null values
            val vietnamese = word.vietnamese ?: return@mapNotNull null
            val english = word.english ?: return@mapNotNull null

            // Create the question
            word.id?.let {
                WordType(
                    id = it,
                    english = english,
                    vietnamese = vietnamese,
                    isMarked = word.isMarked,
                    isChoice = false,
                    isCorrect = true,
                    choice = "",
                )
            }
        }.shuffled()
    }
}