package com.iiex.languageassistant.data.model

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.type.DateTime
import java.io.Serializable
import java.time.LocalDateTime
import java.time.ZoneOffset

data class User @RequiresApi(Build.VERSION_CODES.O) constructor(
    var id: String?=null,
    var email: String?=null,
    var name: String?=null,
    var avatarUrl: String? = "https://firebasestorage.googleapis.com/v0/b/language-assistant-7727.appspot.com/o/Users%2FAvatars%2Favatar_default.png?alt=media&token=490b3731-c6a2-4d1b-a75a-4902372c307b",
    var dataOfBirth: String? = null,
    val createTime: Long =LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7")),
    var updateTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7")),
): Serializable
