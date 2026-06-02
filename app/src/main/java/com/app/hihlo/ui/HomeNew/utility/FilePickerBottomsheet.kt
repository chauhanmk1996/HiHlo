package com.app.hihlo.ui.HomeNew.utility

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
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
import com.app.hihlo.ImageVideoConverter.ImageVideoConverter
import com.app.hihlo.R
import com.app.hihlo.databinding.FilePickerForStatusBinding
import com.app.hihlo.ui.HomeNew.model.MediaModel
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class FilePickerBottomsheet : BottomSheetDialogFragment() {

    private var _binding: FilePickerForStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private val mediaList = ArrayList<MediaModel>()
    private var listener: OnMediaSelectedListener? = null
    private var dY = 0f

    override fun getTheme(): Int = R.style.FilePickerTheme

    /**
     * Callback interface to return the selected media URI and type.
     */
    fun interface OnMediaSelectedListener {
        fun onMediaSelected(uri: String, type: String, headline_caption: String)
    }

    fun setOnMediaSelectedListener(listener: OnMediaSelectedListener) {
        this.listener = listener
    }

    // Launcher for ImageVideoConverter activity
    private val converterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val returnedUri = result.data?.getStringExtra("uri") ?: ""
            val returnedType = result.data?.getStringExtra("type") ?: ""
            val headline_caption = result.data?.getStringExtra("headline_caption") ?: ""
            Log.d("RETURNED_DATA", "uri = $returnedUri, type = $returnedType")
            listener?.onMediaSelected(returnedUri, returnedType, headline_caption)
            dismiss()
        }
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
        recyclerView = view.findViewById(R.id.recyclerView)
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
        dialog?.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )
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
        val bottomSheet =
            dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.apply {
            setBackgroundColor(Color.TRANSPARENT)
            background = null
            clipToOutline = false
        }
        val container = dialog?.findViewById<View>(com.google.android.material.R.id.container)
        container?.setBackgroundColor(Color.TRANSPARENT)
        container?.background = null
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
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                ), 100
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
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                loadAllMedia()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permissions required to show media",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }
    }

    // ─── Media loading ──────────────────────────────────────────────
    private fun loadAllMedia() {
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Video.VideoColumns.DURATION
        )

        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val args = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        val cursor = requireContext().contentResolver.query(
            collection, projection, selection, args, sortOrder
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val typeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val pathCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val durationCol = it.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val type = it.getInt(typeCol)
                val path = it.getString(pathCol) ?: ""
                val size = it.getLong(sizeCol)
                val duration = it.getLong(durationCol)

                val contentUri = when (type) {
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    else ->
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                }

                val mediaType =
                    if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) "image" else "video"

                mediaList.add(
                    MediaModel(
                        uri = contentUri,
                        actualPath = path,
                        mediaType = mediaType,
                        fileSize = size,
                        duration = duration
                    )
                )
            }
        }

        recyclerView.adapter = MediaAdapter(mediaList)
    }

    // ─── RecyclerView adapter (inner class) ─────────────────────────
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

            holder.txtType.text =
                item.mediaType.replaceFirstChar { it.uppercase() }
            if (item.mediaType == "video") {
                holder.txtDuration.text = formatDuration(item.duration)
                holder.txtDuration.isVisible = true
            } else {
                holder.txtDuration.isVisible = false
            }
//            holder.txtDuration.text =
//                if (item.mediaType == "video") formatDuration(item.duration) else ""

            holder.itemView.setOnClickListener {
                Log.d("MEDIA_DATA", "uri = ${item.uri}")
                Log.d("MEDIA_DATA", "actual_path = ${item.actualPath}")
                Log.d("MEDIA_DATA", "media_type = ${item.mediaType}")
                Log.d("MEDIA_DATA", "file_size = ${item.fileSize}")
                Log.d("MEDIA_DATA", "duration = ${item.duration}")

                val isVideo = item.mediaType == "video"
                val intent = Intent(requireContext(), ImageVideoConverter::class.java).apply {
                    putExtra("uri", item.uri.toString())
                    putExtra("isVideo", isVideo)
                }
                converterLauncher.launch(intent)
            }
        }

        private fun formatDuration(ms: Long): String {
            val sec = ms / 1000
            val min = sec / 60
            val remain = sec % 60
            return String.format("%02d:%02d", min, remain)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}