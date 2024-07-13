package com.iiex.languageassistant.data.model.ref

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.firestore.DocumentReference
import java.time.LocalDateTime
import java.time.ZoneOffset

data class UserTopicRef @RequiresApi(Build.VERSION_CODES.O) constructor(
    var topicRef: DocumentReference? = null,
    var wordLearned: Int = 0,
    var lastAccess: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
)
