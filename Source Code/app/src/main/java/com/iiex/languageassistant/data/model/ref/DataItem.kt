package com.iiex.languageassistant.data.model.ref
import com.google.firebase.firestore.DocumentReference
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneOffset

sealed class DataItem : Serializable {



    data class Topic(
        val authorID: String?,
        val authorName: String?,
        val authorAvatar: String?,
        val topicID: String?,
        val topicTitle: String?,
        val wordLearned: Int = 0,
        var wordCount: Int = 0,
        val lastAccess: Long? = null,
        val viewCount: Int = 0,
        val updatedAt: Long? = null,
        var isAdded: Boolean? = null
    ) : DataItem(), Serializable

    data class Header(val time: String) : DataItem(), Serializable

    data class Folder(var id: String? = null,
                      var title: String? = null,
                      var topicCount: Int = 0,
                      var isAdded: Boolean? = null,
                      var topicRefs: MutableList<DocumentReference>  = mutableListOf(),
                      val createTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7")),
                      var updateTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
    ) : DataItem(), Serializable

}