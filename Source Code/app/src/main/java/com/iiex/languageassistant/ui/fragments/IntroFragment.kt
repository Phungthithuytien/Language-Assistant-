package com.iiex.languageassistant.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.iiex.languageassistant.R
import com.iiex.languageassistant.databinding.FragmentIntroBinding

class IntroFragment : Fragment(R.layout.fragment_intro) {
    private  var binding: FragmentIntroBinding?= null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var fragmentBinding  = FragmentIntroBinding.inflate(inflater,container,false)
        binding = fragmentBinding
        return  fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.introFragmemt = this
    }

    fun login(){
        findNavController().navigate(R.id.action_introFragment_to_loginFragment)
    }

    fun register(){
        findNavController().navigate(R.id.action_introFragment_to_registerFragment)
    }

}