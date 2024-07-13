package com.iiex.languageassistant.adapter
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.iiex.languageassistant.R
import com.iiex.languageassistant.data.model.ref.DataItem
import com.iiex.languageassistant.databinding.ItemPostBinding
import com.iiex.languageassistant.ui.activities.AuthorProfileActivity
import com.iiex.languageassistant.ui.activities.TopicDetailPersonalActivity
import com.iiex.languageassistant.util.DateTimeUtil
import java.io.Serializable

class PostAdapter(private val context: Context, private var topicList: List<DataItem.Topic>) :
    RecyclerView.Adapter<PostAdapter.TopicViewHolder>() {
    private var isProfile: Boolean = false
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding: ItemPostBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_post, parent, false
        )
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topicList[position]
        holder.binding.topic = topic
        Glide.with(context)
            .load(topic.authorAvatar)
            .into(holder.binding.imageViewAvt)
        if (!isProfile){
            holder.binding.layoutAuthor.setOnClickListener {
                val intent = Intent(context, AuthorProfileActivity::class.java)
                intent.putExtra("authorID", topic.authorID)
                context.startActivity(intent)
            }
        }
        holder.binding.textViewTime.setText(topic.updatedAt?.let {
            DateTimeUtil.getDateFromTimestamp(
                it
            )
        })
        holder.binding.buttonContinue.setOnClickListener(View.OnClickListener {
            val intent = Intent(context, TopicDetailPersonalActivity::class.java)
            intent.putExtra("key", topic as Serializable)
            context.startActivity(intent)
        })
        holder.binding.executePendingBindings()


    }
    fun setIsProfile(isProfile: Boolean){
        this.isProfile = isProfile
    }

    override fun getItemCount(): Int {
        return topicList.size
    }

    inner class TopicViewHolder(var binding: ItemPostBinding) :  RecyclerView.ViewHolder(
        binding.root
    )
    fun updateData(newList: List<DataItem.Topic>) {
        topicList = newList
        notifyDataSetChanged()
    }
}
