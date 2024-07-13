package com.iiex.languageassistant.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.PostAdapter
import com.iiex.languageassistant.databinding.FragmentSearchBinding
import com.iiex.languageassistant.viewmodels.SearchViewModel

class SearchFragment : Fragment() {

    private lateinit var viewModel: SearchViewModel
    private lateinit var binding: FragmentSearchBinding
    companion object {
        private const val ARG_QUERY = "query"

        fun newInstance(query: String): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle()
            args.putString(ARG_QUERY, query)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(this).get(SearchViewModel::class.java)
        binding = FragmentSearchBinding.inflate(inflater,container,false)

        val query = arguments?.getString(ARG_QUERY) ?: ""
        viewModel.initFilter(query)
        val postAdapter = PostAdapter(requireContext(), ArrayList())
        viewModel.topics.observe(this){
            postAdapter.updateData(it)
        }
        binding.searchView.setQuery(query,false)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.title = requireActivity().getString(R.string.searchVi)
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = postAdapter
        }


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
                    viewModel.initFilter(query)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
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

}