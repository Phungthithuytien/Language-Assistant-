package com.iiex.languageassistant.data.model

import android.os.Build
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.iiex.languageassistant.data.model.ref.WordStatus
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneOffset

data class Word @RequiresApi(Build.VERSION_CODES.O) constructor(
    var id: String? = null,
    var english: String? = null,
    var vietnamese: String? = null,
    var isMarked: Boolean = false,
    val createTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7")),
    var updateTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7")),
    var imageUrl: String? = null,
    var statusByUser: Map<String, WordStatus> = emptyMap(),
    var BookmarkByUser: Map<String, Boolean> = emptyMap()
)
