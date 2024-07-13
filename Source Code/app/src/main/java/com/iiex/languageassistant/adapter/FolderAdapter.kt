package com.iiex.languageassistant.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.ItemFolderBinding
import com.iiex.languageassistant.databinding.ItemFolderChildBinding
import com.iiex.languageassistant.ui.activities.FolderDetailActivity
import com.iiex.languageassistant.viewmodels.FolderViewModel
import java.io.Serializable

class FolderAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_FOLDER = 0
        private const val VIEW_TYPE_HEADER = 1
    }
    private lateinit var viewModel: FolderViewModel
    private lateinit var topicID: String

    inner class FolderViewHolder(val folderBinding: ItemFolderChildBinding) : RecyclerView.ViewHolder(folderBinding.root){
        fun bind(item : DataItem.Folder){
            folderBinding.folder = item
            folderBinding.buttonContinue.setOnClickListener {
                val intent = Intent(context, FolderDetailActivity::class.java)
                intent.putExtra("folderID", item.id)
                context.startActivity(intent)
            }
            if (item.isAdded != null){
                if (item.isAdded!!){
                    folderBinding.buttonContinue.setText("Xóa")
                    folderBinding.buttonContinue.setTextColor(Color.RED)
                    folderBinding.buttonContinue.setOnClickListener {
                        item.id?.let { folderID -> viewModel.removeFormFolder(topicID, folderID){} }
                    }
                }else{
                    folderBinding.buttonContinue.setText("Thêm")
                    folderBinding.buttonContinue.setTextColor(context.resources.getColor(R.color.blue_400))
                    folderBinding.buttonContinue.setOnClickListener {
                        item.id?.let {
                                folderID -> viewModel.addToFolder(topicID, folderID){}
                        }
                    }
                }
            }
        }
    }
    class HeaderViewHolder(val headerBinding: ItemFolderBinding) : RecyclerView.ViewHolder(headerBinding.root){
        fun bind(item : DataItem.Header){
            headerBinding.header = item
        }
    }


    private  val  itemList = arrayListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType){
            VIEW_TYPE_FOLDER -> FolderViewHolder(ItemFolderChildBinding.inflate(LayoutInflater.from(parent.context),parent,false))
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemFolderBinding.inflate(LayoutInflater.from(parent.context),parent,false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder) {
            is FolderViewHolder -> holder.bind(itemList[position] as DataItem.Folder)
            is HeaderViewHolder -> holder.bind(itemList[position] as DataItem.Header)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(itemList[position]){
            is DataItem.Header -> VIEW_TYPE_HEADER
            is DataItem.Folder -> VIEW_TYPE_FOLDER
            else -> throw IllegalArgumentException("Not found VIEW_TYPE")
        }
    }
    fun updateList(updateList: List<Any>){
        itemList.clear()
        itemList.addAll(updateList)
        notifyDataSetChanged()
    }

    fun setViewModel(viewModel: FolderViewModel){
        this.viewModel = viewModel
    }
    fun setTopicID(topicID: String){
        this.topicID = topicID
    }
}