package com.iiex.languageassistant.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.DocumentReference
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Topic @RequiresApi(Build.VERSION_CODES.O) constructor(
    var id: String? = null,
    var title: String? = null,
    var description: String? = null,
    var wordCount: Int = 0,
    var viewCount: Int = 0,
    var isPublic: Boolean = false,
    var author: String? = null,
    var authorRef: DocumentReference?=null,
    val createTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7")),
    var updateTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
): Serializable
