package com.redevrx.video_trimmer.view

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.redevrx.video_trimmer.R

class TimelineAdapter(
    private val frames: ArrayList<Bitmap>
) : RecyclerView.Adapter<TimelineAdapter.FrameViewHolder>() {

    class FrameViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val imageView: ImageView = itemView.findViewById(R.id.ivFrame)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FrameViewHolder {

        return FrameViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_timeline_frame, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: FrameViewHolder,
        position: Int
    ) {
        holder.imageView.setImageBitmap(frames[position])
    }

    override fun getItemCount(): Int {
        return frames.size
    }
}