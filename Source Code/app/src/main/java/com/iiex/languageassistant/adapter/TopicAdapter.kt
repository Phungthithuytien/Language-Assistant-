package com.iiex.languageassistant.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.ItemTopicBinding
import com.iiex.languageassistant.databinding.ItemTopicChildBinding
import com.iiex.languageassistant.ui.activities.AuthorProfileActivity
import com.iiex.languageassistant.ui.activities.TopicDetailPersonalActivity
import com.iiex.languageassistant.viewmodels.FolderViewModel
import java.io.Serializable

class TopicAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_TOPIC = 0
        private const val VIEW_TYPE_HEADER = 1
    }

    private lateinit var viewModel: FolderViewModel
    private lateinit var folderID: String

    inner class TopicViewHolder(private val topicBinding: ItemTopicChildBinding) :
        RecyclerView.ViewHolder(topicBinding.root) {
        fun bind(item: DataItem.Topic) {
            topicBinding.topic = item
            Glide.with(context)
                .load(item.authorAvatar) // Đường dẫn URL của hình ảnh
                .into(topicBinding.imageViewAvt)

            topicBinding.layoutAuthor.setOnClickListener {
                val intent = Intent(context, AuthorProfileActivity::class.java)
                intent.putExtra("authorID", item.authorID)
                context.startActivity(intent)
            }
            topicBinding.seekBarWords.progress =
                ((item.wordLearned.toDouble() / item.wordCount.toDouble()) * 100).toInt()
            topicBinding.seekBarWords.setPadding(10, 0, 0, 0)
            //lock seekbar
            topicBinding.seekBarWords.isEnabled = false
            topicBinding.buttonContinue.setOnClickListener {
                val intent = Intent(context, TopicDetailPersonalActivity::class.java)
                intent.putExtra("key", item as Serializable)
                context.startActivity(intent)
            }

            if (item.isAdded != null) {
                if (item.isAdded!!) {
                    topicBinding.buttonContinue.setText("Xóa")
                    topicBinding.buttonContinue.setTextColor(Color.RED)
                    topicBinding.buttonContinue.setOnClickListener {
                        item.topicID?.let { topicID ->
                            viewModel.removeFormFolder(topicID, folderID){
                                if (it) {
                                    topicBinding.buttonContinue.setText("Thêm")
                                    topicBinding.buttonContinue.setTextColor(context.resources.getColor(R.color.blue_400))
                                }
                            }
                        }
                    }
                } else {
                    topicBinding.buttonContinue.setText("Thêm")
                    topicBinding.buttonContinue.setTextColor(context.resources.getColor(R.color.blue_400))
                    topicBinding.buttonContinue.setOnClickListener {
                        item.topicID?.let { topicID ->
                            viewModel.addToFolder(topicID, folderID) {
                                if (it) {
                                    topicBinding.buttonContinue.setText("Xóa")
                                    topicBinding.buttonContinue.setTextColor(Color.RED)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    class HeaderViewHolder(private val headerBinding: ItemTopicBinding) :
        RecyclerView.ViewHolder(headerBinding.root) {
        fun bind(item: DataItem.Header) {
            headerBinding.header = item
        }
    }


    private val itemList = mutableListOf<DataItem>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_TOPIC -> TopicViewHolder(
                ItemTopicChildBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )
            VIEW_TYPE_HEADER -> HeaderViewHolder(
                ItemTopicBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemViewType(position: Int): Int {


        return when (itemList[position]) {
            is DataItem.Header -> VIEW_TYPE_HEADER
            is DataItem.Topic -> VIEW_TYPE_TOPIC
            else -> throw IllegalArgumentException("EO CO CO CO")
        }
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = itemList[position]

        when (holder) {
            is TopicViewHolder -> holder.bind(itemList[position] as DataItem.Topic)
            is HeaderViewHolder -> holder.bind(itemList[position] as DataItem.Header)
        }
    }

    fun updateList(updateList: List<DataItem>) {
        itemList.clear()
        itemList.addAll(updateList)
        notifyDataSetChanged()
    }

    fun setViewModel(viewModel: FolderViewModel) {
        this.viewModel = viewModel
    }

    fun setFolderID(folderID: String) {
        this.folderID = folderID
    }
}


