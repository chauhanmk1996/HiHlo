package com.app.hihlo.ui.HomeNew.utility

import android.Manifest
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
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
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
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import java.io.File

class ImageFilePickerBottomsheet : BottomSheetDialogFragment() {

    private var _binding: FilePickerForStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var recyclerView: RecyclerView
    private val mediaList = ArrayList<MediaModel>()
    private var listener: OnMediaSelectedListener? = null

    override fun getTheme(): Int = R.style.FilePickerTheme

    fun interface OnMediaSelectedListener {
        fun onMediaSelected(uri: String, type: String, ratio: Int)
    }

    fun setOnMediaSelectedListener(listener: OnMediaSelectedListener) {
        this.listener = listener
    }

    private val converterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                val extras = result.data?.extras
                val width = extras?.getInt(UCrop.EXTRA_OUTPUT_IMAGE_WIDTH, -1) ?: -1
                val height = extras?.getInt(UCrop.EXTRA_OUTPUT_IMAGE_HEIGHT, -1) ?: -1
                var ratio = 0
                if (width > 0 && height > 0) {
                    val w = width.toFloat()
                    val h = height.toFloat()
                    ratio = when {
                        isCloseTo(w/h, 1f)    -> 2
                        isCloseTo(w/h, 9f/16f) -> 1
                        isCloseTo(w/h, 16f/9f) -> 3
                        else -> 0
                    }
                }
                listener?.onMediaSelected(resultUri.toString(), "image", ratio)
            } else {
                Toast.makeText(requireContext(), "Failed to crop image", Toast.LENGTH_SHORT).show()
            }
            dismiss()
        }
    }

    // Helper (same as your old code)
    private fun isCloseTo(value: Float, target: Float, tolerance: Float = 0.05f): Boolean {
        return kotlin.math.abs(value - target) <= tolerance
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FilePickerForStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.titleTextView.text = "New Post"
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
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
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
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults.all {
                    it == PackageManager.PERMISSION_GRANTED
                }) {
                loadImages()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission required to show images",
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
        }
    }

    // ─── Load only images ───────────────────────────────────────────
    private fun loadImages() {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = requireContext().contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val sizeCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val size = it.getLong(sizeCol)

                val contentUri = ContentUris.withAppendedId(collection, id)

                mediaList.add(
                    MediaModel(
                        uri = contentUri,
                        actualPath = "",
                        mediaType = "image",
                        fileSize = size,
                        duration = 0L
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

            holder.txtType.text =
                item.mediaType.replaceFirstChar { it.uppercase() }
            holder.txtDuration.isVisible = false

            holder.itemView.setOnClickListener {
                Log.d("MEDIA_DATA", "uri = ${item.uri}")
                val options = UCrop.Options().apply {
                    setFreeStyleCropEnabled(false)

                    // Supply only the ratios you want (exclude "Original")
                    setAspectRatioOptions(
                        0, // default selection index
                        AspectRatio("1:1", 1f, 1f),
                        AspectRatio("9:16", 9f, 16f),
                        AspectRatio("16:9", 16f, 9f)
                    )
                }

                val destinationUri = Uri.fromFile(
                    File(requireActivity().cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                )

                val uCropIntent = UCrop.of(item.uri, destinationUri)
                    .withOptions(options)
                    .getIntent(requireContext())
                converterLauncher.launch(uCropIntent)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}