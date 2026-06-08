package com.app.hihlo.ui.profile.activity
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import com.app.hihlo.R
import com.app.hihlo.base.BaseActivity
import com.app.hihlo.databinding.ActivityHomeBinding
import com.app.hihlo.databinding.ActivityRechargeCoinsBinding
import com.app.hihlo.model.add_coins.AddCoinsRequest
import com.app.hihlo.ui.signUpToHome.LoginResponse
import com.app.hihlo.model.recharge_package.response.RechargePackageListResponse
import com.app.hihlo.model.wallet_history.WalletHistoryResponse
import com.app.hihlo.preferences.LOGIN_DATA
import com.app.hihlo.preferences.Preferences
import com.app.hihlo.ui.profile.adapter.AdapterRechargeCoins
import com.app.hihlo.ui.profile.adapter.AdapterWalletHistory
import com.app.hihlo.ui.profile.fragment.WithdrawCoinsFragment
import com.app.hihlo.ui.profile.view_model.RechargeCoinsViewModel
import com.app.hihlo.ui.profile.view_model.WalletHistoryViewModel
import com.app.hihlo.ui.signUpToHome.LoginPayload
import com.app.hihlo.utils.CommonUtils.showCustomDialogWithBinding
import com.app.hihlo.utils.network_utils.ProcessDialog
import com.app.hihlo.utils.network_utils.Status
import com.google.gson.Gson
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener

import org.json.JSONObject


class RechargeCoinsActivity : BaseActivity<ActivityRechargeCoinsBinding>() , PaymentResultListener {

//    private  lateinit var binding: ActivityRechargeCoinsBinding

    private val viewModel: WalletHistoryViewModel by viewModels()
    private val viewModelRecharge: RechargeCoinsViewModel by viewModels()

    var selectedCoins = ""

