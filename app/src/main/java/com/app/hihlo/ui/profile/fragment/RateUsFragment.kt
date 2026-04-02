package com.app.hihlo.ui.profile.fragment

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentRateUsBinding
import com.app.hihlo.model.contact_us.request.ContactUsRequest
import com.app.hihlo.model.login.response.LoginResponse
import com.app.hihlo.model.rating_review.RatingReviewRequest
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.home.activity.HomeActivity
import com.app.hihlo.ui.profile.view_model.ContactUsViewModel
import com.app.hihlo.ui.profile.view_model.RateUsViewModel
import com.app.hihlo.utils.CommonUtils
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import kotlin.getValue

class RateUsFragment : Fragment() {
    private lateinit var binding: FragmentRateUsBinding
    private val viewModel: RateUsViewModel by viewModels()

    private var currentRating = 0
    private val stars: List<ImageView> by lazy {
        listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireContext() as HomeActivity).setOnlineStatusVisibility(true)
        CommonUtils.touchHideKeyBoard(view,requireActivity())
        setObserver()

    }
    private fun setObserver() {
        viewModel.getRateUsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }else{
                            Toast.makeText(requireContext(), it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(requireContext(), "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRateUsBinding.inflate(layoutInflater)
        initViews()
        return binding.root
    }

    private fun initViews() {
        setupRatingBar()
        showCharCount()
        binding.clMain.setOnClickListener {
            CommonUtils.hideKeyboard(requireActivity())
        }
        binding.ivBack.setOnClickListener {
            (requireContext() as HomeActivity).setOnlineStatusVisibility(false)
            findNavController().popBackStack()
        }
        binding.updateButton.setOnClickListener { 
            val etText = binding.etCharCount.text.toString()
            if(etText.isEmpty()){
                Toast.makeText(requireActivity(), "Please enter review text", Toast.LENGTH_SHORT).show()
            }else{
                viewModel.hitRateUsDataApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, request = RatingReviewRequest(review = binding.etCharCount.toString(), rating = currentRating.toString()))
            }
        }
    }

    private fun showCharCount() {
        binding.etCharCount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                binding.tvCount.text = "$currentLength/100"
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRatingBar() {
        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                updateStars(index + 1)
            }
        }
    }

    private fun updateStars(rating: Int) {
        currentRating = rating
        for (i in stars.indices) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.filled_star)
            } else {
                stars[i].setImageResource(R.drawable.unfilled_star)
            }
        }
        Log.d("Rating", "User selected rating: $currentRating")
    }

}