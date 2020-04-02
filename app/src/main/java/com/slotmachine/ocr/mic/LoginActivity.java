package com.slotmachine.ocr.mic;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private CheckBox checkBox;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
        }

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        checkBox = findViewById(R.id.checkBox);
        TextView textView = findViewById(R.id.textView);

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
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            finish();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.putExtra("comingFromLogin", true);
                            startActivity(intent);
                        } else {
                            showToast("Incorrect username or password.  Try again.");
                        }
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
