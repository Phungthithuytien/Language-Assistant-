package com.iiex.languageassistant.ui.fragments

import android.content.ContentValues.TAG
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.TypefaceSpan
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.PostAdapter
import com.iiex.languageassistant.adapter.TopTopicsAdapter
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.FragmentCommunityBinding
import com.iiex.languageassistant.viewmodels.CommunityViewModel

class CommunityFragment : Fragment() {

    private lateinit var binding: FragmentCommunityBinding
    private lateinit var viewModel : CommunityViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_community, container, false
        )
        // set tool bar
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        val customTypeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_bold)  // Replace with your font resource ID
        val spannableString = SpannableString(requireActivity().getString(R.string.community))
        spannableString.setSpan(customTypeface?.let { TypefaceSpan(it) }, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = spannableString
        val upArrow: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.ic_back_toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setHomeAsUpIndicator(upArrow)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
        viewModel =  ViewModelProvider(this).get(CommunityViewModel::class.java)
        viewModel.init()
        val topTopicsAdapter = TopTopicsAdapter(requireContext())

        viewModel.topTopic.observe(this){
                datalist ->
            if(datalist!=null){
                topTopicsAdapter.updateList(datalist as List<DataItem.Topic>)
            }
        }
        binding.recyclerViewRanking.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = topTopicsAdapter
        }

        val postAdapter = PostAdapter(requireContext(), ArrayList())
        viewModel.topics.observe(viewLifecycleOwner){
            if (!it.isEmpty()){
                postAdapter.updateData(it)
            }
        }

        binding.recyclerViewComunity.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = postAdapter
        }

        binding.nestedScrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            if (scrollY == v.getChildAt(0).measuredHeight - v.measuredHeight) {
                viewModel.loadMoreData()
            }
        })

        val progressDialog = createProgressDialog()
        viewModel.loadingIndicator.observe(this){
            if (it){
                progressDialog.show()
            }else{
                progressDialog.dismiss()
            }
        }
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Check if the query is not null or empty
                if (!query.isNullOrEmpty()) {
                    val searchFragment = SearchFragment.newInstance(query)
                    val transaction = requireActivity().supportFragmentManager.beginTransaction()
                    transaction.replace(R.id.fragment_container_view_tag, searchFragment)
                    transaction.addToBackStack(null) // Allow back navigation
                    transaction.commit()
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

        binding.lifecycleOwner = this
        return binding.root
    }
    private fun createProgressDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_progress,null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return  dialog
    }

    override fun onResume() {
        viewModel.init()
        super.onResume()
    }
}