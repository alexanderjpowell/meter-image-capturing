package com.slotmachine.ocr.mic

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, MySettingsFragment())
                .commit()
        val firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this@SettingsActivity, LoginActivity::class.java))
            finish()
        }
    }

    override fun onClick(view: View) {}

    override fun onBackPressed() {
        startActivity(Intent(this@SettingsActivity, MainActivity::class.java))
        finish()
    }

}