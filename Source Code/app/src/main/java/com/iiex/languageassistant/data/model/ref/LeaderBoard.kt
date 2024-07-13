package com.iiex.languageassistant.data.model.ref

import com.google.firebase.firestore.DocumentReference
import java.time.LocalDateTime
import java.time.ZoneOffset

data class LeaderBoard(
    var userRef: DocumentReference? = null,
    var score: Int=0,
    var timeDuration: Long = 0,
    var submitted:  Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
)
