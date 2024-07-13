package com.iiex.languageassistant.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.iiex.languageassistant.data.model.Word
import com.iiex.languageassistant.databinding.ItemWordBinding
import com.iiex.languageassistant.viewmodels.WordItemViewModel

class WordItemAdapter(private val viewModel: WordItemViewModel) : RecyclerView.Adapter<WordItemAdapter.ItemWordHolder>() {
    private val itemList = arrayListOf<Word>()
    private var itemHeight: Int = 0
    var onHeightAvailable: ((Int) -> Unit)? = null

    class ItemWordHolder(private val itemWordBinding: ItemWordBinding) : RecyclerView.ViewHolder(itemWordBinding.root) {
        var buttonDelete = itemWordBinding.buttonDeleteWord

        fun bind(item: Word) {
            itemWordBinding.word = item
            itemWordBinding.editTextDine.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    val enteredInput = itemWordBinding.editTextDine.text.toString()
                    if (enteredInput.isEmpty() || enteredInput.isBlank()){
                        itemWordBinding.textlayoutDine.error = "Vui lòng không bỏ trống!"
                    }else{
                        itemWordBinding.textlayoutDine.error = null
                    }
                }else{
                    itemWordBinding.textlayoutDine.error = null
                }
            }
            itemWordBinding.editTextTerm.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    val enteredInput = itemWordBinding.editTextTerm.text.toString()
                    if (enteredInput.isEmpty() || enteredInput.isBlank()){
                        itemWordBinding.textlayoutTerm.error = "Vui lòng không bỏ trống!"
                    }else{
                        itemWordBinding.textlayoutTerm.error = null
                    }
                }else{
                    itemWordBinding.textlayoutTerm.error = null
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemWordHolder {
        val itemWordBinding = ItemWordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItemWordHolder(itemWordBinding)
    }

    override fun onBindViewHolder(holder: ItemWordHolder, position: Int) {
        val item = itemList[position]
        holder.bind(item)
        holder.buttonDelete.setOnClickListener {
            viewModel.deleteItem(position)
            notifyItemRemoved(position)
        }
        if (itemHeight == 0){
            holder.itemView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    holder.itemView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    itemHeight = holder.itemView.height
                    onHeightAvailable?.invoke(itemHeight)
                }
            })
        }
    }


    override fun getItemCount(): Int = itemList.size

    fun getItemHeight():Int = itemHeight
    fun updateList(updateList: List<Word>) {
        itemList.clear()
        itemList.addAll(updateList)
        notifyDataSetChanged()
    }
}
