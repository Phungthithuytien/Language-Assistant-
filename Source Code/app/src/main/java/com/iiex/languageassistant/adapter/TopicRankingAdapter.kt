package com.iiex.languageassistant.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iiex.languageassistant.databinding.ItemRankingTopicBinding
import com.iiex.languageassistant.viewmodels.TopicDetailPersonalViewModel


class TopicRankingAdapter : RecyclerView.Adapter<TopicRankingAdapter.TopicRankingHolder>() {


    class TopicRankingHolder(private val topicRankingHolder: ItemRankingTopicBinding) :
        RecyclerView.ViewHolder(topicRankingHolder.root) {
        fun bind(item: TopicDetailPersonalViewModel.RankItem) {
            topicRankingHolder.rankDetail = item

            Glide.with(itemView)
                .load(item.avatarUrl) // Đường dẫn URL của hình ảnh
                .into(topicRankingHolder.imageBtnAvt)
        }
    }

    private val itemList = arrayListOf<Any>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicRankingHolder {
        val topicRankingBinding = ItemRankingTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopicRankingHolder(topicRankingBinding)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: TopicRankingHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item as TopicDetailPersonalViewModel.RankItem)
    }

    fun updateList(updateList: List<TopicDetailPersonalViewModel.RankItem>) {
        itemList.clear()
        itemList.addAll(updateList)
        notifyDataSetChanged()
    }
}
