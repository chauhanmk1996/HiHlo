package com.app.hihlo.ui.signup.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentAddCountryBinding
import com.app.hihlo.model.city_list.response.Cities
import com.app.hihlo.ui.profile.view_model.EditProfileViewModel
import com.app.hihlo.ui.signUpToHome.SignUpRequest
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status

class AddCountryFragment : Fragment(),CityAdapter.OnCitySelectedListener {
    private lateinit var binding:FragmentAddCountryBinding
    private lateinit var cityAdapter: CityAdapter
    private val viewModel: EditProfileViewModel by viewModels()
    var selectedCity:String? = null
    var selectedPosition:Int?=-1
    var signUpData: SignUpRequest?=null

    var isLoading = false
    var currentPage=1

    private var originalCityList: MutableList<Cities> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            signUpData = it.getParcelable("data")
            Log.e("TAG", "onCreate: $signUpData", )
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddCountryBinding.inflate(layoutInflater)
        intiViews()
        return binding.root
    }
    private fun intiViews(){
        loadRcv()
        binding.apply {
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }
            clNext.setOnClickListener {
                if(selectedCity.isNullOrEmpty()){
                    Toast.makeText(requireActivity(), "Please select a city", Toast.LENGTH_SHORT).show()
                }else{
                    val data = SignUpRequest(
                        name = signUpData?.name,
                        username = signUpData?.username,
                        email = signUpData?.email,
                        phoneNumber = signUpData?.phoneNumber,
                        gender_id = signUpData?.gender_id,
                        dob = signUpData?.dob,
                        password = signUpData?.password,
                        deviceType = signUpData?.deviceType,
                        confirmPassword = signUpData?.password,
                        city = selectedCity,
                        deviceToken = signUpData?.deviceToken
                    )
                    val bundle = Bundle()
                    bundle.putParcelable("data",data)
                    Log.i("TAG", "intiViews: "+selectedCity)
                    findNavController().navigate(R.id.selectInterestFragment,bundle)
                }
            }

        }
        Log.e("TAG", "intiViews:city $selectedCity", )

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getCityApi()
        viewModel.hitCitiesListDataApi("", "20", currentPage.toString())  // Call API with search + limit

    }
    private fun loadRcv() {
        binding.rcvCity.layoutManager = LinearLayoutManager(requireActivity(),LinearLayoutManager.VERTICAL,false)
        cityAdapter = CityAdapter(requireActivity(),this)
        binding.rcvCity.adapter = cityAdapter
        cityAdapter.selectedPosition = selectedPosition!!
        binding.rcvCity.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
                if (layoutManager?.findLastVisibleItemPosition() == cityAdapter.itemCount - 2) {
                    Log.i("TAG", "onScrolled: "+"triggered")
                    if (isLoading) {
                        currentPage++
                        viewModel.hitCitiesListDataApi("", "10", currentPage.toString())  // Call API with search + limit
                    }
                    isLoading = false
                }
            }
        })
//        getCityApi()
        setupSearchListener()

    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                cityAdapter.selectedPosition = -1
                currentPage=1
                originalCityList.clear()
                viewModel.hitCitiesListDataApi(s.toString(), "20")  // Call API with search + limit

            }
        })
    }

    private fun filterCityList(query: String) {
        val filtered = if (query.isEmpty()) {
            originalCityList
        } else {
            originalCityList.filter {
                it.city_name.contains(query, ignoreCase = true)
            }
        }
        cityAdapter.setData(filtered)
    }


    private fun getCityApi() {
        viewModel.getCitiesListLiveData().observe(viewLifecycleOwner) {
            Log.e("two", "getCityApi: called this", )
            when (it.status) {
                Status.SUCCESS -> {
                    ProcessDialog.dismissDialog(true)
                    if (it.data?.status == 1 && it.data.code == 200) {
                        if (binding.etSearch.text.toString().isNotEmpty()){
                            val list = it.data.payload.cities
                            cityAdapter.setData(list)
                        }else{
                            if (it.data.payload.cities.isNotEmpty() == true) {
                                isLoading=true
                                originalCityList.addAll(it.data.payload.cities.toMutableList())
                                if (currentPage == 1 ) {
                                    cityAdapter.setData(originalCityList)
                                } else {
                                    cityAdapter.setData(originalCityList)
                                }
                            }
                        }

                    } else {
                        Toast.makeText(requireContext(), it.data?.message ?: "Unknown error", Toast.LENGTH_SHORT).show()
                    }
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(requireContext(), true)
                }
                Status.ERROR -> {
                    ProcessDialog.dismissDialog(true)
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCitySelected(city: Cities,position: Int) {
        selectedCity = city.city_name
        selectedPosition = position
    }
}