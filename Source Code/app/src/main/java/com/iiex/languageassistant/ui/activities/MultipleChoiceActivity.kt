package com.iiex.languageassistant.ui.activities

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.view.*
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.TabLayoutPagerAdapter
import com.iiex.languageassistant.databinding.ActivityMultipleChoiceBinding
import com.iiex.languageassistant.ui.fragments.FolderFragment
import com.iiex.languageassistant.ui.fragments.MultipleChoiceFragment
import com.iiex.languageassistant.ui.fragments.TopicFragment
import com.iiex.languageassistant.ui.fragments.WordTypeFragment
import com.iiex.languageassistant.viewmodels.MultipleChoiceFragViewModel
import com.iiex.languageassistant.viewmodels.MultipleChoiceViewModel

class MultipleChoiceActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMultipleChoiceBinding
    private lateinit var viewModel: MultipleChoiceViewModel
    private var topicID: String? = null
    private var topicTitle: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_multiple_choice)
        viewModel = ViewModelProvider(this)[MultipleChoiceViewModel::class.java]
        binding.viewModel = viewModel


        topicID = intent.extras?.getString("topicID")
        topicTitle = intent.extras?.getString("topicTitle")

        if (topicID == null || topicTitle == null) {
            // Handle the case where no topic ID was passed in
            Toast.makeText(this, "No topic ID provided", Toast.LENGTH_LONG).show()
            finish() // Optionally close the activity if the topic ID is crucial
        }else{
            // set toolbar
            val toolbar: androidx.appcompat.widget.Toolbar = binding.toolbar
            setSupportActionBar(toolbar)
            val customTypeface = ResourcesCompat.getFont(this, R.font.roboto_bold)  // Replace with your font resource ID
            val spannableString = SpannableString("Multiple-choice")
            spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = spannableString
            val upArrow: Drawable? = ContextCompat.getDrawable(this, R.drawable.ic_back_toolbar)
            supportActionBar?.setHomeAsUpIndicator(upArrow)
            binding.toolbar.setNavigationOnClickListener{
                this.onBackPressed()
            }
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(binding.frameMultipleChoice.id, MultipleChoiceFragment(topicID!!, topicTitle!!),MultipleChoiceFragment::class.java.simpleName)
                    .commit()
            }
        }
        binding.menu.setOnClickListener {
            showMenuDialog()
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
        val fragment = supportFragmentManager.findFragmentByTag(MultipleChoiceFragment::class.java.simpleName)
        if (fragment is MultipleChoiceFragment) {
            switchAudiocMode.isChecked = fragment.audioMode
        }

        textViewRandom.setOnClickListener {
            if (fragment is MultipleChoiceFragment) {
                fragment.shuffleList()
            }
        }

        switchAudiocMode.setOnCheckedChangeListener { _, isChecked ->
            if (fragment is MultipleChoiceFragment) {
                fragment.audioMode = isChecked
            }
        }

        dialog.show()
    }

}