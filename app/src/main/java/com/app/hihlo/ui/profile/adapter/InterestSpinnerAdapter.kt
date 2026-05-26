package com.app.hihlo.ui.profile.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.app.hihlo.R
import com.app.hihlo.model.interest_list.response.Interests
import com.bumptech.glide.Glide

class InterestSpinnerAdapter(
    context: Context,
    private val interests: List<Interests>
) : ArrayAdapter<Interests>(context, 0, interests) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createCustomView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createCustomView(position, convertView, parent)
    }

    private fun createCustomView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.spinner_item_with_image, parent, false)

        val interest = interests[position]
        val icon = view.findViewById<androidx.appcompat.widget.AppCompatImageView>(R.id.imageViewIcon)
        val text = view.findViewById<TextView>(R.id.textViewName)

        text.text = interest.name

        // Load image using Glide or Coil
        Glide.with(context)
            .load(interest.image_url)
            .into(icon)

        return view
    }
}
