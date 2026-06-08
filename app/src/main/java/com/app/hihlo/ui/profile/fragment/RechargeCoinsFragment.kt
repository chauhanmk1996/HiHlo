package com.app.hihlo.ui.profile.fragment

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.app.hihlo.R
import com.app.hihlo.base.BaseFragment
import com.app.hihlo.databinding.FragmentRechargeCoinsBinding
import com.app.hihlo.model.add_coins.AddCoinsRequest
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.adapter.AdapterRechargeCoins
import com.app.hihlo.ui.profile.view_model.RechargeCoinsViewModel
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener

import org.json.JSONObject


class RechargeCoinsFragment : BaseFragment<FragmentRechargeCoinsBinding>(), PaymentResultListener {
    private val viewModel: RechargeCoinsViewModel by viewModels()
    var selectedCoins = ""
    override fun initView(savedInstanceState: Bundle?) {
        onClick()
    }
    private fun onClick() {
        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }
    override fun getLayoutId(): Int {
        return R.layout.fragment_recharge_coins
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObserver()
        viewModel.hitRechargeCoinsApi()

    }

    override fun onResume() {
        super.onResume()
        viewModel.hitRechargeCoinsApi()
    }
    private fun setObserver() {
        viewModel.getRechargeCoinsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.rechargeCoinsRecycler.adapter = AdapterRechargeCoins(it.data.payload, ::getSelectedCoins)

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
        viewModel.getAddCoinsLiveData().observe(viewLifecycleOwner) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){

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
    fun getSelectedCoins(data:RechargePackageListResponse.Payload){
        selectedCoins = data.coins.toString()
        startPayment(requireActivity())
    }
    fun startPayment(activity: Activity) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_b3OUKR1FfVBM3j") // Replace with your Razorpay public key

        try {
            val options = JSONObject()
            options.put("name", "HiHlo")
            options.put("description", "Payment for order Recharge Coins")
            options.put("image", "https://your-logo-url.com/logo.png") // Optional
            options.put("theme.color", R.color.theme)
            options.put("currency", "INR")
            options.put("amount", "100") // Amount in paise (₹500.00)

            val prefill = JSONObject()
            prefill.put("email", "test@example.com")
            prefill.put("contact", "9876543210")
            options.put("prefill", prefill)

            checkout.open(activity, options)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("TAG", "startPayment: "+e.message)
            Toast.makeText(activity, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    override fun onPaymentSuccess(razorpayPaymentID: String) {
        Toast.makeText(requireContext(), "Payment Successful: $razorpayPaymentID", Toast.LENGTH_SHORT).show()
        Log.d("Razorpay", "Success: $razorpayPaymentID")
        viewModel.hitAddCoinsApi(token = "Bearer "+ Preferences.getCustomModelPreference<LoginResponse>(requireContext(), LOGIN_DATA)?.payload?.authToken, request = AddCoinsRequest(payment_id = razorpayPaymentID, coins = selectedCoins))
    }

    // Called when payment fails
    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(requireContext(), "Payment Failed: $response", Toast.LENGTH_SHORT).show()
        Log.e("Razorpay", "Error ($code): $response")
    }
}