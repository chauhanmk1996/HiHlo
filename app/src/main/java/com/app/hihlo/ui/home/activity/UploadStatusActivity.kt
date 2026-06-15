package com.app.hihlo.ui.home.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.app.hihlo.R
import com.app.hihlo.base.BaseActivity
import com.app.hihlo.databinding.ActivityUploadStatusBinding
import com.app.hihlo.ui.home.fragment.UploadStatusFragment

class UploadStatusActivity : BaseActivity<ActivityUploadStatusBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fl_upload_status, UploadStatusFragment())
        ft.commit()
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_upload_status
    }

}