package com.iiex.languageassistant.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iiex.languageassistant.data.model.Topic
import com.iiex.languageassistant.data.model.User
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.model.ref.LeaderBoard
import com.iiex.languageassistant.data.model.ref.UserTopicRef
import com.iiex.languageassistant.util.DateTimeUtil
import com.iiex.languageassistant.viewmodels.TopicDetailPersonalViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset


class TopicRepository {

    private val db = FirebaseFirestore.getInstance()
    private val userRepository = UserRepository()

    fun add(topic: Topic, onComplete: (Boolean, Topic?) -> Unit) {
        val topicCollection = db.collection("topics")
        topic.authorRef = topic.author?.let { db.collection("users").document(it) }

        topicCollection.add(topic).addOnSuccessListener { documentReference ->
            topic.id = documentReference.id
            onComplete(true, topic)
        }.addOnFailureListener { e ->
            onComplete(false, null)
        }
    }

    //    update words  vào topic
//    PATH: /words/{user_id(author)}/{topic_id}/
    @RequiresApi(Build.VERSION_CODES.O)
    fun addWord(words: List<Word>, topic: Topic, onComplete: (Boolean) -> Unit) {

        val wordCollection = topic.id?.let {
            db.collection("topics").document(it).collection("words")
        }
        for (word in words) {
            if (wordCollection != null) {
                word.id?.let {
                    wordCollection.document(it).set(word)
                        .addOnSuccessListener { documentReference ->

                        }.addOnFailureListener { e ->
                        }
                }
            }
        }

        topic.wordCount = words.size
        // Thêm chủ đề vào Firestore
        val topicCollection = db.collection("topics")
        topic.id?.let { topic_id ->
            topicCollection.document(topic_id).set(topic).addOnSuccessListener {
                onComplete(true)
            }.addOnFailureListener {
                onComplete(false)
            }
        }
        val topicRef = UserTopicRef(
            topicRef = topic.id?.let { topicCollection.document(it) },
        )
        topic.author?.let {
            topic.id?.let { it1 ->
                userRepository.addTopicToUser(
                    it,
                    it1,
                    topicRef
                )
            }
        }
    }

