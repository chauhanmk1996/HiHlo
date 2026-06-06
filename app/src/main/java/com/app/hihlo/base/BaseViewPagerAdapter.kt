package com.app.hihlo.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

abstract class BasePagerAdapter<VB : ViewDataBinding> :
    RecyclerView.Adapter<BasePagerAdapter<VB>.BindingViewHolder>() {

    abstract fun getLayoutId(): Int
    abstract fun getItemCountList(): Int
    abstract fun bind(binding: VB, position: Int)

    inner class BindingViewHolder(val binding: VB) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding: VB = DataBindingUtil.inflate(inflater, getLayoutId(), parent, false)
        return BindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        bind(holder.binding, position)
        holder.binding.executePendingBindings()
    }

    override fun getItemCount(): Int = getItemCountList()
}