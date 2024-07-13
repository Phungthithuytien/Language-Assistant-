package com.iiex.languageassistant.ui.fragments

import android.content.ContentValues.TAG
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.databinding.FragmentMultipleChoiceBinding
import com.iiex.languageassistant.viewmodels.MultipleChoiceFragViewModel
import com.iiex.languageassistant.viewmodels.Question
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList


class MultipleChoiceFragment(val topicID: String, val topicTitle: String) : Fragment() {
    var audioMode = true
    private lateinit var binding: FragmentMultipleChoiceBinding
    private lateinit var viewModel: MultipleChoiceFragViewModel

    private val startTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
    private var itemList: List<Question> = emptyList()
    private var englishQuestion: List<Question> = emptyList()
    private var vietnameseQuestion: List<Question> = emptyList()
    private val DELAY_MILLIS = 1000
    private var doneQuestion: Int = 0;
    private var listLayout = listOf<LinearLayout>()
    private var listRadioButton = listOf<RadioButton>()
    private var current: Int = 0
    private var isEnglish : Boolean = true
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_multiple_choice, container, false
        )
        viewModel = ViewModelProvider(this)[MultipleChoiceFragViewModel::class.java]
        binding.lifecycleOwner = this
        FirebaseAuth.getInstance().uid?.let { viewModel.setTopic(it, topicID) }
        viewModel.word.observe(this) { words ->
            itemList = viewModel.getList(false)
            englishQuestion = itemList
            vietnameseQuestion = viewModel.getList(true)
            bindingQuestion(itemList[0])
        }

        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale.ENGLISH)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(requireContext(), "Lỗi TTS!", Toast.LENGTH_LONG).show()
                }
                tts.setSpeechRate(1.0f)
            }
        }

        setSpeaker()
        setLayout()
        binding.textViewQuestion.setText(topicTitle)


        // set Next Button
        binding.buttonNext.setOnClickListener {
            if (!itemList[current].isChoice) {
                return@setOnClickListener
            }
            if (current < itemList.size - 1) {
                current++
                bindingQuestion(itemList[current])
            }
        }
        // set Previous Button
        binding.buttonBack.setOnClickListener {
            if (current > 0) {
                current--
                bindingQuestion(itemList[current])
            }
        }

        //default
        setButtonModeOn(binding.buttonEnglishMode)
        setButtonModeOff(binding.buttonVietnameseMode)
        binding.buttonNext.isEnabled = false
        binding.buttonNext.setBackgroundResource(R.drawable.ic_backleft_gray)

        binding.buttonEnglishMode.setOnClickListener(View.OnClickListener {
            setButtonModeOn(binding.buttonEnglishMode)
            setButtonModeOff(binding.buttonVietnameseMode)
            itemList = itemList.subList(0, current)

            val existingIds = itemList.map { it.id }.toSet()
            val newQuestions = englishQuestion.filter { it.id !in existingIds }
            itemList = itemList + newQuestions.shuffled()
            bindingQuestion(itemList[current])
            isEnglish = true

        })
        binding.buttonVietnameseMode.setOnClickListener(View.OnClickListener {
            setButtonModeOn(binding.buttonVietnameseMode)
            setButtonModeOff(binding.buttonEnglishMode)
            itemList = itemList.subList(0, current)

            val existingIds = itemList.map { it.id }.toSet()
            val newQuestions = vietnameseQuestion.filter { it.id !in existingIds }
            itemList = itemList + newQuestions.shuffled()
            bindingQuestion(itemList[current])
            isEnglish = false
        })

        return binding.root
    }

    private fun setButtonModeOn(button: Button) {
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        button.setBackgroundResource(R.drawable.style_background_btn_on)
    }

    private fun setButtonModeOff(button: Button) {
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_400))
        button.setBackgroundResource(R.drawable.style_background_btn_off)
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }


    fun setSpeaker() {

        binding.buttonSpeaker.setOnClickListener(View.OnClickListener {
            if (isEnglish){
                tts.setLanguage(Locale.US)
            }else{
                tts.setLanguage(Locale("vi"))
            }
            tts.speak(binding.textViewWord.text, TextToSpeech.QUEUE_ADD, null, null)
        })

        val listener = View.OnClickListener { view ->
            tts.language = if (isEnglish) Locale("vi") else Locale.US
            val text = when(view.id) {
                R.id.buttonSpeakerA -> binding.radioOptionA.text
                R.id.buttonSpeakerB -> binding.radioOptionB.text
                R.id.buttonSpeakerC -> binding.radioOptionC.text
                R.id.buttonSpeakerD -> binding.radioOptionD.text
                else -> ""
            }
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
        }

        binding.buttonSpeakerA.setOnClickListener(listener)
        binding.buttonSpeakerB.setOnClickListener(listener)
        binding.buttonSpeakerC.setOnClickListener(listener)
        binding.buttonSpeakerD.setOnClickListener(listener)
    }


    fun bindingQuestion(question: Question) {
        binding.question = question
        binding.textViewQuestionCount.text = "${current + 1}/${itemList.size}"
        listLayout.forEach { layout ->
            setColorDeFault(layout)
        }
        if (question.isChoice) {
            if (question.isCorrect) {
                setColorTrue(listLayout[question.choice])
            } else {
                setColorFalse(listLayout[question.choice])
                val trueOption = question.options.firstOrNull {
                    it.isTrue
                }
                setColorTrue(listLayout[question.options.indexOf(trueOption)])
            }
            disableUserInput()
        } else {
            enableUserInput()
        }
        if (current == 0){
            binding.buttonBack.isEnabled = false
            binding.buttonBack.setBackgroundResource(R.drawable.ic_backleft_gray)
        }else{
            binding.buttonBack.isEnabled = true
            binding.buttonBack.setBackgroundResource(R.drawable.ic_backleft_blue)
        }
        if (audioMode){
            if (isEnglish){
                tts.setLanguage(Locale.US)
            }else{
                tts.setLanguage(Locale("vi"))
            }
            tts.speak(question.questionText, TextToSpeech.QUEUE_ADD, null, null)
        }
    }

    fun setLayout() {
        listLayout = listOf<LinearLayout>(
            binding.layoutOptionA,
            binding.layoutOptionB,
            binding.layoutOptionC,
            binding.layoutOptionD
        )

        listRadioButton = listOf<RadioButton>(
            binding.radioOptionA,
            binding.radioOptionB,
            binding.radioOptionC,
            binding.radioOptionD
        )

        listRadioButton.forEach { radioButton ->
            radioButton.setOnClickListener {
                selectAnswer(radioButton)
            }
        }
    }

    private fun selectAnswer(radioButton: RadioButton) {
        disableUserInput()
        setColorChose(listLayout[listRadioButton.indexOf(radioButton)])
        binding.question?.isChoice = true
        binding.question?.choice = listRadioButton.indexOf(radioButton)
        val selectedOption =
            binding.question?.options?.find { it.optionText == radioButton.text.toString() }
        if (selectedOption != null) {
            if (selectedOption.isTrue) {
                binding.question?.isCorrect = true
                setColorTrue(listLayout[listRadioButton.indexOf(radioButton)])
            } else {
                binding.question?.isCorrect = false
                setColorFalse(listLayout[listRadioButton.indexOf(radioButton)])
                val trueOption = binding.question?.options?.find { it.isTrue }
                val trueRadioButton = listRadioButton.firstOrNull { radioBtn ->
                    radioBtn.text.toString() == trueOption?.optionText
                }
                trueRadioButton?.let { trueRadioBtn ->
                    setColorTrue(listLayout[listRadioButton.indexOf(trueRadioBtn)])
                }
            }
        }
        Log.d(
            TAG,
            "onCreateViewHolder: ${binding.question?.isChoice} ${binding.question?.isCorrect}"
        )

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (current < itemList.size - 1) {
                current++
                bindingQuestion(itemList[current])
            }
        }, DELAY_MILLIS.toLong())

        doneQuestion++
        Log.d(TAG, "Done Question: $doneQuestion")
        if (doneQuestion == itemList.size) {
            navigateToTranscriptFragment()
        }
    }

    private fun setColorChose(layout: LinearLayout) {
        layout.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.style_custom_radio_answer_chose)
    }


    private fun setColorDeFault(layout: LinearLayout) {
        layout.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.style_custom_radio_answer)
    }

    private fun setColorTrue(layout: LinearLayout) {
        layout.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.style_custom_radio_answer_true)
    }

    private fun setColorFalse(layout: LinearLayout) {
        layout.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.style_custom_radio_answer_false)
    }

    private fun navigateToTranscriptFragment() {
        val bundle = Bundle()
        bundle.putParcelableArrayList("MultipleResultList", ArrayList(itemList))
        bundle.putLong("startTime", startTime)
        bundle.putString("topicID", topicID)
        bundle.putString("topicTitle", topicTitle)

        val fragment = MultipleChoiceTranscriptFragment()
        fragment.arguments = bundle

        val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameMultipleChoice, fragment)

        transaction.commit()

    }

    private fun disableUserInput() {
        binding.buttonNext.isEnabled = true
        binding.buttonNext.setBackgroundResource(R.drawable.ic_backleft_blue)
        listRadioButton.forEach { it.isEnabled = false }
        listRadioButton.forEach { it.setTextColor(Color.BLACK) }
    }

    private fun enableUserInput() {
        binding.buttonNext.isEnabled = false
        binding.buttonNext.setBackgroundResource(R.drawable.ic_backleft_gray)
        listRadioButton.forEach { it.isEnabled = true }
    }

    fun shuffleList() {
        val existingQuestions = itemList.subList(0, current)
        val newQuestions = itemList.subList(current, itemList.size)
        itemList = existingQuestions + newQuestions.shuffled()
        bindingQuestion(itemList[current])
    }
}