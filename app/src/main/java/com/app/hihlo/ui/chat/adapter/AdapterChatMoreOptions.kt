package com.app.hihlo.ui.chat.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.databinding.AdapterChatMoreOptionsBinding
import com.app.hihlo.model.staticModel.ProfileDetailModel

class AdapterChatMoreOptions(val chatMoreOptionsList: List<ProfileDetailModel>, function: () -> Unit) : RecyclerView.Adapter<AdapterChatMoreOptions.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(AdapterChatMoreOptionsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        holder.binding.apply {
            title.text = chatMoreOptionsList[position].title
            iconImage.setImageResource(chatMoreOptionsList[position].image)
        }
    }

    override fun getItemCount(): Int {
        return chatMoreOptionsList.size
    }

    inner class ViewHolder(val binding: AdapterChatMoreOptionsBinding): RecyclerView.ViewHolder(binding.root)
}