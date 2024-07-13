package com.iiex.languageassistant.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.PostAdapter
import com.iiex.languageassistant.adapter.TopTopicsAdapter
import com.iiex.languageassistant.adapter.TopicAdapter
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.FragmentHomePageBinding
import com.iiex.languageassistant.databinding.ItemTopicChildBinding
import com.iiex.languageassistant.ui.activities.MainActivity
import com.iiex.languageassistant.viewmodels.HomeViewModel


class HomePageFragment : Fragment() {
    private lateinit var viewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        var binding  = FragmentHomePageBinding.inflate(inflater,container,false)
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
        }

        viewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        viewModel.init()
        //TODO: recentRecycleview
        val recentItemBinding = DataBindingUtil.inflate<ItemTopicChildBinding>(inflater,
            R.layout.item_topic_child,container,false)

        // enable seekbar
        recentItemBinding.seekBarWords.isEnabled = false
        recentItemBinding.seekbarFrame.isClickable = false
        recentItemBinding.seekbarFrame.isEnabled = false
        //adapter
        val topicAdapterList = TopicAdapter(requireContext())
        // set data for recylceView
        viewModel.recentList.observe(viewLifecycleOwner,Observer{ dataList ->
            if (dataList != null) {
                topicAdapterList.updateList(dataList)
            }
        })
        binding.recycleViewToDo.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = topicAdapterList
        }

        //TODO: recycleViewHintTopic
        val topTopicsAdapter = TopTopicsAdapter(requireContext())

        viewModel.topTopic.observe(this){
            datalist ->
            if(datalist!=null){
                topTopicsAdapter.updateList(datalist as List<DataItem.Topic>)
            }
        }
        binding.recycleViewHintTopic.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = topTopicsAdapter
        }

        val postAdapter = PostAdapter(requireContext(), ArrayList())
        viewModel.topics.observe(this){
            if (it.isNotEmpty()){
                postAdapter.updateData(it)
            }
        }

        binding.recycleViewCommunity.apply {
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

        binding.buttonPractice.setOnClickListener {
            val libFragment = LibraryFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container_view_tag, libFragment)
            transaction.addToBackStack(null) // Allow back navigation
            transaction.commit()
            (activity as? MainActivity)?.updateBottomNavigationMenu(LibraryFragment::class.java.simpleName)

        }
        binding.buttonLearnFolder.setOnClickListener {
            //set default tab is Folder
            val defaultTabIndex = 1
            val libFragment = LibraryFragment.newInstance(defaultTabIndex)

            // Start the transaction
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container_view_tag, libFragment)
            transaction.addToBackStack(null) // Allow back navigation
            transaction.commit()

            // Update the bottom navigation menu
            (activity as? MainActivity)?.updateBottomNavigationMenu(LibraryFragment::class.java.simpleName)
        }

        binding.buttonExplore.setOnClickListener {
            val communityFragment = CommunityFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container_view_tag, communityFragment)
            transaction.addToBackStack(null) // Allow back navigation
            transaction.commit()
            (activity as? MainActivity)?.updateBottomNavigationMenu(CommunityFragment::class.java.simpleName)

        }


        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Check if the query is not null or empty
                if (!query.isNullOrEmpty()) {
                    // Navigate to SearchFragment with the query
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


        return  binding.root
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