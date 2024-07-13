package com.iiex.languageassistant.ui.fragments

import android.content.ContentValues.TAG
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.FragmentMultipleChoiceTranscriptBinding
import com.iiex.languageassistant.databinding.ItemTableRowMultiChoiceBinding
import com.iiex.languageassistant.databinding.ItemTableRowWordTypeBinding
import com.iiex.languageassistant.viewmodels.MultipleChoiceFragViewModel
import com.iiex.languageassistant.viewmodels.Question
import com.iiex.languageassistant.viewmodels.WordTypeViewModel
import java.util.*


class MultipleChoiceTranscriptFragment : Fragment() {

    private lateinit var binding: FragmentMultipleChoiceTranscriptBinding
    private lateinit var viewModel: MultipleChoiceFragViewModel
    private lateinit var rowBinding: ItemTableRowMultiChoiceBinding
    private lateinit var rowBindingW: ItemTableRowWordTypeBinding
    private lateinit var tts: TextToSpeech
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_multiple_choice_transcript, container, false
        )
        viewModel = ViewModelProvider(this)[MultipleChoiceFragViewModel::class.java]
        binding.lifecycleOwner = this

        //TTS
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val res = tts.setLanguage(Locale.US)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(requireContext(), "Lỗi TTS!", Toast.LENGTH_LONG).show()
                }
                tts.setSpeechRate(1.0f)
            }
        }

        val tableLayout = binding.tableLayout

        // get data list from MultipleChoice
        val dataMultipleChoice = arguments?.getParcelableArrayList<Question>("MultipleResultList")
        val dataWordType =
            arguments?.getParcelableArrayList<WordTypeViewModel.WordType>("WordTypeResultList")

        val startTime = arguments?.getLong("startTime")
        val topicID = arguments?.getString("topicID")
        val topicTitle = arguments?.getString("topicTitle")

        Log.d(TAG, "data length : " + dataMultipleChoice?.size);

        binding.textViewTitle.setText(topicTitle)
        if (dataMultipleChoice != null) {
            val correctCount = dataMultipleChoice.count { it.isCorrect }
            val score = (correctCount.toDouble() / dataMultipleChoice.size) * 100
            binding.textViewScore.setText(String.format("%d", score.toInt()))
            if (topicID != null) {
                bindingTableQuestions(dataMultipleChoice, topicID)
                FirebaseAuth.getInstance().uid?.let {
                    if (startTime != null) {
                        viewModel.addLeaderBoard(topicID, it, score.toInt(), startTime)
                    }
                    viewModel.updateWordStatusQuestion(it, topicID, dataMultipleChoice)
                }
            }

        }

        if (dataWordType != null) {
            val correctCount = dataWordType.count { it.isCorrect }
            val score = (correctCount.toDouble() / dataWordType.size) * 100
            binding.textViewScore.setText(String.format("%d", score.toInt()))
            if (topicID != null) {
                bindingTableWordTypes(dataWordType, topicID)
                FirebaseAuth.getInstance().uid?.let {
                    if (startTime != null) {
                        viewModel.addLeaderBoard(topicID, it, score.toInt(), startTime)
                    }
                    viewModel.updateWordStatusWordType(it, topicID, dataWordType)
                }
            }
        }

        binding.buttonRedo.setOnClickListener(View.OnClickListener {
            if (topicID != null && topicTitle != null) {
                if (dataMultipleChoice!= null){
                    val fragment = MultipleChoiceFragment(topicID, topicTitle)
                    val transaction =
                        (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.frameMultipleChoice, fragment)
                    transaction.commit()
                }
                else{
                    val fragment = WordTypeFragment(topicID, topicTitle)
                    val transaction =
                        (context as AppCompatActivity).supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.frameWordType, fragment)
                    transaction.commit()
                }
            }
        })

        return binding.root
    }
    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    fun bindingTableQuestions(data: List<Question>, topicID: String) {
        var i = 0
        for (rowData in data) {
            // Inflate the layout for each row
            rowBinding = DataBindingUtil.inflate(
                LayoutInflater.from(requireContext()),
                R.layout.item_table_row_multi_choice,
                null,
                false
            )
            rowBinding.rowData = rowData

            if (!rowData.isCorrect) {
                context?.resources?.let {
                    val redColorID = it.getColor(R.color.red)
                    rowBinding.textViewTerm.setTextColor(redColorID)
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.weight = 0.5f
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    rowBinding.textViewInCorrect.layoutParams = layoutParams
                    rowBinding.textViewInCorrect.setText(rowData.options[rowData.choice].optionText)

                }
            }else{
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.weight = 0.0f
                layoutParams.height = 0
                rowBinding.textViewInCorrect.layoutParams = layoutParams
            }
            if (i % 2 != 0) {
                val backgroundColor = ColorDrawable(resources.getColor(R.color.black_200_2))
                rowBinding.root.background = backgroundColor
            } else {
                val backgroundColor = ColorDrawable(resources.getColor(R.color.blue_400_10))
                rowBinding.root.background = backgroundColor
            }
            rowBinding.buttonSpeaker.setOnClickListener(View.OnClickListener {
                tts.speak(rowData.tts, TextToSpeech.QUEUE_ADD, null, null)
            })
            val finalRowBinding = rowBinding
            finalRowBinding.buttonStar.setOnClickListener(View.OnClickListener {
                FirebaseAuth.getInstance().uid?.let { userID ->
                    rowData.id?.let { wordID ->
                        viewModel.mark(userID, topicID, wordID, !rowData.isMarked)
                        Toast.makeText(
                            requireContext(),
                            "marked: " + rowData.questionText,
                            Toast.LENGTH_LONG
                        ).show()
                        rowData.isMarked = !rowData.isMarked
                        if (rowData.isMarked) {
                            finalRowBinding.buttonStar.setImageResource(R.drawable.ic_star_marked)
                        } else {
                            finalRowBinding.buttonStar.setImageResource(R.drawable.ic_star)
                        }
                    }
                }
            })
            if (rowData.isMarked) {
                rowBinding.buttonStar.setImageResource(R.drawable.ic_star_marked)
            } else {
                rowBinding.buttonStar.setImageResource(R.drawable.ic_star)
            }
            i += 1
            // Add the root view of the binding to the tableLayout
            binding.tableLayout.addView(rowBinding.root)
        }
    }

    fun bindingTableWordTypes(data: List<WordTypeViewModel.WordType>, topicID: String) {
        var i = 0
        for (rowData in data) {
            rowBindingW = DataBindingUtil.inflate(
                LayoutInflater.from(requireContext()),
                R.layout.item_table_row_word_type,
                null,
                false
            )
            rowBindingW.rowData = rowData

            if (!rowData.isCorrect) {
                context?.resources?.let {
                    val redColorID = it.getColor(R.color.red)
                    rowBindingW.textViewTerm.setTextColor(redColorID)
                    val layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    layoutParams.weight = 0.5f
                    layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    rowBindingW.textViewInCorrect.layoutParams = layoutParams
                    rowBindingW.textViewInCorrect.setText(rowData.choice)

                }
            }else{
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.weight = 0.0f
                layoutParams.height = 0
                rowBindingW.textViewInCorrect.layoutParams = layoutParams
            }
            if (i % 2 != 0) {
                val backgroundColor = ColorDrawable(resources.getColor(R.color.black_200_2))
                rowBindingW.root.background = backgroundColor
            } else {
                val backgroundColor = ColorDrawable(resources.getColor(R.color.blue_400_10))
                rowBindingW.root.background = backgroundColor
            }
            rowBindingW.buttonSpeaker.setOnClickListener(View.OnClickListener {
                tts.speak(rowData.english, TextToSpeech.QUEUE_ADD, null, null)
            })
            val finalRowBinding = rowBindingW
            finalRowBinding.buttonStar.setOnClickListener(View.OnClickListener {
                FirebaseAuth.getInstance().uid?.let { userID ->
                    rowData.id?.let { wordID ->
                        viewModel.mark(userID, topicID, wordID, !rowData.isMarked)
                        Toast.makeText(
                            requireContext(),
                            "marked: " + rowData.english,
                            Toast.LENGTH_LONG
                        ).show()
                        rowData.isMarked = !rowData.isMarked
                        if (rowData.isMarked) {
                            finalRowBinding.buttonStar.setImageResource(R.drawable.ic_star_marked)
                        } else {
                            finalRowBinding.buttonStar.setImageResource(R.drawable.ic_star)
                        }
                    }
                }
            })
            if (rowData.isMarked) {
                rowBindingW.buttonStar.setImageResource(R.drawable.ic_star_marked)
            } else {
                rowBindingW.buttonStar.setImageResource(R.drawable.ic_star)
            }
            i += 1
            // Add the root view of the binding to the tableLayout
            binding.tableLayout.addView(rowBindingW.root)
        }
    }

}