    fun updateWord(words: List<Word>, topic: Topic, onComplete: (Boolean) -> Unit) {
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))

        val batch = db.batch()

        // Xóa toàn bộ collection "words" trong topic
        val wordCollectionRef = topic.id?.let {
            db.collection("topics").document(it).collection("words")
        }

        wordCollectionRef?.get()
            ?.addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    // Thêm mỗi tài liệu vào batch để xóa
                    batch.delete(document.reference)
                }

                for (word in words) {
                    word.updateTime = currentTime
                    val newWordRef = word.id?.let { wordCollectionRef.document(it) }
                    if (newWordRef != null) {
                        batch.set(newWordRef, word)
                    }
                }

                // Update thông tin topic
                topic.wordCount = words.size
                topic.updateTime = currentTime
                val topicDocumentRef = topic.id?.let {
                    db.collection("topics").document(it)
                }
                batch.set(topicDocumentRef!!, topic)

                // Commit batch
                batch.commit()
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false)
                    }
            }
            ?.addOnFailureListener { e ->
                onComplete(false)
            }
    }


    fun updateUsertopicWordLearned(
        userId: String,
        topicID: String,
        newWordLearned: Int,
        onComplete: (Boolean) -> Unit
    ) {
        val userCollection = db.collection("users")
        val userTopicDocument =
            userCollection.document(userId).collection("topics").document(topicID)
        val currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
        userTopicDocument.get()
            .addOnSuccessListener { documentSnapshot ->
                var userTopicRef = documentSnapshot.toObject(UserTopicRef::class.java)

                if (userTopicRef != null) {
                    // Update existing document
                    userTopicDocument.update(
                        "wordLearned", newWordLearned,
                        "lastAccess", currentTime
                    )
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                } else {
                    val topicCollection = db.collection("topics")
                    userTopicRef = UserTopicRef(
                        topicRef = topicCollection.document(topicID),
                        wordLearned = newWordLearned,
                        lastAccess = currentTime
                    )
                    userTopicDocument.set(userTopicRef)
                        .addOnSuccessListener {
                            onComplete(true)
                        }
                        .addOnFailureListener {
                            onComplete(false)
                        }
                }

            }
    }

    fun addLeaderBoard(topicID: String, leaderBoard: LeaderBoard, onComplete: (Boolean) -> Unit) {
        val leaderBoardCollection = db.collection("topics")
            .document(topicID)
            .collection("leaderBoard")

        val userRef = leaderBoard.userRef
        val userEntryQuery = userRef?.let { leaderBoardCollection.document(it.id) }

        if (userEntryQuery != null) {
            userEntryQuery.get().addOnCompleteListener { userEntryTask ->
                if (userEntryTask.isSuccessful) {
                    val userEntryDocument = userEntryTask.result

                    if (userEntryDocument != null && userEntryDocument.exists()) {
                        // User's entry exists, check if the new score is higher
                        val existingScore = userEntryDocument.getLong("score") ?: 0
                        val newScore = leaderBoard.score

                        if (newScore > existingScore) {
                            // Update the entry with the new score
                            if (userRef != null) {
                                leaderBoardCollection.document(userRef.id).set(leaderBoard)
                                    .addOnSuccessListener {
                                        onComplete(true)
                                    }
                                    .addOnFailureListener {
                                        onComplete(false)
                                    }
                            }
                        } else {
                            // New score is not higher, no update needed
                            onComplete(false)
                        }
                    } else {
                        // User's entry does not exist, create a new one
                        leaderBoardCollection.document(userRef.id).set(leaderBoard)
                            .addOnSuccessListener {
                                onComplete(true)
                            }
                            .addOnFailureListener {
                                onComplete(false)
                            }
                    }
                } else {
                    // Error occurred while querying the leaderboard
                    onComplete(false)
                }
            }
        }
    }

    fun incrementViewCount(topicID: String, onComplete: (Boolean) -> Unit) {
        val topicDocument = db.collection("topics").document(topicID)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(topicDocument)
            val currentCount = snapshot.getLong("viewCount") ?: 0
            transaction.update(topicDocument, "viewCount", currentCount + 1)
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun getTopTopics(onComplete: (List<DataItem.Topic>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val topicsQuerySnapshot = db.collection("topics")
                    .whereEqualTo("public", true)
                    .orderBy("viewCount", Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()

                val topics = topicsQuerySnapshot.documents.mapNotNull { document ->
                    val topic = document.toObject(Topic::class.java)?.apply { id = document.id }
                    if (topic != null) {
                        val authorSnapshot =
                            topic.author?.let { db.collection("users").document(it).get().await() }
                        val author = authorSnapshot?.toObject(User::class.java)
                        if (author != null) {
                            DataItem.Topic(
                                authorID = author.id,
                                authorName = author.name,
                                authorAvatar = author.avatarUrl,
                                topicID = topic.id,
                                topicTitle = topic.title,
                                wordCount = topic.wordCount,
                                viewCount = topic.viewCount,
                                updatedAt = topic.updateTime
                            )
                        } else null
                    } else null
                }

                withContext(Dispatchers.Main) {
                    onComplete(topics)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(emptyList())
                }
            }
        }
    }

    fun getNewTopics(
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): LiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>> {
        val result = MutableLiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>>()
        val topicsCollection = db.collection("topics")

        var query = topicsCollection
            .orderBy("createTime", Query.Direction.DESCENDING)
            .whereEqualTo("public", true)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                result.value = Pair(emptyList(), Pair(false, null))
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val topicsDeferred = querySnapshot?.documents?.map { topicDocument ->
                        async {
                            val topic = topicDocument?.toObject(Topic::class.java)
                            val authorSnapshot =
                                topic?.author?.let {
                                    db.collection("users").document(it).get().await()
                                }
                            val author = authorSnapshot?.toObject(User::class.java)
                            if (topic != null && author != null) {
                                DataItem.Topic(
                                    authorID = author.id,
                                    authorName = author.name,
                                    authorAvatar = author.avatarUrl,
                                    topicID = topic.id,
                                    topicTitle = topic.title,
                                    wordCount = topic.wordCount,
                                    viewCount = topic.viewCount,
                                    updatedAt = topic.updateTime
                                )
                            } else {
                                null
                            }
                        }
                    } ?: listOf()

                    val userTopics = topicsDeferred.awaitAll().filterNotNull()
                    val hasNextPage = (querySnapshot?.size() ?: 0) >= pageSize

                    withContext(Dispatchers.Main) {
                        val newLastDocument = querySnapshot?.documents?.lastOrNull()
                        result.value = Pair(userTopics, Pair(hasNextPage, newLastDocument))
                    }
                }
            }
        }
        return result
    }

    fun filterByTopicTitle(
        topTitle: String,
    ): LiveData<List<DataItem.Topic>> {
        val result = MutableLiveData<List<DataItem.Topic>>()
        val topicsCollection = db.collection("topics")

        var query = topicsCollection
            .orderBy("createTime", Query.Direction.DESCENDING)
            .whereEqualTo("public", true)

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val topicsDeferred = querySnapshot?.documents?.map { topicDocument ->
                    async {
                        val topic = topicDocument?.toObject(Topic::class.java)
                        if (topic != null) {
                            if (topic.title?.contains(topTitle, true) == true) {
                                val authorSnapshot =
                                    topic.author?.let {
                                        db.collection("users").document(it).get().await()
                                    }
                                val author = authorSnapshot?.toObject(User::class.java)
                                if (author != null) {
                                    DataItem.Topic(
                                        authorID = author.id,
                                        authorName = author.name,
                                        authorAvatar = author.avatarUrl,
                                        topicID = topic.id,
                                        topicTitle = topic.title,
                                        wordCount = topic.wordCount,
                                        viewCount = topic.viewCount,
                                        updatedAt = topic.updateTime
                                    )
                                } else {
                                    null
                                }
                            } else {
                                null
                            }
                        } else {
                            null
                        }

                    }
                } ?: listOf()
                val userTopics = topicsDeferred.awaitAll().filterNotNull()
                withContext(Dispatchers.Main) {
                    result.value = userTopics
                }
            }
        }
        return result
    }


    fun getLeaderBoard(
        topicID: String,
        onComplete: (List<TopicDetailPersonalViewModel.RankItem>) -> Unit
    ) {
        val leaderBoardCollection = db.collection("topics")
            .document(topicID)
            .collection("leaderBoard")
        val query = leaderBoardCollection
            .orderBy("score", Query.Direction.DESCENDING)
            .orderBy("timeDuration", Query.Direction.ASCENDING)
            .orderBy("submitted", Query.Direction.ASCENDING)
            .limit(10)

        val registration = query.addSnapshotListener { querySnapshot, error ->
            if (error != null) {
                onComplete(emptyList())
                return@addSnapshotListener
            }
            if (querySnapshot != null) {
                val leaderBoardList = mutableListOf<TopicDetailPersonalViewModel.RankItem>()
                val fetchUserPromises = mutableListOf<Task<DocumentSnapshot>>()

                for (document in querySnapshot) {
                    val leaderBoard = document.toObject(LeaderBoard::class.java)
                    val userRef = leaderBoard.userRef

                    val fetchUserPromise =
                        userRef?.get()?.addOnSuccessListener { documentSnapshot ->
                            val user = documentSnapshot.toObject(User::class.java)
                            val avatarUrl = user?.avatarUrl
                            val name = user?.name
                            val timeDuration = leaderBoard.timeDuration ?: 0
                            val time = formatTimeDuration(timeDuration)
                            val date = DateTimeUtil.getDateFromTimestamp(leaderBoard.submitted ?: 0)
                            val top = "Top #${leaderBoardList.size + 1}"

                            avatarUrl?.let {
                                if (name != null) {
                                    val rankItem = TopicDetailPersonalViewModel.RankItem(
                                        it,
                                        name,
                                        time,
                                        date,
                                        top
                                    )
                                    leaderBoardList.add(rankItem)
                                }
                            }
                        }

                    if (fetchUserPromise != null) {
                        fetchUserPromises.add(fetchUserPromise)
                    }
                }
                Tasks.whenAllSuccess<Void>(*fetchUserPromises.toTypedArray())
                    .addOnCompleteListener {
                        onComplete(leaderBoardList)
                    }
            }
        }
    }

    fun formatTimeDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        val timeStringBuilder = StringBuilder()

        if (hours > 0) {
            timeStringBuilder.append("${hours}h")
        }

        if (minutes > 0 || hours > 0) {
            timeStringBuilder.append("${minutes}p")
        }

        timeStringBuilder.append("${remainingSeconds}")

        return timeStringBuilder.toString()
    }


    fun getByID(topicID: String, onComplete: (Topic?) -> Unit) {
        val topicsCollection = db.collection("topics")
        val topicDocRef = topicsCollection.document(topicID)

        topicDocRef.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                onComplete(null)
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val topic = documentSnapshot.toObject(Topic::class.java)
                onComplete(topic)
            } else {
                onComplete(null)
            }
        }
    }

    fun update(topic: Topic, onComplete: (Boolean) -> Unit) {
        val topicsCollection = db.collection("topics")
        topic.id?.let {
            topicsCollection.document(it).set(topic).addOnSuccessListener {
                onComplete(true)
            }.addOnFailureListener {
                onComplete(false)
            }
        }
    }

    fun delete(topicID: String, onComplete: (Boolean) -> Unit) {
        val topicGroup = db.collectionGroup("topics")
        val topicRef = db.collection("topics").document(topicID)
        val query = topicGroup.whereEqualTo("topicRef", topicRef)

        query.get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    document.reference.delete()
                        .addOnFailureListener { e ->
                            onComplete(false)
                        }
                }
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }

        val leaderBoardCollection = db.collection("topics").document(topicID).collection("leaderBoard")
        leaderBoardCollection.get().addOnSuccessListener { leaderBoardQuerySnapshot ->
            for (leaderBoardDocument in leaderBoardQuerySnapshot.documents) {
                leaderBoardDocument.reference.delete()
                    .addOnFailureListener {
                        onComplete(false)
                        return@addOnFailureListener
                    }
            }

            // Delete documents in the "words" collection
            val wordsCollection = db.collection("topics").document(topicID).collection("words")
            wordsCollection.get().addOnSuccessListener { wordsQuerySnapshot ->
                for (wordDocument in wordsQuerySnapshot.documents) {
                    wordDocument.reference.delete()
                        .addOnFailureListener {
                            onComplete(false)
                            return@addOnFailureListener
                        }
                }

                // Delete the topic document
                val topicsCollection = db.collection("topics")
                topicsCollection.document(topicID).delete()
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }.addOnFailureListener {
                onComplete(false)
            }
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun setPublic(topicID: String, isPublic: Boolean, onComplete: (Boolean) -> Unit) {
        val topicsCollection = db.collection("topics")
        topicsCollection.document(topicID).update("public", isPublic).addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }


    fun loadTopics(onComplete: (List<Topic>) -> Unit) {
        val topicCollection = db.collection("topics")
        topicCollection.get().addOnSuccessListener { result ->
            val topics = result.toObjects(Topic::class.java)
            onComplete(topics)
        }.addOnFailureListener { e ->
            onComplete(emptyList())
        }
    }

    fun loadWords(topic_id: String, onComplete: (List<Word>) -> Unit) {
        val wordCollection = db.collection("topics")
            .document(topic_id)
            .collection("words")

        wordCollection.get().addOnSuccessListener { result ->
            val words = result.toObjects(Word::class.java)
            onComplete(words)
        }.addOnFailureListener { e ->
            onComplete(emptyList())
        }
    }

}
