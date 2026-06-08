package com.app.hihlo.ui.signUpToHome

import com.app.hihlo.R
import com.app.hihlo.base.BaseRecyclerAdapter
import com.app.hihlo.databinding.ListItemCityBinding
import com.app.hihlo.model.city_list.response.Cities

class CityAdapter(
    private val citySelected: (Int) -> Unit,
) : BaseRecyclerAdapter<ListItemCityBinding>() {

    private var cityList: ArrayList<Cities> = ArrayList()
    private var selectedPosition: Int = -1

    override fun getLayoutId(): Int = R.layout.list_item_city

    override fun getItemCount(): Int {
        return cityList.size
    }

    override fun bind(binding: ListItemCityBinding, position: Int) {
        binding.apply {
            val city = cityList[position]
            val isSelected = position == selectedPosition

            tvCity.text = city.city_name

            btnRadio.setImageResource(
                if (isSelected) R.drawable.checked_radio else R.drawable.unchecked_radio
            )

            clCity.setOnClickListener {
                citySelected(position)
            }
        }
    }

    fun addList(list: ArrayList<Cities>) {
        cityList.clear()
        cityList.addAll(list)
        notifyDataSetChanged()
    }

    fun updateSelectedPosition(position: Int) {
        selectedPosition = position
        notifyDataSetChanged()
    }

    fun clearList() {
        cityList.clear()
        notifyDataSetChanged()
    }
}