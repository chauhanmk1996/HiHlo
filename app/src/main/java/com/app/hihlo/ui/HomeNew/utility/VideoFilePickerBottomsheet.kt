package com.app.hihlo.ui.HomeNew.utility

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.FilePickerForStatusBinding
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.HomeNew.model.MediaModel
import com.app.hihlo.ui.profile.fragment.ProfileFragment
import com.app.hihlo.ui.trim_video.TrimVideoActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import java.io.File
import androidx.core.net.toUri

class VideoFilePickerBottomsheet : BottomSheetDialogFragment() {

    private var _binding: FilePickerForStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private val mediaList = ArrayList<MediaModel>()
    private var dY = 0f

    override fun getTheme(): Int = R.style.FilePickerTheme

    fun interface OnVideoPickedListener {
        fun onVideoPicked(uri: Uri, ratio: Int) // ratio can be ignored or default
    }

    private var listener: OnVideoPickedListener? = null
    fun setOnVideoPickedListener(listener: OnVideoPickedListener) {
        this.listener = listener
    }

    private val trimVideoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val trimmedUriString = data?.getStringExtra(ProfileFragment.EXTRA_CROPPED_URI) // same as image
            if (!trimmedUriString.isNullOrEmpty()) {
                val uri = trimmedUriString.toUri()
                listener?.onVideoPicked(uri, 0) // 0 = no ratio for video
            }
        }
        dismiss()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FilePickerForStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.titleTextView.text = "New Video"
        recyclerView = binding.recyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        val cornerRadius = 25f.toPx(requireContext())
        val shapeDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel.Builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                .build()
        )

        shapeDrawable.fillColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.bottom_sheet_color)
        )

        binding.root.background = shapeDrawable
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        checkPermission()
        fastScrollerHandle()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun fastScrollerHandle() {
        binding.ivFastScroller.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dY = view.y - event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val recyclerTop = binding.recyclerView.top.toFloat()
                    val recyclerBottom = binding.recyclerView.bottom.toFloat()
                    val newY = event.rawY + dY
                    val minY = recyclerTop
                    val maxY = recyclerBottom - view.height
                    val finalY = newY.coerceIn(minY, maxY)
                    view.y = finalY
                    val proportion = (finalY - recyclerTop) / (maxY - minY)
                    val verticalRange = binding.recyclerView.computeVerticalScrollRange()
                    val verticalExtent = binding.recyclerView.computeVerticalScrollExtent()
                    val scrollRange = verticalRange - verticalExtent
                    val targetScroll = (proportion * scrollRange).toInt()

                    binding.recyclerView.scrollBy(
                        0,
                        targetScroll - binding.recyclerView.computeVerticalScrollOffset()
                    )
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                    -> {
                    view.performClick()
                    true
                }

                else -> false
            }
        }

        binding.recyclerView.addOnScrollListener(
            object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    updateFastScrollerPosition()
                }
            }
        )
    }

    private fun updateFastScrollerPosition() {
        val verticalRange = binding.recyclerView.computeVerticalScrollRange()
        val verticalOffset = binding.recyclerView.computeVerticalScrollOffset()
        val verticalExtent = binding.recyclerView.computeVerticalScrollExtent()
        val scrollRange = verticalRange - verticalExtent
        if (scrollRange <= 0) return
        val proportion = verticalOffset.toFloat() / scrollRange
        val recyclerTop = binding.recyclerView.top.toFloat()
        val recyclerBottom = binding.recyclerView.bottom.toFloat()
        val maxY = recyclerBottom - binding.ivFastScroller.height
        val finalY = recyclerTop + ((maxY - recyclerTop) * proportion)
        binding.ivFastScroller.y = finalY
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val bottomSheet = dialog?.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
        bottomSheet?.apply {
            setBackgroundColor(Color.TRANSPARENT)
            background = null
            clipToOutline = false
        }
    }

    fun Float.toPx(context: android.content.Context): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this,
            context.resources.displayMetrics
        )

    // ─── Permissions ────────────────────────────────────────────────
    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(
                arrayOf(Manifest.permission.READ_MEDIA_VIDEO),
                100
            )
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                100
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }) {
                loadVideos()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission required to show videos",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }
    }

    // ─── Load only videos ───────────────────────────────────────────
    private fun loadVideos() {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        val cursor = requireContext().contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val size = it.getLong(sizeCol)
                val duration = it.getLong(durationCol)

                val contentUri = ContentUris.withAppendedId(collection, id)

                mediaList.add(
                    MediaModel(
                        uri = contentUri,
                        actualPath = "",
                        mediaType = "video",
                        fileSize = size,
                        duration = duration
                    )
                )
            }
        }

        recyclerView.adapter = MediaAdapter(mediaList)
    }

    // ─── Adapter ────────────────────────────────────────────────────
    inner class MediaAdapter(private val list: ArrayList<MediaModel>) :
        RecyclerView.Adapter<MediaAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imgThumb: ImageView = view.findViewById(R.id.imgThumb)
            val txtType: TextView = view.findViewById(R.id.txtType)
            val txtDuration: TextView = view.findViewById(R.id.txtDuration)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.file_picker_for_status_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]

            Glide.with(holder.itemView.context)
                .load(item.uri)
                .into(holder.imgThumb)

            holder.txtType.text = "Video"
            holder.txtDuration.isVisible = true
            holder.txtDuration.text = formatDuration(item.duration)

            holder.itemView.setOnClickListener {
                val uri = item.uri
                UserPreference.seletedUri = Uri.EMPTY
                // 3. Launch TrimVideoActivity with the selected video
                val intent = Intent(requireActivity(), TrimVideoActivity::class.java)
                intent.putExtra("videoUrl", uri.toString())
                trimVideoLauncher.launch(intent)   // <-- use launcher, NOT startActivity
                // do NOT dismiss here – dismiss is called after result
            }
        }
    }

    // ─── Format duration ────────────────────────────────────────────
    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}