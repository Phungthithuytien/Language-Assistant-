package com.iiex.languageassistant

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.firestore.FirebaseFirestore
import com.iiex.languageassistant.data.model.*
import com.iiex.languageassistant.data.model.ref.WordStatus
import org.junit.Before
import com.iiex.languageassistant.data.repository.TopicRepository
import com.iiex.languageassistant.data.repository.WordRepository
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.internal.runners.statements.ExpectException
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.jvm.Throws

@RunWith(JUnit4::class)
class TopicRepositoryTest {

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val db = FirebaseFirestore.getInstance()
    private val topicRepository = TopicRepository()
    private val wordRepository = WordRepository()

    @Before
    fun setUp() {
        // Thực hiện cài đặt trước khi chạy các hàm kiểm tra
    }

    @Test
    @Throws(Exception::class)
    fun testAddWordsToTopic() {

        // Tạo một danh sách từ vựng
        val words = listOf(
            Word(english = "Word1", vietnamese = "Từ 1", imageUrl = null),
            Word(english = "Word2", vietnamese = "Từ 2", imageUrl = null),
            Word(english = "Word3", vietnamese = "Từ 3", imageUrl = null)
        )

        val topic = Topic(
            title = "Tiêu đề của topic",
            description = "Mô tả của topic",
            isPublic = true,
            author = "Alice",
            createTime = System.currentTimeMillis(),
            updateTime = System.currentTimeMillis(),
        )

        var isTopicSuccess = false
        var isWordSuccess = false

        topicRepository.add(topic){ success, addedTopic ->
            if (success) {
                // Thêm thành công
                isTopicSuccess = true
                if (addedTopic != null) {
                    topic.id = addedTopic.id
                    // addedTopic chứa thông tin về chủ đề đã được thêm, bao gồm cả ID sau khi thêm vào Firestore
                    println("Thêm chủ đề thành công. ID của chủ đề: ${addedTopic.id}")
                    topicRepository.addWord(words,addedTopic,){ success ->
                        if (success){
                            isWordSuccess = true
                            println("Thêm từ vụng thành công")
                        }
                    }
                } else {
                    println("Thêm chủ đề thành công nhưng không có thông tin về chủ đề trả về.")
                }
            } else {
                // Thêm thất bại
                println("Thêm chủ đề thất bại.")
            }
        }
        Thread.sleep(5000) // Chờ 5 giây để đảm bảo rằng Firebase có thời gian xử lý
        // Kiểm tra xem cả hai thao tác đã thành công hay không
        assertTrue(isTopicSuccess && isWordSuccess)

    }

    @Test
    @Throws(Exception::class)
    fun testGetTopicByID(){
        topicRepository.getByID("FtceQ0Yvss63sZREFqUg"){
                topic->
            TestCase.assertNotNull(topic)
            if (topic != null) {
                assertEquals(3, topic.wordCount)
                assertEquals("alicee", topic.author)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateTopic(){
        topicRepository.getByID("FtceQ0Yvss63sZREFqUg"){
                topic->
            if (topic != null) {
                topic.title = "Updated kekek"
                topicRepository.update(topic){ succes ->
                        assert(succes)
                }
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testDeleteTopic(){
        topicRepository.delete("0mFhbJVal6kc4iM5R2ZG"){ succes ->
                assert(succes)
        }
        Thread.sleep(10000)
    }

    @Test
    @Throws(Exception::class)
    fun testUpDateWordStatus(){
        wordRepository.updateWordStatusForUser("rBjzcfFET3DsbDrA4VTO","w4","T1WMSIO1hVeD58fPu0kH4dkEg8G2",
            WordStatus.LEARNED){
            s -> assert(s)
        }
        Thread.sleep(2000)
    }


}
