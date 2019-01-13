package com.slotmachine.ocr.mic;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText reenterPasswordEditText;
    private Button signUpButton;
    private TextView loginActivityTextView;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        }

        nameEditText = (EditText)findViewById(R.id.nameEditText);
        emailEditText = (EditText)findViewById(R.id.emailEditText);
        passwordEditText = (EditText)findViewById(R.id.passwordEditText);
        reenterPasswordEditText = (EditText)findViewById(R.id.reenterPasswordEditText);
        signUpButton = (Button)findViewById(R.id.signUpButton);
        loginActivityTextView = (TextView)findViewById(R.id.loginActivityTextView);

        progressDialog = new ProgressDialog(this);

        signUpButton.setOnClickListener(this);
        loginActivityTextView.setOnClickListener(this);
    }

    private void registerUser() {

        //getting email and password from edit texts
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String reenterPassword = reenterPasswordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            showToast("Please enter email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            showToast("Please enter password");
            return;
        }

        if (!password.equals(reenterPassword)) {
            showToast("Passwords don't match.  Try again.");
            return;
        }

        progressDialog.setMessage("Registering Please Wait...");
        progressDialog.show();

        //creating a new user
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            finish();
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                            //showToast("Successfully registered");
                        } else {
                            showToast("Registration Error");
                        }
                        progressDialog.dismiss();
                    }
                });

    }

    @Override
    public void onClick(View view) {
        if (view == signUpButton) {
            registerUser();
        } else if (view == loginActivityTextView) {
            Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
            finish();
            startActivity(intent);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
