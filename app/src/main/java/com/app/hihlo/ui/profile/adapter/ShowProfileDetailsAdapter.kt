package com.app.hihlo.ui.profile.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.databinding.AdapterShowProfileDetailsBinding
import com.app.hihlo.model.staticModel.ProfileDetailModel

class ShowProfileDetailsAdapter(val list: List<ProfileDetailModel>): RecyclerView.Adapter<ShowProfileDetailsAdapter.ViewHolder>() {
    inner class ViewHolder(val binding:AdapterShowProfileDetailsBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowProfileDetailsAdapter.ViewHolder {
        return ViewHolder(AdapterShowProfileDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ShowProfileDetailsAdapter.ViewHolder, position: Int) {
        holder.binding.apply {
            image.setImageResource(list[position].image)
            title.text = "   "+list[position].title
        }
    }
}