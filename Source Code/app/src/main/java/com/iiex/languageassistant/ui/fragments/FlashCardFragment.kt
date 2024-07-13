package com.iiex.languageassistant.ui.fragments

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.data.model.ref.WordStatus
import com.iiex.languageassistant.databinding.FragmentFlashCardBinding
import com.iiex.languageassistant.ui.activities.FlashCardActivity
import com.iiex.languageassistant.viewmodels.FlashCardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList


class FlashCardFragment(val topicID: String, val topicTitle: String, val isMarked: String) : Fragment(), TextToSpeech.OnInitListener {
    private lateinit var viewModel: FlashCardViewModel
    // set cardview default position
    private var initialX: Float = 0f
    private var initialY: Float = 0f
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    //tts
    private var tts: TextToSpeech? =null
    private lateinit var binding: FragmentFlashCardBinding
    private var flashCardList : List<Word> = ArrayList();
    private var indexCurrentFlashCard = 0;
    private var masterCount = 0;
    private var learndCount = 0;
    private var autoSwipeJob: Job? = null
    private var isAuto: Boolean = false
    private val currentUser = FirebaseAuth.getInstance().currentUser?.uid ?:""
    private var isEnglish : Boolean = true
    var audioMode : Boolean = true
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_flash_card, container, false
        )
        viewModel = ViewModelProvider(this)[FlashCardViewModel::class.java]

        viewModel.setTopic(currentUser, topicID,isMarked)

        viewModel.word.observe(this){
            flashCardList = it
            val currentFlash = flashCardList[indexCurrentFlashCard]
            if (isEnglish){
                binding.textViewWord.setText(currentFlash.vietnamese)
            }else{
                binding.textViewWord.setText(currentFlash.english)
            }
            if (currentFlash.isMarked) {
                binding.buttonStar.setImageResource(R.drawable.ic_star_marked)
            } else {
                binding.buttonStar.setImageResource(R.drawable.ic_star)
            }
            binding.cardViewAbove.tag = "faceDown"
            binding.textViewQuestionCount.text = "${indexCurrentFlashCard + 1}/${flashCardList.size}"
            binding.textViewMasterWord.text = "Đã thành thạo: ${masterCount}"
            binding.textViewNotMasterWord.text = "Chưa thành thạo: ${learndCount}"
        }
        binding.textViewQuestion.setText(topicTitle)
        moveCard(binding.cardViewAbove)
        binding.buttonAuto.setOnClickListener {
            if (isAuto){
                autoSwipeJob?.cancel()
                isAuto = false
                binding.buttonAuto.setBackgroundResource(R.drawable.ic_playing)
            }else{
                isAuto = true
                autoSwipeJob = GlobalScope.launch(Dispatchers.Main) {
                    for (index in indexCurrentFlashCard until flashCardList.size) {
                        delay(2000) // Adjust the delay as needed
                        flipCard(binding.cardViewAbove)
                        delay(2000) // Adjust the delay as needed
                        initialX = binding.cardViewAbove.x
                        initialY = binding.cardViewAbove.y
                        setCardOutScreen(binding.cardViewAbove, index)
                    }
                }
                binding.buttonAuto.setBackgroundResource(R.drawable.ic_play_stop)
            }
        }
        binding.buttonBack.setOnClickListener {
            val preFlashCard= flashCardList[--indexCurrentFlashCard]
            if (isEnglish){
                binding.textViewWord.setText(preFlashCard.vietnamese)
            }else{
                binding.textViewWord.setText(preFlashCard.english)
            }
            binding.cardViewAbove.tag = "faceDown"
            if (preFlashCard.statusByUser[currentUser] == WordStatus.MASTERED){
                masterCount--
            }else if(preFlashCard.statusByUser[currentUser] == WordStatus.LEARNED){
                learndCount--
            }
            preFlashCard.id?.let { it1 ->
                preFlashCard.statusByUser.toMutableMap()[currentUser] = WordStatus.LEARNED
            }

            binding.textViewQuestionCount.text = "${indexCurrentFlashCard + 1}/${flashCardList.size}"
            binding.textViewMasterWord.text = "Đã thành thạo: ${masterCount}"
            binding.textViewNotMasterWord.text = "Chưa thành thạo: ${learndCount}"
            if (indexCurrentFlashCard == 0){
                binding.buttonBack.isEnabled =false
                binding.buttonBack.setBackgroundResource(R.drawable.ic_backleft_gray)
            }
            if (preFlashCard.isMarked) {
                binding.buttonStar.setImageResource(R.drawable.ic_star_marked)
            } else {
                binding.buttonStar.setImageResource(R.drawable.ic_star)
            }
        }
        // set button tick
        binding.buttonTick.setOnClickListener {
            buttonTick()
        }
        // set cancel tick
        binding.buttonCancel.setOnClickListener {
            buttonCancel()
        }
        //set tts
        tts = TextToSpeech(requireContext(),this)
        binding.buttonSpeaker.setOnClickListener {
            speak()
        }

        //set Mode
        setButtonModeOn(binding.buttonEnglishMode)
        setButtonModeOff(binding.buttonVietnameseMode)
        binding.buttonEnglishMode.setOnClickListener(View.OnClickListener {
            setButtonModeOn(binding.buttonEnglishMode)
            setButtonModeOff(binding.buttonVietnameseMode)
            binding.textViewWord.text = flashCardList[indexCurrentFlashCard].vietnamese
            binding.cardViewAbove.tag = "faceDown"
            isEnglish = true

        })
        binding.buttonVietnameseMode.setOnClickListener(View.OnClickListener {
            setButtonModeOn(binding.buttonVietnameseMode)
            setButtonModeOff(binding.buttonEnglishMode)
            binding.textViewWord.text = flashCardList[indexCurrentFlashCard].english
            isEnglish = false
            binding.cardViewAbove.tag = "faceDown"
        })

        //setMark
        binding.buttonStar.setOnClickListener(View.OnClickListener {
            val currentFlash = flashCardList[indexCurrentFlashCard]
            viewModel.mark(currentUser, currentFlash.id, !currentFlash.isMarked)
            currentFlash.isMarked = !currentFlash.isMarked
        })
        return binding.root
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    private fun speak() {
        val flashCard = flashCardList[indexCurrentFlashCard]
        val text = binding.textViewWord.text
        if (text.equals(flashCard.english)){
            tts?.setLanguage(Locale.US)
        }else{
            tts?.setLanguage(Locale("vi"))
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }
    override fun onInit(p0: Int) {
       if (p0 == TextToSpeech.SUCCESS){
           val res = tts?.setLanguage(Locale.ENGLISH)
           if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
               Toast.makeText(requireContext(), "Lỗi TTS!", Toast.LENGTH_LONG).show()
           }
           tts?.setSpeechRate(1.0f)
       }
    }
    fun shuffleFlashCardList() {
        if (indexCurrentFlashCard < flashCardList.size - 1) {
            val sublistToShuffle = flashCardList.subList(indexCurrentFlashCard, flashCardList.size)
            val shuffledSublist = sublistToShuffle.shuffled()
            // Replace the shuffled sublist back into the original list
            flashCardList = flashCardList.toMutableList().apply {
                subList(indexCurrentFlashCard, size).clear()
                addAll(shuffledSublist)
            }

            val preFlashCard= flashCardList[indexCurrentFlashCard]
            if (isEnglish){
                binding.textViewWord.text = preFlashCard.vietnamese
            }else{
                binding.textViewWord.text = preFlashCard.english
            }
            binding.cardViewAbove.tag = "faceDown"
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    fun moveCard(cardView: CardView) {
        var isSwiping = false
        var isAnimating = false
        cardView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = view.x
                    initialY = view.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isSwiping = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val offsetX = event.rawX - initialTouchX
                    val offsetY = event.rawY - initialTouchY
                    if (kotlin.math.abs(offsetX) > 5 || kotlin.math.abs(offsetY) > 5) {
                        isSwiping = true
                    }
                    if (isSwiping) {
                        view.x = initialX + offsetX
                        view.y = initialY + offsetY
                        val threshold = - 700
                        if (event.rawX - threshold < initialTouchX) {
                            // Chạm sang trái
                            handleSwipeLeft()
                        } else if (event.rawX + threshold > initialTouchX) {
                            // Chạm sang phải
                            handleSwipeRight()
                        } else {
                            Log.d("SwipeTest", "No significant swipe")
                            handleSwipeDefault()
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val threshold = - 700
                    if (isSwiping) {
                        isAnimating = true
                        if (event.rawX - threshold < initialTouchX) {
                            // Chạm sang trái
                           setCardOutScreen(view,-1)
                        } else if (event.rawX + threshold > initialTouchX) {
                            // Chạm sang phải
                            setCardOutScreen(view,1)
                        } else {
                            Log.d("SwipeTest", "No significant swipe")
                        }
                            view.animate()
                                .x(initialX)
                                .y(initialY)
                                .setDuration(300)
                                .setListener(object : AnimatorListener {
                                    override fun onAnimationStart(animation: Animator) {
                                        // Animation start, if needed\
                                        view.isEnabled = false
                                        handleSwipeDefault()
                                    }
                                    override fun onAnimationEnd(animation: Animator) {
                                        // Animation end\
                                        view.isEnabled = true
                                    }
                                    override fun onAnimationCancel(animation: Animator) {
                                        // Animation cancel, if needed
                                    }
                                    override fun onAnimationRepeat(animation: Animator) {
                                        // Animation repeat, if needed
                                    }
                                })
                                .start()
                    } else {
                        flipCard(cardView)
                    }
                    true
                }
                else -> false
            }
        }
    }
    // set card văng ra
    private fun setCardOutScreen(view: View,turn : Int){
        val slideOutAnimation = TranslateAnimation(0f, view.width.toFloat()*turn, 0f, 0f)
        slideOutAnimation.duration = 200 // Adjust the animation duration as needed
        slideOutAnimation.setAnimationListener(object : Animation.AnimationListener {
            // 1 = rìght , 0 = left
            override fun onAnimationStart(animation: Animation?) {
                if (turn<0){
                    handleSwipeLeft()
                }else{
                    handleSwipeRight()
                }
            }
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                view.visibility = View.GONE
                view.animate()
                    .x(initialX).y(initialY)
                    .setDuration(0) // Adjust the duration as needed
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            view.visibility = View.VISIBLE
                            handleSwipeDefault()
                            if (turn<0){
                                learndCount++
                                val currentFlashCard = flashCardList[indexCurrentFlashCard]
                                currentFlashCard.statusByUser.toMutableMap()[currentUser] = WordStatus.LEARNED
                            }else{
                                masterCount++
                                val currentFlashCard = flashCardList[indexCurrentFlashCard]
                                currentFlashCard.id?.let {
                                    currentFlashCard.statusByUser.toMutableMap()[currentUser] = WordStatus.MASTERED
                                }
                            }
                            setNextFlashCard()
                        }
                    })
                    .start()
            }
        })
        view.startAnimation(slideOutAnimation)
    }
    // Handle Tick Button
    private fun buttonTick(){
        binding.cardViewAbove.animate()
            .setDuration(100) // Adjust the duration as needed
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    initialX = binding.cardViewAbove.x
                    initialY = binding.cardViewAbove.y

                    handleSwipeRight()
                }
                override fun onAnimationEnd(animation: Animator) {
                    setCardOutScreen(binding.cardViewAbove,1)
                }
            })
            .start()
    }
    // Handle Cancel Button
    private fun buttonCancel(){
        binding.cardViewAbove.animate()
            .setDuration(100) // Adjust the duration as needed
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    initialX = binding.cardViewAbove.x
                    initialY = binding.cardViewAbove.y
                    handleSwipeLeft()
                }
                override fun onAnimationEnd(animation: Animator) {
                    setCardOutScreen(binding.cardViewAbove,-1)
                }
            })
            .start()
    }
    private  fun setNextFlashCard(){
        if (indexCurrentFlashCard == flashCardList.size - 1 ){
            val bundle = Bundle()
            val fragment = FlashCardTranscriptFragment()
            fragment.arguments = bundle
            val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(R.id.frameFlashCard, fragment)
            transaction.commit()

            (activity as? FlashCardActivity)?.hideMennu()
        }else{
            val newFlashCard = flashCardList[++indexCurrentFlashCard]
            if (isEnglish){
                binding.textViewWord.setText(newFlashCard.vietnamese)
            }else{
                binding.textViewWord.setText(newFlashCard.english)
            }
            binding.cardViewAbove.tag = "faceDown"

            binding.textViewQuestionCount.text = "${indexCurrentFlashCard + 1}/${flashCardList.size}"
            binding.textViewMasterWord.text = "Đã thành thạo: ${masterCount}"
            binding.textViewNotMasterWord.text = "Chưa thành thạo: ${learndCount}"
            if (indexCurrentFlashCard == 0){
                binding.buttonBack.isEnabled = false
                binding.buttonBack.setBackgroundResource(R.drawable.ic_backleft_gray)
            }else{
                binding.buttonBack.isEnabled = true
                binding.buttonBack.setBackgroundResource(R.drawable.ic_backleft_blue)
            }
            if (audioMode){
                speak()
            }
            if (newFlashCard.isMarked) {
                binding.buttonStar.setImageResource(R.drawable.ic_star_marked)
            } else {
                binding.buttonStar.setImageResource(R.drawable.ic_star)
            }
        }

    }
    private fun flipCard(cardView: CardView) {
        val flipFront = ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 0f)
        val flipBack = ObjectAnimator.ofFloat(cardView, "scaleX", 0f, 1f)
        flipFront.interpolator = AccelerateInterpolator()
        flipBack.interpolator = DecelerateInterpolator()
        flipFront.duration = 200
        flipBack.duration = 200
        flipFront.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                changeCardContent(cardView)
                flipBack.start()
                if (audioMode){
                    speak()
                }
            }
        })
        flipFront.start()
    }

    // Function to change the content of the card
    private fun changeCardContent(cardView: CardView) {
        val flashCard = flashCardList[indexCurrentFlashCard]
        if (isEnglish) {
            binding.textViewWord.text = if (cardView.tag == "faceUp") {
                flashCard.vietnamese
            } else {
                flashCard.english
            }
            cardView.tag = if (cardView.tag == "faceUp") "faceDown" else "faceUp"
        } else {
            binding.textViewWord.text = if (cardView.tag == "faceUp") {
                flashCard.english
            } else {
                flashCard.vietnamese
            }
            cardView.tag = if (cardView.tag == "faceUp") "faceDown" else "faceUp"
        }
    }


    // Function to simulate a swipe gesture
    private fun setButtonModeOn(button: Button) {
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        button.setBackgroundResource(R.drawable.style_background_btn_on)
    }

    private fun setButtonModeOff(button: Button) {
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_400))
        button.setBackgroundResource(R.drawable.style_background_btn_off)
    }

    private fun handleSwipeLeft() {
        binding.layoutFlashCardBelow.setBackgroundResource(R.drawable.style_background_card_red);
    }

    private fun handleSwipeRight() {
        binding.layoutFlashCardBelow.setBackgroundResource(R.drawable.style_background_card_green);
    }
    private fun handleSwipeDefault() {
        binding.layoutFlashCardBelow.setBackgroundResource(R.drawable.style_background_btn_off);
    }



}