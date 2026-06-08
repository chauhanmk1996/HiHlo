package com.app.hihlo.ui.signUpToHome

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentAddCountryBinding
import com.app.hihlo.model.city_list.response.Cities
import com.app.hihlo.utils.hide
import com.app.hihlo.utils.show
import kotlin.getValue

class AddCountryFragment :
    BaseNewFragment<FragmentAddCountryBinding, SignUpToHomeViewModel>(R.layout.fragment_add_country) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    var signUpRequest: SignUpRequest? = null
    private lateinit var cityAdapter: CityAdapter
    private var cityList: ArrayList<Cities> = ArrayList()
    private var selectedPosition: Int = -1
    private var selectedCity: String = ""
    private var page: Int = 0

    override fun onInitDataBinding(viewBinding: FragmentAddCountryBinding) {
        mViewModel.mSearchLiveData.value = ""
        arguments?.let {
            signUpRequest = it.getParcelable("data")
        }
        setUpCityAdapter(viewBinding)
        observer(viewBinding)
        setupSearchListener(viewBinding)
        onClick(viewBinding)
        nestedScrollViewListener(viewBinding)
    }

    private fun setUpCityAdapter(viewBinding: FragmentAddCountryBinding) {
        cityAdapter = CityAdapter { pos ->
            selectedCity = cityList[pos].city_name
            selectedPosition = pos
            cityAdapter.updateSelectedPosition(pos)
        }
        viewBinding.rvCity.adapter = cityAdapter
    }

    private fun observer(viewBinding: FragmentAddCountryBinding) {
        mViewModel.cityListResponse.observe(this) {
            if (it?.peekContent() != null) {
                it.peekContent().payload.cities.let { list ->
                    cityList.addAll(list)
                }

                if (cityList.isEmpty()) {
                    viewBinding.tvNoCity.show()
                    viewBinding.rvCity.hide()
                } else {
                    viewBinding.tvNoCity.hide()
                    viewBinding.rvCity.show()
                    cityAdapter.addList(cityList)
                }
                mViewModel.cityListResponse.value = null
            }
        }
    }

    private fun setupSearchListener(viewBinding: FragmentAddCountryBinding) {
        viewBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                selectedCity = ""
                cityList.clear()
                cityAdapter.clearList()
                page = 1
                mViewModel.getCityListApi(page, false)
            }
        })
    }

    private fun onClick(viewBinding: FragmentAddCountryBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }

            clNext.setOnClickListener {
                if (selectedCity.isEmpty()) {
                    showToast(getString(R.string.please_select_a_city))
                } else {
                    val data = SignUpRequest(
                        name = signUpRequest?.name ?: "",
                        username = signUpRequest?.username ?: "",
                        email = signUpRequest?.email ?: "",
                        phoneNumber = signUpRequest?.phoneNumber ?: "",
                        gender_id = signUpRequest?.gender_id ?: "",
                        dob = signUpRequest?.dob ?: "",
                        password = signUpRequest?.password ?: "",
                        deviceType = signUpRequest?.deviceType ?: "",
                        confirmPassword = signUpRequest?.password ?: "",
                        city = selectedCity,
                        deviceToken = signUpRequest?.deviceToken ?: ""
                    )
                    val bundle = Bundle()
                    bundle.putParcelable("data", data)
                    findNavController().navigate(R.id.selectInterestFragment, bundle)
                }
            }
        }
    }

    private fun nestedScrollViewListener(viewBinding: FragmentAddCountryBinding) {
        viewBinding.nvCity.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val view = v.getChildAt(v.childCount - 1)
                val diff = view.bottom - (v.height + scrollY)
                if (diff == 0) {
                    page += 1
                    mViewModel.getCityListApi(page, true)
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        cityList.clear()
        cityAdapter.clearList()
        page = 1
        mViewModel.getCityListApi(page, true)
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}