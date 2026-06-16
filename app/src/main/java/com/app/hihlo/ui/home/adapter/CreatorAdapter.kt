package com.app.hihlo.ui.home.adapter

import android.content.Context
import androidx.core.view.isVisible
import com.app.hihlo.R
import com.app.hihlo.base.BaseRecyclerAdapter
import com.app.hihlo.databinding.ListItemCreatorBinding
import com.app.hihlo.model.home.response.Creator
import com.bumptech.glide.Glide

class CreatorAdapter(
    private val onSelected: (Int) -> Unit,
) : BaseRecyclerAdapter<ListItemCreatorBinding>() {

    private var creatorList: ArrayList<Creator> = ArrayList()

    override fun getLayoutId(): Int = R.layout.list_item_creator

    override fun getItemCount(): Int {
        return creatorList.size
    }

    override fun bind(binding: ListItemCreatorBinding, position: Int) {
        binding.apply {
            val context = binding.root.context
            val creator = creatorList[position]

            val layoutParams = binding.root.layoutParams
            val randomHeight = (190..300).random()
            layoutParams.height = randomHeight.dpToPx(context)
            binding.root.layoutParams = layoutParams

            creator.creatorDetail?.let { creatorDetail ->
                if (creator.display_image?.isNotEmpty() == true) {
                    Glide.with(root.context).load(creator.display_image).into(ivImage)
                }else if (creatorDetail.profile_image.isNotEmpty()){
                    Glide.with(root.context).load(creatorDetail.profile_image).into(ivImage)
                }else{
                    Glide.with(root.context).load(R.drawable.profile_placeholder).into(ivImage)
                }

                tvInterest.text = creatorDetail.interest_name
                //Glide.with(root.context).load(creatorDetail.interest_image).into(ivInterest)

                when (creatorDetail.user_live_status) {
                    "1" -> {
                        ivOnlineStatusImage.setImageResource(R.drawable.online_status_green)
                        tvOnline.text = "Online"
                    }

                    "2", "3" -> {
                        ivOnlineStatusImage.setImageResource(R.drawable.offline_status_red)
                        tvOnline.text = "Offline"
                    }
                }

                Glide.with(root.context).load(creatorDetail.profile_image)
                    .placeholder(R.drawable.profile_placeholder)
                    .error(R.drawable.profile_placeholder).into(ivCreatorPic)
                tvCreatorName.text = creatorDetail.name
                verifiedNameTick.isVisible = creatorDetail.is_creator == 1
                tvCreatorLocation.text = creatorDetail.city + ", " + creatorDetail.country
            }

            cvCreator.setOnClickListener {
                onSelected(creator.creator_id?:0)
            }
        }
    }

    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    fun addList(list: ArrayList<Creator>) {
        val start = creatorList.size
        creatorList.addAll(list)
        notifyItemRangeInserted(start, creatorList.size)
    }

    fun clearList() {
        creatorList.clear()
        notifyDataSetChanged()
    }
}