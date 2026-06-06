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
import com.app.hihlo.databinding.FragmentCallTypeBottomSheetBinding
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.staticModel.StaticLists.callTypeList
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.utils.CommonUtils.toPx
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class CallTypeBottomSheetFragment :BottomSheetDialogFragment() {
    private var _binding: FragmentCallTypeBottomSheetBinding? = null
    private val binding get() = _binding!!
    var onCallTypeSelected: ((Int) -> Unit)? = null

    companion object {
        private const val ARG_CHECK = "arg_check"

        fun newInstance(previousScreen: String): CallTypeBottomSheetFragment {
            val fragment = CallTypeBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_CHECK, previousScreen)
            fragment.arguments = args
            return fragment
        }
    }
    val previousScreen: String?
        get() = arguments?.getString(ARG_CHECK)

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallTypeBottomSheetBinding.inflate(inflater, container, false)
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
                shapeDrawable.fillColor = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.screen_background_black))
                it.background = shapeDrawable

            }
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        when(previousScreen){
//            "reel" ->{
        binding.apply {
            videoCallCoins.text = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.video_call_charges.toString()
            audioCallCoins.text = Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.audio_call_charges.toString()
            item1.text = callTypeList[0]
            item2.text = callTypeList[1]
            audioCallLayout.setOnClickListener {
                onCallTypeSelected?.invoke(0)
                dismiss()
            }
            videoCallLayout.setOnClickListener {
                onCallTypeSelected?.invoke(1)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}