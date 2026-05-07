package com.app.hihlo.ui.profile.become_creater

import GridSpacingItemDecoration
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentAddYourPhotoBinding
import com.app.hihlo.preferences.UserPreference
import com.app.hihlo.ui.profile.adapter.ImageAdapter
import com.app.hihlo.ui.profile.fragment.ProfileFragment.Companion.REQUEST_CODE_CROP_VIDEO
import com.app.hihlo.ui.profile.model.ImageItem
import com.app.hihlo.utils.MediaUtils
import com.yalantis.ucrop.UCrop
import java.io.File

class AddYourPhotoFragment : Fragment() {
    private lateinit var binding: FragmentAddYourPhotoBinding
    private val imageList = MutableList(4) { ImageItem() }
    private lateinit var adapter: ImageAdapter
    private var selectedPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddYourPhotoBinding.inflate(layoutInflater)
        setupRecyclerView()
        initViews()
        return  binding.root
    }

    private fun initViews() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnAddPhotos.setOnClickListener {
            Log.e("TAG", "initViews: ${imageList}", )
            val allUrisPresent = imageList.all { it.imageUri != null }

            if (!allUrisPresent) {
                Toast.makeText(requireActivity(), "Please add all 4 pictures", Toast.LENGTH_SHORT).show()
            } else {
                val bundle = Bundle().apply {
                    putParcelableArrayList("image_list", ArrayList(imageList))
                }
                findNavController().navigate(R.id.captureVideoFragment, bundle)
            }
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {

           /* imageList[selectedPosition].imageUri = it
            adapter.notifyDataSetChanged()*/
            openCropActivity(it)
        }
    }

    private fun setupRecyclerView() {
        adapter = ImageAdapter(imageList,
            onAddImageClick = { position -> pickImageFromGallery(position) },
            onDeleteImageClick = { position -> removeImage(position) }
        )
        binding.rcvAddImage.layoutManager = GridLayoutManager(requireActivity(), 2)
        binding.rcvAddImage.adapter = adapter
        val spacingInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics
        ).toInt()
        binding.rcvAddImage.addItemDecoration(GridSpacingItemDecoration(2, spacingInPx))
    }

    private fun openCropActivity(imageUri: Uri) {
        val options = UCrop.Options().apply {
            setFreeStyleCropEnabled(true)
        }
        val destinationUri = Uri.fromFile(File(requireActivity().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .start(requireContext(), this)
    }

    private fun pickImageFromGallery(position: Int) {
        selectedPosition = position
        imagePickerLauncher.launch("image/*")
        // Launch gallery and handle result using ActivityResultLauncher
        // After result, set: imageList[position].imageUri = pickedUri; adapter.notifyDataSetChanged()
    }

    private fun removeImage(position: Int) {
        imageList.removeAt(position)
        imageList.add(ImageItem()) // Add empty slot at the end
        adapter.notifyDataSetChanged()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data) // Always call super
        Log.i("TAG", "onActivityResult: "+"outside")
        if (resultCode==RESULT_OK){
            when(requestCode){
                UCrop.REQUEST_CROP -> {
                    val resultUri = UCrop.getOutput(data!!)
                    imageList[selectedPosition].imageUri = resultUri
                    adapter.notifyDataSetChanged()
                }
            }
        }else {
            Log.w("HomeFragment", " cropping was cancelled or failed with code: $resultCode")
        }

    }
}