package com.iiex.languageassistant.data.model.ref

data class TopicCard(
    val authorID: String?,
    val authorName: String?,
    val authorAvatar: String?,
    val topicID: String?,
    val topicTitle: String?,
    val wordLearned: Int = 0,
    val wordCount: Int = 0,
    val lastAccess: Long?
): DataItem()
