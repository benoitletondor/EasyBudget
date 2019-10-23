package com.benoitletondor.easybudgetapp.helper

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.benoitletondor.easybudgetapp.R

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
    }
}