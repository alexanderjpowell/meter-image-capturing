package com.slotmachine.ocr.mic;

import android.app.ProgressDialog;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class SignUpActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText reenterPasswordEditText;
    private Button signUpButton;
    private TextView loginActivityTextView;
    private ProgressDialog progressDialog;

    private String displayName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            finish();
            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
        }

        displayName = "";

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        reenterPasswordEditText = findViewById(R.id.reenterPasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        loginActivityTextView = findViewById(R.id.loginActivityTextView);

        progressDialog = new ProgressDialog(this);

        signUpButton.setOnClickListener(this);
        loginActivityTextView.setOnClickListener(this);
    }

    private void registerUser() {

        if (!validate()) {
            return;
        }

        displayName = nameEditText.getText().toString().trim();
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

        progressDialog.setMessage("Registering.  Please Wait...");
        progressDialog.show();

        //creating a new user
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Set user display name
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName).build();
                            user.updateProfile(profileUpdates);

                            finish();
                            startActivity(new Intent(SignUpActivity.this, MainActivity.class));
                        } else {
                            showToast(task.getException().getMessage());
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

    private boolean validate() {
        boolean valid = true;

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String reenterPassword = reenterPasswordEditText.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            valid = false;
        }

        if (password.isEmpty() || password.length() < 4) {
            passwordEditText.setError("Password must be at least 4 characters");
            valid = false;
        }

        if (!password.equals(reenterPassword)) {
            passwordEditText.setError("Passwords don't match");
            valid = false;
        }

        return valid;
    }
}
