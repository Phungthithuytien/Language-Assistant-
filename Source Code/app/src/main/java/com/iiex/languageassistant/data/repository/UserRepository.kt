package com.iiex.languageassistant.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.iiex.languageassistant.data.model.Folder
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.data.model.Topic
import com.iiex.languageassistant.data.model.User
import com.iiex.languageassistant.data.model.ref.UserTopicRef
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.ZoneOffset

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun addUser(user: User, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        var f = false
        val task = user.id?.let {
            userCollection.document(it).set(user)
                .addOnSuccessListener {
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    onComplete(false)
                }
        }
    }

    fun updateUser(user: User, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val userRef = userCollection.document(user.id ?: "")
        userRef.set(user)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }

    fun getUserById(userId: String, onComplete: (User?) -> Unit) {
        val userCollection = db.collection("users")
        val userDocRef = userCollection.document(userId)

        val registration =
            userDocRef.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    onComplete(null)
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    val user = documentSnapshot.toObject(User::class.java)
                    onComplete(user)
                } else {
                    onComplete(null)
                }
            }

    }


    fun deleteUser(userId: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        userCollection.document(userId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }

    fun addTopicToUser(userId: String, topicId: String, newTopicRef: UserTopicRef) {
        val userCollection = db.collection("users")
        val userTopicCollection = userCollection.document(userId).collection("topics")

        userTopicCollection.document(topicId).set(newTopicRef)
    }

    fun addFolder(userId: String, folder: Folder, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val userTopicCollection = userCollection.document(userId).collection("folders")

        userTopicCollection.add(folder)
            .addOnSuccessListener { documentReference ->
                folder.id = documentReference.id

                documentReference.update("id", documentReference.id)
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            }
            .addOnFailureListener {
                onComplete(false)
            }
    }


    fun getUserTopic(userId: String, topicId: String, onComplete: (DataItem.Topic?) -> Unit) {
        val userCollection = db.collection("users")
        val topicsSubCollection = userCollection.document(userId).collection("topics")
        topicsSubCollection.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                // Get the TopicRef for the specific topicId
                val topicRefSnapshot = topicsSubCollection.document(topicId).get().await()
                val topicRef = topicRefSnapshot.toObject(UserTopicRef::class.java)

                if (topicRef != null) {
                    // Get the actual Topic from the topics collection
                    val topicSnapshot = topicRef.topicRef?.get()?.await()
                    val topic = topicSnapshot?.toObject(Topic::class.java)

                    // Get the author User from the users collection
                    val authorSnapshot = topic?.authorRef?.get()?.await()
                    val author = authorSnapshot?.toObject(User::class.java)

                    if (topic != null && author != null) {
                        val userTopic = DataItem.Topic(
                            author.id,
                            author.name,
                            author.avatarUrl,
                            topic.id,
                            topic.title,
                            topicRef.wordLearned,
                            topic.wordCount,
                            topicRef.lastAccess
                        )
                        withContext(Dispatchers.Main) {
                            onComplete(userTopic)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onComplete(null)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val userTopic = DataItem.Topic(
                            "",
                            "",
                            "",
                            "",
                            "",
                            0,
                            0,
                        )
                        onComplete(userTopic)
                    }
                }
            }
        }
    }


    fun getUserTopics(
        userId: String,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): LiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>> {
        val result = MutableLiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>>()
        val userCollection = db.collection("users")
        val topicsSubCollection = userCollection.document(userId).collection("topics")

        var query = topicsSubCollection
            .orderBy("lastAccess", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val topicsDeferred = querySnapshot?.documents?.map { topicRefDocument ->
                    async {
                        val topicRef = topicRefDocument.toObject(UserTopicRef::class.java)
                        val topicSnapshot = topicRef?.topicRef?.get()?.await()
                        val topic = topicSnapshot?.toObject(Topic::class.java)
                        val authorSnapshot = topic?.authorRef?.get()?.await()
                        val author = authorSnapshot?.toObject(User::class.java)

                        if (topic != null && author != null && topicRef != null) {
                            DataItem.Topic(
                                author.id,
                                author.name,
                                author.avatarUrl,
                                topic.id,
                                topic.title,
                                topicRef.wordLearned,
                                topic.wordCount,
                                topicRef.lastAccess
                            )
                        } else {
                            null
                        }
                    }
                } ?: listOf()

                val userTopics = topicsDeferred.awaitAll().filterNotNull()
                val hasNextPage = querySnapshot?.size() ?: 0 >= pageSize

                withContext(Dispatchers.Main) {
                    val newLastDocument = querySnapshot?.documents?.lastOrNull()
                    result.value = Pair(userTopics, Pair(hasNextPage, newLastDocument))
                }
            }
        }
        return result
    }

    fun getUserTopicsByFolder(
        userId: String,
        folder: DataItem.Folder,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): LiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>> {
        val result = MutableLiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>>()

        val userCollection = db.collection("users")
        val topicsSubCollection = userCollection.document(userId).collection("topics")

        if (folder.topicRefs.isEmpty()){
            result.value = Pair(emptyList(), Pair(false, null))
            return result
        }
        var query = topicsSubCollection
            .whereIn(FieldPath.documentId(), folder.topicRefs.map { it.id })
            .orderBy("lastAccess", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val topicsDeferred = querySnapshot?.documents?.map { topicRefDocument ->
                    async {
                        val topicRef = topicRefDocument.toObject(UserTopicRef::class.java)
                        val topicSnapshot = topicRef?.topicRef?.get()?.await()
                        val topic = topicSnapshot?.toObject(Topic::class.java)
                        val authorSnapshot = topic?.authorRef?.get()?.await()
                        val author = authorSnapshot?.toObject(User::class.java)

                        if (topic != null && author != null && topicRef != null) {
                            DataItem.Topic(
                                author.id,
                                author.name,
                                author.avatarUrl,
                                topic.id,
                                topic.title,
                                topicRef.wordLearned,
                                topic.wordCount,
                                topicRef.lastAccess
                            )
                        } else {
                            null
                        }
                    }
                } ?: listOf()

                val userTopics = topicsDeferred.awaitAll().filterNotNull()
                val hasNextPage = querySnapshot?.size() ?: 0 >= pageSize

                withContext(Dispatchers.Main) {
                    val newLastDocument = querySnapshot?.documents?.lastOrNull()
                    result.value = Pair(userTopics, Pair(hasNextPage, newLastDocument))
                }
            }
        }
        return result
    }

    fun getUserTopics(
        userId: String,
        folder: DataItem.Folder,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): LiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>> {
        val result = MutableLiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>>()

        val userCollection = db.collection("users")
        val topicsSubCollection = userCollection.document(userId).collection("topics")

        var query = topicsSubCollection
            .orderBy("lastAccess", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }

            CoroutineScope(Dispatchers.IO).launch {
                val topicsDeferred = querySnapshot?.documents?.map { topicRefDocument ->
                    async {
                        val topicRef = topicRefDocument.toObject(UserTopicRef::class.java)
                        val topicSnapshot = topicRef?.topicRef?.get()?.await()
                        val topic = topicSnapshot?.toObject(Topic::class.java)
                        val authorSnapshot = topic?.authorRef?.get()?.await()
                        val author = authorSnapshot?.toObject(User::class.java)

                        if (topic != null && author != null && topicRef != null) {
                            var isAdded = false
                            if (folder.topicRefs.any { it.id == topic.id }) {
                                isAdded = true
                            }
                            DataItem.Topic(
                                author.id,
                                author.name,
                                author.avatarUrl,
                                topic.id,
                                topic.title,
                                topicRef.wordLearned,
                                topic.wordCount,
                                topicRef.lastAccess,
                                isAdded = isAdded
                            )
                        } else {
                            null
                        }
                    }
                } ?: listOf()

                val userTopics = topicsDeferred.awaitAll().filterNotNull()
                val hasNextPage = querySnapshot?.size() ?: 0 >= pageSize

                withContext(Dispatchers.Main) {
                    val newLastDocument = querySnapshot?.documents?.lastOrNull()
                    result.value = Pair(userTopics, Pair(hasNextPage, newLastDocument))
                }
            }
        }
        return result
    }

    fun getUserFolders(
        userId: String,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): LiveData<Pair<List<DataItem.Folder>, Pair<Boolean, DocumentSnapshot?>>> {
        val result = MutableLiveData<Pair<List<DataItem.Folder>, Pair<Boolean, DocumentSnapshot?>>>()
        val userCollection = db.collection("users")
        val topicsSubCollection = userCollection.document(userId).collection("folders")

        var query = topicsSubCollection
            .orderBy("updateTime", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val topicsDeferred = querySnapshot?.documents?.map { topicRefDocument ->
                    async {
                        val folder = topicRefDocument.toObject(DataItem.Folder::class.java)
                        if (folder != null ) {
                            folder
                        } else {
                            null
                        }
                    }
                } ?: listOf()

                val userTopics = topicsDeferred.awaitAll().filterNotNull()
                val hasNextPage = querySnapshot?.size() ?: 0 >= pageSize

                withContext(Dispatchers.Main) {
                    val newLastDocument = querySnapshot?.documents?.lastOrNull()
                    result.value = Pair(userTopics, Pair(hasNextPage, newLastDocument))
                }
            }
        }
        return result
    }

    fun getUserFolders(userId: String, topicID: String): LiveData<List<DataItem.Folder>> {
        val result = MutableLiveData<List<DataItem.Folder>>()
        val userCollection = db.collection("users")
        val foldersSubCollection = userCollection.document(userId).collection("folders")

        foldersSubCollection
            .orderBy("updateTime", Query.Direction.DESCENDING)
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    result.value = listOf()
                    return@addSnapshotListener
                }
                val folders = querySnapshot?.documents?.mapNotNull { document ->
                    val folder = document.toObject(DataItem.Folder::class.java)
                    folder?.apply {
                        isAdded = topicID in topicRefs.map { it.id }
                    }
                }.orEmpty()
                result.postValue(folders)
            }

        return result
    }

    fun addIntoFolder(userID: String, topicID: String, folderID: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val foldersSubCollection = userCollection.document(userID).collection("folders").document(folderID)
        foldersSubCollection.get().addOnSuccessListener { documentSnap ->
            val folder = documentSnap.toObject(DataItem.Folder::class.java)
            if (folder != null) {
                folder.topicRefs.add(db.collection("topics").document(topicID))
                folder.topicCount++
                folder.updateTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))

                // Update the folder in Firestore
                foldersSubCollection.set(folder)
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        }
    }

    fun removeFromFolder(userID: String, topicID: String, folderID: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val foldersSubCollection = userCollection.document(userID).collection("folders").document(folderID)

        foldersSubCollection.get().addOnSuccessListener { documentSnap ->
            val folder = documentSnap.toObject(DataItem.Folder::class.java)
            if (folder != null) {
                folder.topicRefs.remove(db.collection("topics").document(topicID))
                folder.topicCount--
                folder.updateTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
                foldersSubCollection.set(folder)
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        }
    }

    fun editFolder(userID: String, folderTitle: String, folderID: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val foldersSubCollection = userCollection.document(userID).collection("folders").document(folderID)

        foldersSubCollection.get().addOnSuccessListener { documentSnap ->
            val folder = documentSnap.toObject(DataItem.Folder::class.java)
            if (folder != null) {
                folder.title = folderTitle
                folder.updateTime = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
                foldersSubCollection.set(folder)
                    .addOnSuccessListener {
                        onComplete(true)
                    }
                    .addOnFailureListener {
                        onComplete(false)
                    }
            } else {
                onComplete(false)
            }
        }
    }
    fun deleteFolder(userID: String, folderID: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val foldersSubCollection = userCollection.document(userID).collection("folders").document(folderID)

        foldersSubCollection.delete().addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }


    fun getFolderByID(userID: String, folderID: String, onComplete: (DataItem.Folder?) -> Unit) {
        val userCollection = db.collection("users")
        val foldersSubCollection = userCollection.document(userID).collection("folders").document(folderID)
        foldersSubCollection.get().addOnSuccessListener { documentSnap ->
            val folder = documentSnap.toObject(DataItem.Folder::class.java)
            onComplete(folder)
        }
    }

    fun getAuthorTopics(
        userId: String,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): LiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>> {
        val result = MutableLiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>>()
        val topicsCollection = db.collection("topics")

        var query = topicsCollection
            .whereEqualTo("author", userId)
            .whereEqualTo("public", true)
            .orderBy("updateTime", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val authorSnapshot = db.collection("users").document(userId).get().await()
                val author = authorSnapshot?.toObject(User::class.java)
                val topicsDeferred = querySnapshot?.documents?.map { topicDocument ->
                    async {
                        val topic = topicDocument?.toObject(Topic::class.java)
                        if (topic != null && author != null) {
                            DataItem.Topic(
                                authorID = author.id,
                                authorName = author.name,
                                authorAvatar = author.avatarUrl,
                                topicID = topic.id,
                                topicTitle = topic.title,
                                wordCount = topic.wordCount,
                                viewCount =  topic.viewCount,
                                updatedAt = topic.updateTime
                            )
                        } else {
                            null
                        }
                    }
                } ?: listOf()

                val userTopics = topicsDeferred.awaitAll().filterNotNull()
                val hasNextPage = querySnapshot?.size() ?: 0 >= pageSize

                withContext(Dispatchers.Main) {
                    val newLastDocument = querySnapshot?.documents?.lastOrNull()
                    result.value = Pair(userTopics, Pair(hasNextPage, newLastDocument))
                }
            }
        }
        return result
    }

    fun getPersonalTopics(
        userId: String,
        lastDocument: DocumentSnapshot?,
        pageSize: Int
    ): LiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>> {
        val result = MutableLiveData<Pair<List<DataItem.Topic>, Pair<Boolean, DocumentSnapshot?>>>()
        val topicsCollection = db.collection("topics")

        var query = topicsCollection
            .whereEqualTo("author", userId)
            .orderBy("updateTime", Query.Direction.DESCENDING)
            .limit(pageSize.toLong())

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        query.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                return@addSnapshotListener
            }
            CoroutineScope(Dispatchers.IO).launch {
                val authorSnapshot = db.collection("users").document(userId).get().await()
                val author = authorSnapshot?.toObject(User::class.java)
                val topicsDeferred = querySnapshot?.documents?.map { topicDocument ->
                    async {
                        val topic = topicDocument?.toObject(Topic::class.java)
                        if (topic != null && author != null) {
                            DataItem.Topic(
                                authorID = author.id,
                                authorName = author.name,
                                authorAvatar = author.avatarUrl,
                                topicID = topic.id,
                                topicTitle = topic.title,
                                wordCount = topic.wordCount,
                                viewCount =  topic.viewCount,
                                updatedAt = topic.updateTime
                            )
                        } else {
                            null
                        }
                    }
                } ?: listOf()

                val userTopics = topicsDeferred.awaitAll().filterNotNull()
                val hasNextPage = querySnapshot?.size() ?: 0 >= pageSize

                withContext(Dispatchers.Main) {
                    val newLastDocument = querySnapshot?.documents?.lastOrNull()
                    result.value = Pair(userTopics, Pair(hasNextPage, newLastDocument))
                }
            }
        }
        return result
    }

    fun updateAvatar(userID: String, avatarUrl: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val userRef = userCollection.document(userID)

        userRef.update("avatarUrl",avatarUrl)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }

    fun updateName(userID: String, name: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val userRef = userCollection.document(userID)
        userRef.update("name",name)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }

    fun updateDayOfBirth(userID: String, dateOfBirth: String, onComplete: (Boolean) -> Unit) {
        val userCollection = db.collection("users")
        val userRef = userCollection.document(userID)
        userRef.update("dataOfBirth",dateOfBirth)
            .addOnSuccessListener {
                onComplete(true)
            }
            .addOnFailureListener { e ->
                onComplete(false)
            }
    }

}

