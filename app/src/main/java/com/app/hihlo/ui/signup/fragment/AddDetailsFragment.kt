package com.app.hihlo.ui.signup.fragment

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.databinding.FragmentAddDetailsBinding
import com.app.hihlo.model.gender_list.Gender
import com.app.hihlo.ui.profile.view_model.AddDetailViewModel
import com.app.hihlo.ui.signUpToHome.SignUpRequest
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddDetailsFragment : Fragment() {
    private lateinit var binding:FragmentAddDetailsBinding
    private val viewModel: AddDetailViewModel by viewModels()
   var selectedGenderId_ :Int ? =null

    private var selectedDate:String? = "Select DOB"
    var signUpRequest: SignUpRequest?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            signUpRequest = it.getParcelable("data")
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAddDetailsBinding.inflate(layoutInflater)
        intiViews()
        return binding.root
    }

    private fun intiViews() {
        hitGenderList()
        if(selectedDate!=="Select DOB"){
            binding.tvDob.text = selectedDate
        }
        binding.apply {
            backButton.setOnClickListener {
                findNavController().popBackStack()
            }
            clSelectDob.setOnClickListener {
                openDateCalender()
            }
            clNext.setOnClickListener {
                if(selectedDate.isNullOrEmpty() || selectedDate=="Select DOB"){
                    Toast.makeText(requireActivity(), "Please select date of birth", Toast.LENGTH_SHORT).show()
                }else{

                    val gender =selectedGenderId_ // binding.spinGender.selectedItemId.toInt()+1
                    val signUpRequest = SignUpRequest(
                        name = signUpRequest?.name,
                        username = signUpRequest?.username,
                        email = signUpRequest?.email,
                        phoneNumber = signUpRequest?.phoneNumber,
                        gender_id = gender.toString(),
                        dob = selectedDate,
                        password = signUpRequest?.password,
                        deviceType = signUpRequest?.deviceType,
                        city = signUpRequest?.password,
                        deviceToken = signUpRequest?.deviceToken
                        )
                    val bundle = Bundle()
                    bundle.putParcelable("data",signUpRequest)
                    Log.d("TAG", "intiVicdsfdsfews: $signUpRequest")
                    findNavController().navigate(R.id.addCountryFragment,bundle)
                }

            }
        }
    }
    fun hitGenderList(){
        viewModel.hitGenderListApi()
    }

    private fun openDateCalender() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireActivity(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                // Calculate age
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - selectedCalendar.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < selectedCalendar.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }

                if (age >= 13) {
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = formatter.format(selectedCalendar.time)
                    selectedDate = formattedDate
                    binding.tvDob.text = formattedDate
                    Log.d("DatePicker", "Selected Date: $formattedDate")
                } else {
                    Toast.makeText(requireContext(), "You must be at least 13 years old", Toast.LENGTH_SHORT).show()
                    selectedDate = "Select DOB"
                    binding.tvDob.text = "Select DOB"
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
   /* private fun initGenderSpinner(gendeList: List<Gender>) {
        val genderNames = gendeList.map { it.gender_name }  // Assuming Gender has a 'name' property
        var selectedGenderId = gendeList.map { it.id }
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item_selected,
            genderNames
        )
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinGender.adapter = adapter

        // ✅ Pre-select gender if exists
        val selectedIndex = genderNames.indexOfFirst { it.equals(userDetailsMain?.gender, ignoreCase = true) }
        if (selectedIndex >= 0) {
            binding.spinGender.setSelection(selectedIndex)
        }

        binding.spinGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedGenderId_ = selectedGenderId[position]
                Log.d("TAG", "onItemSelected: ${selectedGenderId_}")
                // ✅ Do something with selected gender object if needed
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

*/
   private fun initGenderSpinner(data: List<Gender>) {
       val genderNames = data.map { it.gender_name }

       val adapter = object : ArrayAdapter<String>(
           requireActivity(),
           R.layout.spinner_item_selected,
           genderNames
       ) {
           override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
               val view = super.getView(position, convertView, parent)
               // ✅ Background of spinner (selected item)
               view.setBackgroundColor(ContextCompat.getColor(context, R.color.theme))
               return view
           }

           override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
               val view = super.getDropDownView(position, convertView, parent)
               // ✅ Highlight selected item differently in dropdown
               if (position == binding.spinGender.selectedItemPosition) {
                   view.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
               } else {
                   view.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
               }
               return view
           }
       }

       adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
       binding.spinGender.adapter = adapter

       binding.spinGender.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
           override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
               val selectedCity = parent?.getItemAtPosition(position).toString()
               selectedGenderId_ = data[position].id
               Log.d("Spinner", "Selected gender: $selectedCity, id: $selectedGenderId_")
           }

           override fun onNothingSelected(parent: AdapterView<*>?) {}
       }
   }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
    }
    fun setObserver(){
        viewModel.getGenderLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Reels success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            var data = it.data.payload.genderList
                            Log.d("TAG", "setOsdcdcbserver: ${it.data.payload}")
                            initGenderSpinner(data)

                            //  setCountData(it.data.payload.userDetails.posts_count, it.data.payload.userDetails.followers_count, it.data.payload.userDetails.following_count)

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


}