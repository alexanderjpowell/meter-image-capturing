package com.slotmachine.ocr.mic

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.slotmachine.ocr.mic.viewmodels.AuthViewModel

class LoginActivity : AppCompatActivity(), View.OnClickListener {

    private var firebaseAuth: FirebaseAuth? = null
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var checkBox: CheckBox
    private lateinit var progressDialog: ProgressDialog
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //
        firebaseAuth = FirebaseAuth.getInstance()
        if (firebaseAuth!!.currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        //
        val authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        authViewModel.userLiveData.observe(this, {
            if (it != null) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        })
        //

        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        checkBox = findViewById(R.id.checkBox)
        textView = findViewById(R.id.textView)

        val message = "I have read and agree to the terms and conditions"
        val ss = SpannableString(message)
        ss.setSpan(MyClickableSpan(), message.length - "terms and conditions".length, message.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = ss
        progressDialog = ProgressDialog(this)
        loginButton.setOnClickListener(this)
        textView.movementMethod = LinkMovementMethod.getInstance()
        loginButton.isEnabled = false
    }

    private fun userLogin() {
        val email = emailEditText.text.toString().trim { it <= ' ' }
        val password = passwordEditText.text.toString().trim { it <= ' ' }
        if (TextUtils.isEmpty(email)) {
            showToast("Please enter email")
            return
        }
        if (TextUtils.isEmpty(password)) {
            showToast("Please enter password")
            return
        }
        progressDialog.setMessage("Logging in.  Please Wait...")
        progressDialog.show()
        firebaseAuth!!.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        finish()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("comingFromLogin", true)
                        startActivity(intent)
                    } else {
                        showToast("Incorrect username or password.  Try again.")
                    }
                }
    }

    override fun onClick(view: View) {
        if (view === loginButton) {
            userLogin()
        }
    }

    fun onCheckboxClicked(view: View?) {
        loginButton.isEnabled = checkBox.isChecked
    }

    // Prevent back press from logging user back in
    override fun onBackPressed() {
        val startMain = Intent(Intent.ACTION_MAIN)
        startMain.addCategory(Intent.CATEGORY_HOME)
        startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(startMain)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    internal inner class MyClickableSpan : ClickableSpan() {
        override fun onClick(textView: View) {
            val intent = Intent(this@LoginActivity, DisclaimerActivity::class.java)
            startActivity(intent)
        }

        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = true
        }
    }
}