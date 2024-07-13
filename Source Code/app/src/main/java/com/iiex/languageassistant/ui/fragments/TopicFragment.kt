package com.iiex.languageassistant.ui.fragments

import android.content.ContentValues
import android.content.ContentValues.TAG
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.TopicAdapter
import com.iiex.languageassistant.databinding.FragmentTopicBinding
import com.iiex.languageassistant.databinding.ItemTopicBinding
import com.iiex.languageassistant.databinding.ItemTopicChildBinding
import com.iiex.languageassistant.viewmodels.TopicViewModel


class TopicFragment : Fragment() {

//    private val topicAdapterList by lazy { TopicAdapter(this) }
    private lateinit var viewModel: TopicViewModel

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = DataBindingUtil.inflate<FragmentTopicBinding>(
            inflater, R.layout.fragment_topic, container, false
        )
        val itemBinding = DataBindingUtil.inflate<ItemTopicChildBinding>(inflater,R.layout.item_topic_child,container,false)

        viewModel = ViewModelProvider(this).get(TopicViewModel::class.java)
        binding.viewModel = viewModel

        // enable seekbar
        itemBinding.seekBarWords.isEnabled = false
        itemBinding.seekbarFrame.isClickable = false
        itemBinding.seekbarFrame.isEnabled = false

        val topicAdapterList = TopicAdapter(requireContext())


        // set data for recylceView
        viewModel.dataList.observe(viewLifecycleOwner,Observer{ dataList ->
            if (dataList != null) {
                topicAdapterList.updateList(dataList)
            }
        })

        // fill recycleView
        binding.recycleViewTopic.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
            adapter = topicAdapterList
        }

        binding.recycleViewTopic.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                if (visibleItemCount + firstVisibleItemPosition >= totalItemCount && firstVisibleItemPosition >= 0) {
                    viewModel.loadMoreData()
                }
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
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
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

    fun filter(query: String?) {
        viewModel.filter(query)
    }

}
