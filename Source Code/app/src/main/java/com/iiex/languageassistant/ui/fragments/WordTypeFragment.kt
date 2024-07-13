package com.iiex.languageassistant.ui.fragments


import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.databinding.FragmentWordTypeBinding
import com.iiex.languageassistant.databinding.ItemWordTypeBinding
import com.iiex.languageassistant.viewmodels.Question
import com.iiex.languageassistant.viewmodels.WordTypeViewModel
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.collections.ArrayList


class WordTypeFragment(val topicID: String, val topicTitle: String) : Fragment() {

    var audioMode = true
    private lateinit var viewModel: WordTypeViewModel
    private val startTime: Long = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+7"))
    private var itemList: List<WordTypeViewModel.WordType> = emptyList()
    private val DELAY_MILLIS = 1000
    private var doneQuestion: Int = 0;
    private var current: Int = 0
    private var isEnglish : Boolean = true
    lateinit var binding: FragmentWordTypeBinding
    private lateinit var tts: TextToSpeech

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate<FragmentWordTypeBinding>(
            inflater, R.layout.fragment_word_type, container, false
        )

        viewModel = ViewModelProvider(this)[WordTypeViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        FirebaseAuth.getInstance().uid?.let { viewModel.setTopic(it, topicID) }
        viewModel.word.observe(this) { words ->
            itemList = viewModel.getList()
            bindingQuestion(itemList[0])
        }

        binding.textViewQuestion.setText(topicTitle)
        setButtonModeOn(binding.buttonEnglishMode)
        // set button MODE
        binding.buttonEnglishMode.setOnClickListener(View.OnClickListener {
            setButtonModeOn(binding.buttonEnglishMode)
            setButtonModeOff(binding.buttonVietnameseMode)
            val existingQuestions = itemList.subList(0, current)
            val newQuestions = itemList.subList(current, itemList.size)
            itemList = existingQuestions + newQuestions.shuffled()
            isEnglish = true
            bindingQuestion(itemList[current])
        })
        binding.buttonVietnameseMode.setOnClickListener(View.OnClickListener {
            setButtonModeOn(binding.buttonVietnameseMode)
            setButtonModeOff(binding.buttonEnglishMode)

            val existingQuestions = itemList.subList(0, current)
            val newQuestions = itemList.subList(current, itemList.size)
            itemList = existingQuestions + newQuestions.shuffled()
            isEnglish = false
            bindingQuestion(itemList[current])
        })

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

        //set tts
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale.ENGLISH)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(requireContext(), "Lỗi TTS!", Toast.LENGTH_LONG).show()
                }
                tts.setSpeechRate(1.0f)
            }
        }
        binding.buttonSpeaker.setOnClickListener(View.OnClickListener {
            tts.speak(itemList[current].english, TextToSpeech.QUEUE_ADD, null, null)
        })

        binding.buttonCheck.setOnClickListener(View.OnClickListener {
            checkWord()
        })

        //default
        binding.buttonNext.isEnabled = false
        binding.buttonNext.setBackgroundResource(R.drawable.ic_backleft_gray)
        return binding.root
    }

    fun bindingQuestion(question: WordTypeViewModel.WordType) {
        binding.word = question
        if (!isEnglish){
            binding.textViewWord.setText(question.vietnamese)
            binding.editTextWordCorrect.setText(question.english)
        }else{
            binding.textViewWord.setText(question.english)
            binding.editTextWordCorrect.setText(question.vietnamese)
        }
        binding.textViewQuestionCount.text = "${current + 1}/${itemList.size}"

        if (question.isChoice) {
            if (question.isCorrect) {
                bindingCorrect()
            } else {
                bindingInCorrect()
            }
            disableUserInput()
        } else {
            enableUserInput()
            bindingDefault()
        }

        if (current == 0){
            binding.buttonBack.isEnabled = false
            binding.buttonBack.setBackgroundResource(R.drawable.ic_backleft_gray)
        }else{
            binding.buttonBack.isEnabled = true
            binding.buttonBack.setBackgroundResource(R.drawable.ic_backleft_blue)
        }
        if (audioMode){
            tts.speak(question.english, TextToSpeech.QUEUE_ADD, null, null)
        }
    }
    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    private fun setButtonModeOn(button: Button) {
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        button.setBackgroundResource(R.drawable.style_background_btn_on)
    }

    private fun setButtonModeOff(button: Button) {
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_400))
        button.setBackgroundResource(R.drawable.style_background_btn_off)
    }

    private fun checkWord() {
        binding.editTextWord.inputType = InputType.TYPE_NULL
        binding.word?.isChoice = true
        binding.word?.choice = binding.editTextWord.text.toString()
        val userInput = binding.editTextWord.text.toString().trim()
        val correctWord = binding.editTextWordCorrect.text.toString().trim() ?: ""
        if (userInput.equals(correctWord, ignoreCase = true)) {
            bindingCorrect()
        } else {
            binding.word?.isCorrect = false
            bindingInCorrect()
        }
        enableNextButton(binding)

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (current < itemList.size - 1) {
                current++
                bindingQuestion(itemList[current])
            }
        }, DELAY_MILLIS.toLong())

        doneQuestion++
        Log.d(ContentValues.TAG, "Done Question: $doneQuestion")
        if (doneQuestion == itemList.size) {
            navigateToTranscriptFragment()
        }
    }

    private fun bindingCorrect() {
        binding.editTextWordCorrect.visibility = View.GONE
        binding.editTextWordCorrect.inputType = InputType.TYPE_NULL
        setEditTextStyle(
            requireContext(),
            binding.editTextWord,
            R.drawable.style_edit_text_word_true,
            R.color.green
        )
        setButtonStyle(requireContext(), binding.buttonCheck, R.color.green)
    }

    private fun bindingDefault() {
        binding.editTextWordCorrect.visibility = View.GONE
        binding.editTextWordCorrect.inputType = InputType.TYPE_NULL
        setEditTextStyle(
            requireContext(),
            binding.editTextWord,
            R.drawable.style_edit_text_word_input,
            R.color.blue_400
        )
        setButtonStyle(requireContext(), binding.buttonCheck, R.color.blue_400)
    }
    private fun bindingInCorrect() {
        binding.editTextWordCorrect.visibility = View.VISIBLE
        binding.editTextWordCorrect.inputType = InputType.TYPE_NULL
        setEditTextStyle(
            requireContext(),
            binding.editTextWord,
            R.drawable.style_edit_text_word_false,
            R.color.red
        )
        setButtonStyle(requireContext(), binding.buttonCheck, R.color.red)
    }

    // Hàm này cài đặt màu sắc cho EditText
    private fun setEditTextStyle(
        context: Context,
        editText: EditText,
        backgroundResource: Int,
        textColorResource: Int
    ) {
        editText.setBackgroundResource(backgroundResource)
        editText.setTextColor(ContextCompat.getColor(context, textColorResource))
    }

    // Hàm này cài đặt màu sắc cho Button
    private fun setButtonStyle(context: Context, button: Button, backgroundColorResource: Int) {
        button.setBackgroundColor(ContextCompat.getColor(context, backgroundColorResource))
    }

    // Hàm này kích hoạt nút "Next"
    private fun enableNextButton(binding: FragmentWordTypeBinding) {
        binding?.buttonNext?.isEnabled = true
        binding?.buttonNext?.setBackgroundResource(R.drawable.ic_backleft_blue)
    }

    private fun disableUserInput() {
        binding.buttonNext.isEnabled = true
        binding.buttonNext.setBackgroundResource(R.drawable.ic_backleft_blue)
        binding.buttonCheck.isEnabled = false
        binding.editTextWord.isEnabled = false
    }

    private fun enableUserInput() {
        binding.buttonNext.isEnabled = false
        binding.buttonNext.isEnabled = false
        binding.buttonNext.setBackgroundResource(R.drawable.ic_backleft_gray)
        binding.editTextWord.isEnabled = true
        binding.buttonCheck.isEnabled = true
        binding.editTextWord.inputType = InputType.TYPE_CLASS_TEXT
        binding.editTextWord.setText("")
    }

    private fun navigateToTranscriptFragment() {
        val bundle = Bundle()
        bundle.putParcelableArrayList("WordTypeResultList", ArrayList(itemList))
        bundle.putLong("startTime", startTime)
        bundle.putString("topicID", topicID)
        bundle.putString("topicTitle", topicTitle)

        val fragment = MultipleChoiceTranscriptFragment()
        fragment.arguments = bundle

        val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameWordType, fragment)

        transaction.commit()

    }

    fun shuffleList() {
        val existingQuestions = itemList.subList(0, current)
        val newQuestions = itemList.subList(current, itemList.size)
        itemList = existingQuestions + newQuestions.shuffled()
        bindingQuestion(itemList[current])
    }


}