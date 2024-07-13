package com.iiex.languageassistant.ui.fragments

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.iiex.languageassistant.R
import com.iiex.languageassistant.adapter.FolderAdapter
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.FragmentFolderBinding
import com.iiex.languageassistant.viewmodels.FolderViewModel


class FolderFragment : Fragment() {

    private val folderAdapterList by lazy { FolderAdapter(requireContext()) }

    private lateinit var viewModel:FolderViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = DataBindingUtil.inflate<FragmentFolderBinding>(
            inflater, R.layout.fragment_folder, container, false
        )
        viewModel = ViewModelProvider(this)[FolderViewModel::class.java]
        viewModel.setFolder()
        viewModel.folders.observe(this){
            folderAdapterList.updateList(it)
        }
        binding.recycleViewFolder.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL,false)
            adapter = folderAdapterList
        }
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
        viewModel.setFolder()
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