package com.iiex.languageassistant.data.model

import com.google.firebase.firestore.DocumentReference
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Folder(
    var id: String? = null,
    var title: String? = null,
    var topicCount: Int = 0,
    var topicRefs: MutableList<DocumentReference>  = mutableListOf(),
    val createTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7")),
    var updateTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
)
