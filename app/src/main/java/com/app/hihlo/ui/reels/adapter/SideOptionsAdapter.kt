package com.app.hihlo.ui.reels.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.AdapterReelsSideOptionsBinding
import com.app.hihlo.utils.RTVariable

class SideOptionsAdapter(
    val list: List<Int>,
    val reels_id: Int,
    val adapterPosition: Int,
    private var isLiked: Int,
    val from: String,
    val otherUserId: Int,
    val currentUserId: String,
    var likeCount: Int,
    var commentsCount: Int,
    private val getSelected: (Int) -> Unit,
): RecyclerView.Adapter<SideOptionsAdapter.ViewHolder>() {
    inner class ViewHolder(val binding:AdapterReelsSideOptionsBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterReelsSideOptionsBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    private var lastAnimatedReelId: Int? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {

            if (position==0){
                count.isVisible=true
                count.text = RTVariable.formatCount(likeCount)
            }else if (position==1){
                count.isVisible=true
                count.text = RTVariable.formatCount(commentsCount)
            }else{
                count.isVisible=false
            }
            if (from=="profile"){
                if (position==3||position==5){
                    root.alpha = 0.5f
                }else if(position==2){
                    if (currentUserId!=otherUserId.toString()){
                        root.alpha = 1f
                        root.setOnClickListener {
                            getSelected(position)
                        }
                    }else{
                        root.alpha = 0.5f
                    }
                }
                else{
                    root.alpha = 1f
                    root.setOnClickListener {
                        getSelected(position)
                    }
                }
            }
            else{
                root.alpha = 1f
                root.setOnClickListener {
                    if (position==1){
                        //Toast.makeText(holder.binding.root.context, "CLICKED ST ${reels_id} || ${position}", Toast.LENGTH_LONG).show()
                        RTVariable.POST_ID = reels_id.toString()
                        RTVariable.COMMENT_FROM = true
                        RTVariable.POST_POSITION = adapterPosition
                    }
                    getSelected(position)
                }
            }
            // Apply 3dp padding if position == 2, else 0dp
            val paddingInDp = if (position == 0) 0 else if (position==1) 3 else 5
            val scale = image.resources.displayMetrics.density
            val paddingInPx = (paddingInDp * scale).toInt()
            image.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)


            image.setImageResource(list[position])
            if (position==0){
                if (isLiked==1){
                    image.setImageResource(R.drawable.btn_heart_fill)
                    if (lastAnimatedReelId != reels_id) {
                        lastAnimatedReelId = reels_id
                        image.animate().cancel()
                        image.scaleX = 0.7f
                        image.scaleY = 0.7f
                        image.alpha = 0.5f
                        image.animate()
                            .scaleX(1.4f)
                            .scaleY(1.4f)
                            .alpha(1f)
                            .setDuration(180)
                            .withEndAction {
                                image.animate()
                                    .scaleX(1f)
                                    .scaleY(1f)
                                    .setDuration(180)
                                    .start()
                            }
                            .start()
                    }
                } else{
                    image.setImageResource(R.drawable.btn_heart_normal)
                    image.scaleX = 1f
                    image.scaleY = 1f
                    image.alpha = 1f
                }
            }
        }
    }
    fun update(updatedLikeStatus:Int){
        isLiked = updatedLikeStatus
        if (isLiked==1) likeCount++ else likeCount--
        notifyDataSetChanged()
    }

    fun updateCommentCountDeleted(count:Int){
        commentsCount = count
        notifyDataSetChanged()
    }
    fun updateCommentCount(){
        commentsCount++
        notifyDataSetChanged()
    }
}