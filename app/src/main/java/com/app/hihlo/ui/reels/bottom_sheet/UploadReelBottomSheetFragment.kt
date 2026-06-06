package com.app.hihlo.ui.reels.bottom_sheet

import android.app.Dialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentUploadReelBottomSheetBinding
import com.app.hihlo.model.staticModel.StaticLists.uploadReelList
import com.app.hihlo.ui.reels.adapter.AdapterListPopup
import com.app.hihlo.utils.CommonUtils.toPx
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class UploadReelBottomSheetFragment :BottomSheetDialogFragment() {
    private var _binding: FragmentUploadReelBottomSheetBinding? = null
    private val binding get() = _binding!!
    var onGallerySelected: ((Int) -> Unit)? = null
    var onUploadTypeSelected: ((Int) -> Unit)? = null

    companion object {
        private const val ARG_CHECK = "arg_check"

//        fun newInstance(previousScreen: String): UploadMediaBottomSheet {
//            val fragment = UploadMediaBottomSheet()
//            val args = Bundle()
//            args.putString(ARG_CHECK, previousScreen)
//            fragment.arguments = args
//            return fragment
//        }
    }
    val previousScreen: String?
        get() = arguments?.getString(ARG_CHECK)

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadReelBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val cornerRadius = 20f.toPx(requireContext())
                val shapeDrawable = MaterialShapeDrawable(
                    ShapeAppearanceModel.Builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                        .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                        .build()
                )
                shapeDrawable.fillColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.white))
                it.background = shapeDrawable

            }
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        when(previousScreen){
            "reel" ->{
                binding.listPopupRecycler.adapter = AdapterListPopup(/*uploadReelList*/listOf(uploadReelList[1])){
//                    when(it){
//                        uploadReelList[0]->{
//                            onUploadTypeSelected?.invoke(0)
//                            dismiss()
//                        }
//                        uploadReelList[1]->{
                    onUploadTypeSelected?.invoke(1)
                    dismiss()
//                        }
//                    }
                }
            }
        }
    }

    private fun onClick() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}