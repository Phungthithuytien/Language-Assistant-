package com.iiex.languageassistant

import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Test
import kotlin.jvm.Throws
import com.iiex.languageassistant.data.model.*
import com.iiex.languageassistant.data.model.ref.UserTopicRef
import org.junit.Before
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.UserRepository
import com.iiex.languageassistant.data.repository.WordRepository
import junit.framework.TestCase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.sql.Date
import java.time.LocalDateTime
import java.time.Month
import kotlin.random.Random


@RunWith(JUnit4::class)
class UserRepositoryTest {
    val userRepository = UserRepository()
    private val db = FirebaseFirestore.getInstance()

    private val topicRepository = TopicRepository()
    private val wordRepository = WordRepository()
    val user = User(
        id = "T1WMSIO1hVeD58fPu0kH4dkEg8G2",
        email = "example@example.com",
        name = "John Doe",
        avatarUrl = "https://example.com/avatar.jpg",
        dataOfBirth = "1/1/2000",  // Thời gian Unix epoch, tương ứng với ngày 1/1/2000
        createTime = 1634908800000, // Thời gian Unix epoch, tương ứng với ngày 22/10/2021
        updateTime = 1634908800000, // Thời gian Unix epoch, tương ứng với ngày 22/10/2021
        )
    val author = User(
        id = "jQBsoZuLugWdlbCPWEDLShzw6tU2",
        email = "example@example.com",
        name = "John Doe",
        avatarUrl = "https://example.com/avatar.jpg",
        dataOfBirth = "1/1/2000",  // Thời gian Unix epoch, tương ứng với ngày 1/1/2000
        createTime = 1634908800000, // Thời gian Unix epoch, tương ứng với ngày 22/10/2021
        updateTime = 1634908800000, // Thời gian Unix epoch, tương ứng với ngày 22/10/2021
       )

    @Before
    fun setUp() {
        // Thực hiện cài đặt trước khi chạy các hàm kiểm tra
    }
    @Test
    @Throws(Exception::class)
    fun testAddTopic() {

        val vocabularyList = listOf(
            Pair("music", "âm nhạc"),
            Pair("guitar", "đàn guitar"),
            Pair("piano", "đàn piano"),
            Pair("singer", "ca sĩ"),
            Pair("song", "bài hát"),
            Pair("football", "bóng đá"),
            Pair("basketball", "bóng rổ"),
            Pair("swimming", "bơi lội"),
            Pair("mountain", "núi"),
            Pair("beach", "bãi biển"),
            Pair("camera", "máy ảnh"),
            Pair("book", "sách"),
            Pair("computer", "máy tính"),
            Pair("phone", "điện thoại"),
            Pair("restaurant", "nhà hàng"),
            Pair("coffee", "cà phê"),
            Pair("movie", "phim"),
            Pair("travel", "du lịch"),
            Pair("art", "nghệ thuật"),
            Pair("science", "khoa học")
        )
        val cookingTopics = listOf(
            Pair("Chủ đề nấu ăn mỳ ý", "Các công thức và bí quyết nấu mỳ ý ngon"),
            Pair("Chủ đề nấu ăn sushi", "Hướng dẫn làm sushi tại nhà"),
            Pair("Chủ đề nấu ăn bánh mì", "Cách làm bánh mì tươi ngon"),
            Pair("Chủ đề nấu ăn thịt nướng", "Cách chế biến thịt nướng và sốt phù hợp"),
            Pair("Chủ đề nấu ăn lẩu", "Công thức và cách làm lẩu ngon"),
            Pair("Chủ đề nấu ăn làm bánh", "Cách làm bánh ngọt và bánh tráng miệng"),
            Pair("Chủ đề nấu ăn món Trung Hoa", "Các món ăn Trung Hoa phổ biến"),
            Pair("Chủ đề nấu ăn món Ấn Độ", "Hướng dẫn nấu món ăn Ấn Độ"),
            Pair("Chủ đề nấu ăn món Việt Nam", "Các món ngon của ẩm thực Việt Nam"),
            Pair("Chủ đề nấu ăn món Pháp", "Sưu tập công thức nấu ăn Pháp"),
            Pair("Chủ đề nấu ăn món Ý", "Các món ăn Ý truyền thống"),
            Pair("Chủ đề nấu ăn món Thái Lan", "Hướng dẫn nấu món ăn Thái Lan"),
            Pair("Chủ đề nấu ăn món Nhật Bản", "Công thức và cách làm món ăn Nhật Bản"),
            Pair("Chủ đề nấu ăn món Hàn Quốc", "Các món ăn Hàn Quốc truyền thống"),
            Pair("Chủ đề nấu ăn món Trung Quốc", "Hướng dẫn nấu món ăn Trung Quốc"),
            Pair("Chủ đề nấu ăn món Địa Trung Hải", "Công thức và món ăn Địa Trung Hải")
        )
        var isTopicSuccess = false
        var isWordSuccess = false



        for (i in 1..cookingTopics.size-1){
            val words = mutableListOf<Word>()
            val time = generateRandomTimestamp()
            for (i in 1..Random.nextInt(5, 25)) {
                val randomIndex = Random.nextInt(0, vocabularyList.size)
                val (english, vietnamese) = vocabularyList[randomIndex]
                words.add(Word(id = "w"+ i ,
                    english=  english,
                    vietnamese = vietnamese,
                    imageUrl =  null,
                    createTime = time,
                    updateTime = time))
            }
            val (title, description) = cookingTopics[i]
            val topic = Topic(
                title = title,
                description = description,
                isPublic = true,
                author = author.id,
                createTime = time,
                updateTime = time,
            )
            topicRepository.add(topic){ success, addedTopic ->
                if (success) {
                    // Thêm thành công
                    isTopicSuccess = true
                    runBlocking {
                        launch(Dispatchers.IO) {
                            if (addedTopic != null && author.id != null) {
                                topic.id = addedTopic.id
                                // addedTopic chứa thông tin về chủ đề đã được thêm, bao gồm cả ID sau khi thêm vào Firestore
                                println("Thêm chủ đề thành công. ID của chủ đề: ${addedTopic.id}")
                                topicRepository.addWord(words,addedTopic){ success ->
                                    if (success){
                                        isWordSuccess = true
                                        println("Thêm từ vụng thành công")
                                    }
                                }
                            } else {
                                println("Thêm chủ đề thành công nhưng không có thông tin về chủ đề trả về.")
                            }
                        }
                    }

                } else {
                    println("Thêm chủ đề thất bại.")
                }
            }
            Thread.sleep(2000)
        }


         // Chờ 5 giây để đảm bảo rằng Firebase có thời gian xử lý
        // Kiểm tra xem cả hai thao tác đã thành công hay không
        assertTrue(isTopicSuccess && isWordSuccess)

    }
    @Test
    @Throws(Exception::class)
    fun testAddUser(){
        userRepository.addUser(user){
            it ->assert(it)
        }
        Thread.sleep(1000)

    }

