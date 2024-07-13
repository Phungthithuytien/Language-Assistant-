package com.iiex.languageassistant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.UserTopicRef
import com.iiex.languageassistant.data.model.ref.WordStatus
import java.time.LocalDateTime
import java.time.ZoneOffset

class WordRepository {

    private val db = FirebaseFirestore.getInstance()


    fun updateWordStatusForUser(
        topic_id: String,
        wordId: String,
        userId: String,
        newStatus: WordStatus,
        onComplete: (Boolean) -> Unit
    ) {
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")
            .document(wordId)

        wordCollection.get().addOnSuccessListener { documentSnapshot ->
            val word = documentSnapshot.toObject(Word::class.java)
            if (word != null) {
                val updatedStatusByUser = word.statusByUser.toMutableMap()
                updatedStatusByUser[userId] = newStatus
                val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
                word.updateTime = currentTime

                wordCollection.update(
                    "statusByUser",
                    updatedStatusByUser,
                    "updateTime",
                    currentTime
                )
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        }
    }

    fun mark(
        topic_id: String,
        wordId: String,
        userId: String,
        newStatus: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")
            .document(wordId)

        wordCollection.get().addOnSuccessListener { documentSnapshot ->
            val word = documentSnapshot.toObject(Word::class.java)

            if (word != null) {
                val updatedStatusByUser = word.BookmarkByUser.toMutableMap()

                updatedStatusByUser[userId] = newStatus

                val currentTime = System.currentTimeMillis()
                word.updateTime = currentTime

                wordCollection.update(
                    "bookmarkByUser",
                    updatedStatusByUser,
                    "updateTime",
                    currentTime
                )
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        }
    }

    fun add(topic_id: String, word: Word, onComplete: (Boolean, String) -> Unit): Boolean {
        var flag = false
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")
        wordCollection.add(word)
            .addOnSuccessListener { documentReference ->
                word.id = documentReference.id
                flag = true
                onComplete(true, documentReference.id)
            }
            .addOnFailureListener { e ->
                onComplete(false, "")
            }
        return flag
    }

    fun update(topic_id: String, word: Word, onComplete: (Boolean) -> Unit) {
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")
        val wordRef = wordCollection.document(word.id ?: "")
        wordRef.set(word)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }

    fun getById(topic_id: String, wordId: String, onComplete: (Word?) -> Unit) {
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")
        wordCollection.document(wordId)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val word = documentSnapshot.toObject(Word::class.java)
                onComplete(word)
            }
            .addOnFailureListener { e ->
                onComplete(null)
            }
    }

    fun delete(topic_id: String, wordId: String, onComplete: (Boolean) -> Unit) {
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")
        wordCollection.document(wordId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }

    fun getAllByStatus(
        userID: String,
        topic_id: String,
        status: WordStatus,
        onComplete: (List<Word>) -> Unit
    ) {
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")

        val query = if (status == WordStatus.ALL) {
            wordCollection
        } else if (status == WordStatus.NOT_LEARNED) {
            wordCollection
        } else {
            wordCollection.whereEqualTo("statusByUser.$userID", status)
        }

        val registration = query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                onComplete(emptyList())
                return@addSnapshotListener
            }

            if (querySnapshot != null) {
                val words = mutableListOf<Word>()
                for (document in querySnapshot) {
                    val word = document.toObject(Word::class.java)
                    val updatedStatusByUser = word.BookmarkByUser.toMutableMap()
                    if (updatedStatusByUser[userID] == true) {
                        word.isMarked = true
                    }
                    if (status == WordStatus.NOT_LEARNED) {
                        val statusMap = word.statusByUser.toMutableMap()
                        val ex = statusMap[userID]
                        if (ex == null) {
                            words.add(word)
                        }
                    } else {
                        words.add(word)
                    }
                }
                onComplete(words)
            } else {
                onComplete(emptyList())
            }
        }
    }


}
