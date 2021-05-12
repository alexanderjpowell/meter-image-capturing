package com.slotmachine.ocr.mic

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

open class BaseActivity : AppCompatActivity() {

    private lateinit var mFirebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mFirebaseAnalytics = Firebase.analytics
    }

    protected fun onEvent(eventName: String, param: String?) {
        val bundle = Bundle()
        bundle.putString("param", param)
        onEvent(eventName, bundle)
    }

    private fun onEvent(eventName: String, bundle: Bundle) {
        mFirebaseAnalytics.logEvent(eventName, bundle)
    }

}