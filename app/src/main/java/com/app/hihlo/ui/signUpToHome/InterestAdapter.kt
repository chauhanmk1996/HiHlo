package com.app.hihlo.ui.signUpToHome

import com.app.hihlo.R
import com.app.hihlo.base.BaseRecyclerAdapter
import com.app.hihlo.databinding.ListItemInterestBinding
import com.app.hihlo.model.interest_list.response.Interests

class InterestAdapter(
    private val interestSelected: (Int) -> Unit,
) : BaseRecyclerAdapter<ListItemInterestBinding>() {

    private var interestList: ArrayList<Interests> = ArrayList()
    private  var selectedPosition: Int = -1

    override fun getLayoutId(): Int = R.layout.list_item_interest

    override fun getItemCount(): Int {
        return interestList.size
    }

    override fun bind(binding: ListItemInterestBinding, position: Int) {
        binding.apply {
            val interest = interestList[position]
            val isSelected = position == selectedPosition

            tvInterest.text = interest.name

            btnRadio.setImageResource(
                if (isSelected) R.drawable.checked_radio else R.drawable.unchecked_radio
            )

            clInterest.setOnClickListener {
                interestSelected(position)
            }
        }
    }

    fun addList(list: ArrayList<Interests>) {
        interestList.clear()
        interestList.addAll(list)
        notifyDataSetChanged()
    }

    fun updateSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }
}