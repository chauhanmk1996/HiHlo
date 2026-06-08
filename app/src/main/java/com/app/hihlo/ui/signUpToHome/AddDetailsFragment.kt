package com.app.hihlo.ui.signUpToHome

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseNewFragment
import com.app.hihlo.databinding.FragmentAddDetailsBinding
import com.app.hihlo.model.gender_list.Gender
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.getValue

class AddDetailsFragment :
    BaseNewFragment<FragmentAddDetailsBinding, SignUpToHomeViewModel>(R.layout.fragment_add_details) {

    val mViewModel: SignUpToHomeViewModel by activityViewModels()
    private var signUpRequest: SignUpRequest? = null
    private var selectedGenderId: Int? = null
    private var selectedDate: String = getString(R.string.select_dob)

    override fun onInitDataBinding(viewBinding: FragmentAddDetailsBinding) {
        arguments?.let {
            signUpRequest = it.getParcelable("data")
        }
        if (selectedDate !== getString(R.string.select_dob)) {
            viewBinding.tvDob.text = selectedDate
        }
        observer(viewBinding)
        mViewModel.getGenderListApi()
        onClick(viewBinding)
    }

    private fun observer(viewBinding: FragmentAddDetailsBinding) {
        mViewModel.genderListResponse.observe(this) {
            if (it?.peekContent() != null) {
                it.peekContent().payload.genderList.let { list ->
                    setUpGenderSpinner(list, viewBinding)
                }
                mViewModel.genderListResponse.value = null
            }
        }
    }

    private fun setUpGenderSpinner(list: List<Gender>, viewBinding: FragmentAddDetailsBinding) {
        val genderNames = list.map { it.gender_name }
        val adapter = object : ArrayAdapter<String>(
            requireActivity(),
            R.layout.spinner_item_selected,
            genderNames
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                view.setBackgroundColor(ContextCompat.getColor(context, R.color.theme))
                return view
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup,
            ): View {
                val view = super.getDropDownView(position, convertView, parent)
                if (position == viewBinding.spinnerGender.selectedItemPosition) {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                } else {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.black))
                }
                return view
            }
        }

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        viewBinding.spinnerGender.adapter = adapter

        viewBinding.spinnerGender.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    selectedGenderId = list[position].id
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun onClick(viewBinding: FragmentAddDetailsBinding) {
        viewBinding.apply {
            ivBack.setOnClickListener {
                findNavController().popBackStack()
            }

            clSelectDob.setOnClickListener {
                openDateCalender(viewBinding)
            }

            clNext.setOnClickListener {
                if (selectedDate.isEmpty() || selectedDate == getString(R.string.select_dob)) {
                    showToast(getString(R.string.please_select_date_of_birth))
                } else {
                    val gender = selectedGenderId
                    val signUpRequest = SignUpRequest(
                        name = signUpRequest?.name ?: "",
                        username = signUpRequest?.username ?: "",
                        email = signUpRequest?.email ?: "",
                        phoneNumber = signUpRequest?.phoneNumber ?: "",
                        gender_id = gender.toString(),
                        dob = selectedDate,
                        password = signUpRequest?.password ?: "",
                        deviceType = signUpRequest?.deviceType ?: "",
                        city = signUpRequest?.password ?: "",
                        deviceToken = signUpRequest?.deviceToken ?: ""
                    )
                    val bundle = Bundle()
                    bundle.putParcelable("data", signUpRequest)
                    findNavController().navigate(R.id.addCountryFragment, bundle)
                }
            }
        }
    }

    private fun openDateCalender(viewBinding: FragmentAddDetailsBinding) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireActivity(),
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - selectedCalendar.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < selectedCalendar.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }

                if (age >= 13) {
                    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = formatter.format(selectedCalendar.time)
                    selectedDate = formattedDate
                    viewBinding.tvDob.text = formattedDate
                } else {
                    showToast(getString(R.string.you_must_be_at_least_13_years_old))
                    selectedDate = getString(R.string.select_dob)
                    viewBinding.tvDob.text = selectedDate
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }

    override fun getViewModel(): SignUpToHomeViewModel {
        return mViewModel
    }
}