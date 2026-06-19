package com.app.hihlo.ui.HomeNew.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.imageVideoConverter.ImageVideoConverter
import com.app.hihlo.R
import com.app.hihlo.databinding.FilePickerForStatusBinding
import com.app.hihlo.ui.HomeNew.model.MediaModel
import com.bumptech.glide.Glide

class FilePickerStatus : AppCompatActivity() {

    private lateinit var binding: FilePickerForStatusBinding
    private lateinit var recyclerView: RecyclerView
    private val mediaList = ArrayList<MediaModel>()

    private val converterLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val returnedUri = result.data?.getStringExtra("uri") ?: ""
            val returnedType = result.data?.getStringExtra("type") ?: ""
            // Now use the values as needed. For example, you can log them:
            Log.d("RETURNED_DATA", "uri = $returnedUri, type = $returnedType")
            // Or pass them to another function, update UI, etc.
            setResult(RESULT_OK, Intent().apply {
                putExtra("uri", returnedUri)
                putExtra("type", returnedType)
            })
            Handler(Looper.getMainLooper()).postDelayed({ finish() }, 200)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FilePickerForStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this@FilePickerStatus, 3)
        checkPermission()
        var downY = 0f
        binding.root.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downY = event.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    val diff = event.rawY - downY
                    if (diff > 0) {
                        binding.root.translationY = diff
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val diff = event.rawY - downY
                    if (diff > 250) {
                        finish()
                    } else {
                        binding.root.animate()
                            .translationY(0f)
                            .setDuration(150)
                            .start()
                    }
                }
            }
            true
        }
    }

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
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            loadAllMedia()
        }
    }

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

        val cursor = contentResolver.query(
            collection,
            projection,
            selection,
            args,
            sortOrder
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
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                    else ->
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                }

                val mediaType =
                    if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE)
                        "image"
                    else
                        "video"

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

    // Important: declare as "inner" to access outer activity's converterLauncher
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

            holder.txtType.text = item.mediaType
            holder.txtDuration.text =
                if (item.mediaType == "video") formatDuration(item.duration) else ""

            holder.itemView.setOnClickListener {
                Log.d("MEDIA_DATA", "uri = ${item.uri}")
                Log.d("MEDIA_DATA", "actual_path = ${item.actualPath}")
                Log.d("MEDIA_DATA", "media_type = ${item.mediaType}")
                Log.d("MEDIA_DATA", "file_size = ${item.fileSize}")
                Log.d("MEDIA_DATA", "duration = ${item.duration}")

                val isVideo = item.mediaType == "video"

                val intent = Intent(holder.itemView.context, ImageVideoConverter::class.java).apply {
                    putExtra("uri", item.uri.toString())
                    putExtra("isVideo", isVideo)
                }
                // Use the launcher (now accessible because adapter is inner)
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

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.slide_down)
    }

}