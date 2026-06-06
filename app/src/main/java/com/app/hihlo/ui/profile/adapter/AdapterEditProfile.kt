package com.app.hihlo.ui.profile.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.app.hihlo.databinding.AdapterEditProfileBinding
import com.app.hihlo.model.edit_profile.request.EditProfileRequest
import com.app.hihlo.model.get_profile.UserDetailsX
import com.app.hihlo.model.interest_list.response.Interests
import com.app.hihlo.model.staticModel.EditProfileColumnModel
import com.app.hihlo.model.staticModel.editProfileDataMap

class AdapterEditProfile(
    val list: List<EditProfileColumnModel>,
    val userDetails: UserDetailsX,
    val getSelectedField: (String) -> Unit
) : RecyclerView.Adapter<AdapterEditProfile.ViewHolder>() {
    private val inputMap = mutableMapOf<String, String>()
    private var interestList:List<Interests> = listOf()

    inner class ViewHolder(val binding:AdapterEditProfileBinding):RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(AdapterEditProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.apply {
            title.text = editProfileDataMap[list[position].key]

            when (list[position].key) {
                "name" -> {
                    editText.setText(userDetails.name)
                    editText.setHint("Enter your name")
                    inputMap["name"] = userDetails.name.toString()
                }
                "username" -> {
                    editText.setText(userDetails.username)
                    editText.setHint("Enter Your User Name")
                    inputMap["username"] = userDetails.username.toString()
                }
                "phone" -> {
                    editText.setText(userDetails.phone)
                    editText.setHint("Enter your Number")
                    inputMap["phone"] = userDetails.phone.toString()
                    editText.isEnabled=false
                }
                "dob" -> {
                    editText.setText(userDetails.dob)
                    editText.setHint("Enter your Date Of Birth")
                    inputMap["dob"] = userDetails.dob.toString()
                    editText.isEnabled=false
                }
                "email" -> {
                    editText.setText(userDetails.email)
                    editText.setHint("Enter your Email")
                    inputMap["email"] = userDetails.email.toString()
                    editText.isEnabled=false
                }
                "city" -> {
                    editText.setText(userDetails.city)
                    editText.setHint("Choose")
                    inputMap["city"] = userDetails.city.toString()
                    editText.isEnabled=false

                }
                "country" -> {
                    editText.setText(userDetails.country)
                    editText.setHint("Choose")
                    inputMap["country"] = userDetails.country.toString()
                    editText.isEnabled=false
                }
                "interestId" -> {
                    editText.setText(userDetails.interest_name.toString())
                    editText.setHint("Choose")
                    inputMap["interestId"] = userDetails.interest_name.toString()
//                    editText.isEnabled=false
                    editText.isClickable = true
                }
                "about" -> {
                    editText.setText(userDetails.about)
                    editText.setHint("About Yourself")
                    inputMap["about"] = userDetails.about.toString()
                }
                else -> {
                    editText.setHint("")
                    editText.setHint("")
                }
            }
            editText.addTextChangedListener {
                inputMap[list[position].key] = it.toString()
            }

            editText.setOnClickListener {
                if (list[position].key=="interestId"){
                    initSpinner(spinner, root.context)
                    Log.i("TAG", "createItemView: 1"+interestList)
                }else{
                    getSelectedField(list[position].key)
                }
            }
        }
    }
    fun collectEditProfileRequest(): EditProfileRequest {
        return EditProfileRequest(
            name = inputMap["name"] ?: "",
            username = inputMap["username"] ?: "",
            phone = inputMap["phone"] ?: "",
            dob = inputMap["dob"] ?: "",
            email = inputMap["email"] ?: "",
            city = inputMap["city"] ?: "",
            country = inputMap["country"] ?: "",
            interestName = inputMap["interestId"] ?: "",
            about = inputMap["about"] ?: ""
        )
    }
    private fun initSpinner(spinner: Spinner, context:Context){

        val adapter = CustomSpinnerAdapter(context, interestList)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = interestList[position]
//                Toast.makeText(this@MainActivity, "Selected: $selected", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    fun updateInterestsList(interestList:List<Interests>){
        this.interestList = interestList
    }
}