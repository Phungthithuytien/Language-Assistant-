package com.iiex.languageassistant.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.ItemHintTopicBinding
import com.iiex.languageassistant.databinding.ItemRankingTopicBinding
import com.iiex.languageassistant.ui.activities.AuthorProfileActivity
import com.iiex.languageassistant.ui.activities.TopicDetailPersonalActivity
import com.iiex.languageassistant.viewmodels.TopicDetailPersonalViewModel
import java.io.Serializable

class TopTopicsAdapter(private val context: Context): RecyclerView.Adapter<TopTopicsAdapter.TopTopicsHolder>() {


    class TopTopicsHolder(private val holder: ItemHintTopicBinding) :
        RecyclerView.ViewHolder(holder.root) {
        fun bind(item: DataItem.Topic, position: Int, context: Context) {
            holder.topic = item
            val imageName = "top${position + 1}" // tạo tên file dựa trên vị trí
            val imageResId = itemView.context.resources.getIdentifier(
                imageName,
                "drawable",
                itemView.context.packageName
            )

            holder.layoutAuthor.setOnClickListener {
                val intent = Intent(context, AuthorProfileActivity::class.java)
                intent.putExtra("authorID", item.authorID)
                context.startActivity(intent)
            }
            Glide.with(itemView)
                .load(imageResId)
                .into(holder.imageTop)

            Glide.with(itemView)
                .load(item.authorAvatar)
                .into(holder.imageViewAvt)
        }
    }

    private val itemList = arrayListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopTopicsHolder {
        val topicRankingBinding = ItemHintTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopTopicsHolder(topicRankingBinding)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: TopTopicsHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item as DataItem.Topic, position,context)
        holder.itemView.setOnClickListener(View.OnClickListener {
            val intent = Intent(context, TopicDetailPersonalActivity::class.java)
            intent.putExtra("key", item as Serializable)
            context.startActivity(intent)
        })

    }

    fun updateList(updateList: List<DataItem.Topic>) {
        itemList.clear()
        itemList.addAll(updateList)
        notifyDataSetChanged()
    }
}