    fun generateRandomTimestamp(): Long {
        val startDate = LocalDateTime.of(2023, Month.MARCH, 1, 0, 0, 0)
        val endDate = LocalDateTime.of(2023, Month.OCTOBER, 31, 23, 59, 59)

        val startTimestamp = startDate.toEpochSecond(java.time.ZoneOffset.UTC)
        val endTimestamp = endDate.toEpochSecond(java.time.ZoneOffset.UTC)

        val randomTimestamp = Random.nextLong(startTimestamp, endTimestamp)

        return randomTimestamp
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserById() {
        userRepository.getUserById(user.id ?: ""){retrievedUser->
            assertNotNull(retrievedUser)
            assertEquals(user.name, retrievedUser?.name)
            assertEquals(user.email, retrievedUser?.email)
        }
    }
    @Test
    @Throws(Exception::class)
    fun addTopicToUser(){
        val topicIDs = listOf(
            "0S3JQDCeFYBsnhZmyxE7",
            "4HwY9aCPXfIzJsdj6juQ",
            "9wPtetQioN6xGBc9eKxk",
            "A8orMUZi2qNpIPy6n5QP",
            "FEMfMwkNhIWnFmvUU6iH",
            "Gk33Y9gYE2dgmv5pU8nt",
            "I41iDKDcJlgR3xR6cs53",
            "Ip96MGkDmv2gQD7G4mT6",
            "PR5T1qvw0kxnbnWSkD16",
            "SpbjfNWQ3jDtlPa4VD52",
            "ZLrOJmXe6ndjhNPZXpxe",
            "koXWqONp6CplMv9NPqPq",
            "mGtDzIhGZTdL9MfTtVcM",
            "vtPUvb5wAeJS70b2dZ1L",
            "yybe5L9YP7WgbyTI7DWY"
        )

        for(i in 0..topicIDs.size-1){
            val time = generateRandomTimestamp()
            val topicRef =UserTopicRef(db.collection("topics").document(topicIDs[i]) , 0, time)
            userRepository.addTopicToUser(user.id ?: "",topicIDs[i], topicRef)
            Thread.sleep(5000)
        }
    }

    @Test
    @Throws(Exception::class)
    fun testGetUserTopics() {

    }
}