    var currentCoins: Int?=null
    var loginResponse: LoginPayload?=null
    override fun getLayoutId(): Int {
        return R.layout.activity_recharge_coins // Ensure this points to the correct layout resource
    }
    override fun onResume() {
        super.onResume()

        this?.window?.let { window ->
            // Explicit black nav bar
            window.navigationBarColor = ContextCompat.getColor(this, R.color.black_1c1c1c)

            // Force nav buttons white
            WindowInsetsControllerCompat(window, window.decorView)
                .isAppearanceLightNavigationBars = false
        }
        setObserver()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        binding = ActivityRechargeCoinsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loginResponse = Preferences.getCustomModelPreference<LoginResponse>(this, LOGIN_DATA)?.payload ?: LoginPayload()
        Log.i("TAG", "onCreate loginResponse: "+loginResponse)
        onClick()
        binding.withdraw.isVisible = loginResponse?.isCreator==1
        viewModel.hitWalletHistoryDataApi("Bearer "+ loginResponse?.authToken)
        viewModelRecharge.hitRechargeCoinsApi()
    }

    
    private fun onClick() {
        /* binding.plusButton.setOnClickListener {
             val intent = Intent(this, RechargeCoinsActivity::class.java)
             rechargeResultLauncher.launch(intent)
         }*/
        binding.withdraw.setOnClickListener {
            val fragment = WithdrawCoinsFragment()
            val bundle = Bundle()
            bundle.putInt("totalCoins", currentCoins ?: 0)
            fragment.arguments = bundle

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.backButton.setOnClickListener {
            onBackPressed()
        }
        binding.tvCoinAmount.setOnClickListener {
            startPayment(this)
        }
        binding.tvContinueWithAd.setOnClickListener {
            startActivity(Intent(this, ViewAdsActivity::class.java))
        }
    }

    fun startPayment(activity: Activity) {
        val checkout = Checkout()
        val razorPayId = loginResponse?.RAZOR_PAY_DETAILS?.RAZOR_PAYID
        checkout.setKeyID(razorPayId) // Replace with your Razorpay public key

        try {
            val options = JSONObject()
            options.put("name", "HiHlo")
            options.put("description", "Payment for order Recharge Coins")
            options.put("image", "https://your-logo-url.com/logo.png") // Optional
            options.put("theme.color", R.color.theme)
            options.put("currency", "INR")
            options.put("amount", selectedCoins) // Amount in paise (₹500.00)

            val prefill = JSONObject()
            prefill.put("email", "")
            prefill.put("contact", "")
            options.put("prefill", prefill)

            checkout.open(activity, options)

        } catch (e: Exception) {
            e.printStackTrace()
            Log.i("TAG", "startPayment: "+e.message)
            Toast.makeText(activity, "Error in payment: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    override fun onPaymentSuccess(razorpayPaymentID: String) {
        Toast.makeText(this, "Payment Successful", Toast.LENGTH_SHORT).show()
        Log.d("Razorpay", "Success: $razorpayPaymentID")
        /*viewModelRecharge.hitAddCoinsApi(token = "Bearer "+ loginResponse?.authToken, request = AddCoinsRequest(payment_id = razorpayPaymentID, coins = selectedCoins))*/


        viewModelRecharge.hitAddCoinsApi(
            token = "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                this, LOGIN_DATA
            )?.payload?.authToken,
            request = AddCoinsRequest(
                payment_id = razorpayPaymentID,
                coins = selectedCoins,
                amount = selectedCoins
            )
        )

        // Directly refresh wallet from here instead of ActivityResult
    }

    // Called when payment fails
    override fun onPaymentError(code: Int, response: String?) {
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_SHORT).show()
        Log.e("Razorpay", "Error ($code): $response")
    }

    /*  private val rechargeResultLauncher = registerForActivityResult(
          ActivityResultContracts.StartActivityForResult()
      ) { result ->
          if (result.resultCode == Activity.RESULT_OK) {
  //            val coinsAdded = result.data?.getIntExtra("coinsAdded", 0)
              viewModel.hitWalletHistoryDataApi("Bearer "+ loginResponse?.authToken)
  
          } else if (result.resultCode == 101) {
              // Custom result code handling (e.g., user cancelled payment)
          }
      }
  */


    private fun setObserver() {
        viewModelRecharge.getAddCoinsLiveData().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "wallet history success: ${Gson().toJson(it)}")
                        if (it.data?.code == 200){
                            viewModel.hitWalletHistoryDataApi(
                                "Bearer " + Preferences.getCustomModelPreference<LoginResponse>(
                                    this, LOGIN_DATA
                                )?.payload?.authToken
                            )
                        }else{
//                            Toast.makeText(this, it.data.message, Toast.LENGTH_SHORT).show()
                        }

                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(this, true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

        viewModel.getWalletHistoryLiveData().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "wallet history success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            currentCoins = it.data.payload?.totalCoins
                            binding.coinNumber.text = it.data.payload?.totalCoins.toString()
                            Log.d("TAG", "setObggserver: ${it.data.payload?.history} user ${it.data.payload?.user}")
                            binding.rcvWalletPackage.adapter = AdapterWalletHistory(it.data.payload?.history ?: listOf(), it.data.payload?.user ?: WalletHistoryResponse.Payload.User())
                        }else{
                            Toast.makeText(this, it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(this, "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(this, true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }

        viewModelRecharge.getRechargeCoinsLiveData().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "Following List success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            binding.rechargeCoinsRecycler.adapter = AdapterRechargeCoins(it.data.payload,::getSelectedCoins)
                            binding.tvCoinAmount.text = "Continue with ${it.data.payload.get(0).coins} Coins"
                            selectedCoins = it.data.payload.get(0).coins.toString()
                        }else{
                            Toast.makeText(this, it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(this, "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(this, true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }



        viewModel.getWithdrawCoinsLiveData().observe(this) {
            when (it.status) {
                Status.SUCCESS -> {
                    Log.e("TAG", "withdraw history success: ${Gson().toJson(it)}")
                    if (it.data?.status==1){
                        if (it.data.code == 200){
                            Toast.makeText(this, it.data.message, Toast.LENGTH_SHORT).show()

                        }else{
                            Toast.makeText(this, it.data.message, Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        Toast.makeText(this, "${it.data?.message}", Toast.LENGTH_SHORT).show()
                    }
                    ProcessDialog.dismissDialog(true)
                }
                Status.LOADING -> {
                    ProcessDialog.showDialog(this, true)
                }
                Status.ERROR -> {
                    Log.e("TAG", "Login Failed: ${it.message}")
                    ProcessDialog.dismissDialog(true)
                }
            }
        }
    }

    fun getSelectedCoins(data:RechargePackageListResponse.Payload){
        binding.tvCoinAmount.text = "Continue with ${data.coins} Coins"
        selectedCoins = data.coins.toString()
    }

}
