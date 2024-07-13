package com.iiex.languageassistant.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.FragmentFlashCardTranscriptBinding

class FlashCardTranscriptFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentFlashCardTranscriptBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_flash_card_transcript, container, false
        )
        binding.textViewScore.setOnClickListener {
            activity?.finish()
        }
        return binding.root

    }
}