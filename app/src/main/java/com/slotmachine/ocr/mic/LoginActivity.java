package com.slotmachine.ocr.mic;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private MaterialButton loginButton;
    private MaterialCheckBox checkBox;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }

        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        loginButton = findViewById(R.id.login_button);
        checkBox = findViewById(R.id.check_box);
        MaterialTextView textView = findViewById(R.id.tac_text_view);

        String message = "I have read and agree to the terms and conditions";
        SpannableString ss = new SpannableString(message);
        ss.setSpan(new MyClickableSpan(), message.length() - "terms and conditions".length(), message.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        textView.setText(ss);

        progressDialog = new ProgressDialog(this);

        loginButton.setOnClickListener(this);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        loginButton.setEnabled(false);
    }

    private void userLogin() {

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showToast("Please enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showToast("Please enter password");
            return;
        }

        progressDialog.setMessage("Logging in.  Please Wait...");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        onEvent(FirebaseAnalytics.Event.LOGIN, task.getResult().getUser().getUid());
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("comingFromLogin", true);
                        startActivity(intent);
                        finish();
                    } else {
                        showToast("Incorrect username or password.  Try again.");
                    }
                });
    }

    @Override
    public void onClick(View view) {
        if (view == loginButton) {
            userLogin();
        }
    }

    public void onCheckboxClicked(View view) {
        loginButton.setEnabled(checkBox.isChecked());
    }

    // Prevent back press from logging user back in
    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    class MyClickableSpan extends ClickableSpan {

        public void onClick(@NonNull View textView) {
            Intent intent = new Intent(LoginActivity.this, DisclaimerActivity.class);
            startActivity(intent);
        }

        @Override
        public void updateDrawState(@NonNull TextPaint ds) {
            ds.setUnderlineText(true);
        }
    }
}
