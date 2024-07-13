package com.iiex.languageassistant.ui.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.view.*
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.ActivityFlashCardBinding
import com.iiex.languageassistant.databinding.ActivityMultipleChoiceBinding
import com.iiex.languageassistant.ui.fragments.FlashCardFragment
import com.iiex.languageassistant.ui.fragments.MultipleChoiceFragment
import com.iiex.languageassistant.ui.fragments.WordTypeFragment
import com.iiex.languageassistant.viewmodels.FlashCardViewModel
import com.iiex.languageassistant.viewmodels.MultipleChoiceViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FlashCardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFlashCardBinding
    private lateinit var viewModel: FlashCardViewModel
    private var topicID: String? = null
    private var topicTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_flash_card)

        topicID = intent.extras?.getString("topicID")
        topicTitle = intent.extras?.getString("topicTitle")
        val isMarked = intent.extras?.getString("isMarked") ?: ""

        if (topicID == null || topicTitle == null) {
            finish()
        }else{
            // set toolbar
            val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
            setSupportActionBar(toolbar)
            val customTypeface = ResourcesCompat.getFont(this, R.font.roboto_bold)  // Replace with your font resource ID
            val spannableString = SpannableString("Flashcard")
            spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = spannableString
            val upArrow: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_back_toolbar)
            supportActionBar?.setHomeAsUpIndicator(upArrow)
            binding.toolbar.setNavigationOnClickListener{
                this.onBackPressed()
            }
            binding.menu.setOnClickListener {
                showMenuDialog()
            }

            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.frameFlashCard.id, FlashCardFragment(topicID!!, topicTitle!!,isMarked!!), FlashCardFragment::class.java.simpleName)
                    .commit()
            }
        }
    }
    fun hideMennu() {
        binding.menu.visibility = View.GONE
    }
    private fun showMenuDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setContentView(R.layout.dialog_learning_options)
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val wlp = dialog.window?.attributes
        if (wlp != null) {
            wlp.gravity = Gravity.BOTTOM
            dialog.window!!.attributes = wlp
        }

        val textViewRandom = dialog.findViewById<TextView>(R.id.textViewRandom)
        val switchAudiocMode = dialog.findViewById<Switch>(R.id.switchAudioMode)
        val fragment = supportFragmentManager.findFragmentByTag(FlashCardFragment::class.java.simpleName)
        if (fragment is FlashCardFragment) {
            switchAudiocMode.isChecked = fragment.audioMode
        }

        textViewRandom.setOnClickListener {
            if (fragment is FlashCardFragment) {
                fragment.shuffleFlashCardList()
            }
        }

        switchAudiocMode.setOnCheckedChangeListener { _, isChecked ->
            if (fragment is FlashCardFragment) {
                fragment.audioMode = isChecked
            }
        }

        dialog.show()
    }

}