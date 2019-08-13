package com.slotmachine.ocr.mic;

import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class ChangePasswordActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;

    private EditText currentPasswordEditText;
    private EditText newPassword1EditText;
    private EditText newPassword2EditText;
    private Button submitPasswordChangeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(ChangePasswordActivity.this, LoginActivity.class));
            finish();
            return;
        }

        currentPasswordEditText = findViewById(R.id.currentPasswordEditText);
        newPassword1EditText = findViewById(R.id.newPassword1EditText);
        newPassword2EditText = findViewById(R.id.newPassword2EditText);
        submitPasswordChangeButton = findViewById(R.id.submitPasswordChangeButton);

        submitPasswordChangeButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view == submitPasswordChangeButton) {
            // First check if the current password is valid by attempting to sign in the user
            final String email = firebaseAuth.getCurrentUser().getEmail();
            final String currentPassword = currentPasswordEditText.getText().toString().trim();
            final String newPassword1 = newPassword1EditText.getText().toString().trim();
            final String newPassword2 = newPassword2EditText.getText().toString().trim();

            // Validate data
            if (!newPassword1.equals(newPassword2)) {
                showToast("Passwords don't match. Try again.");
                return;
            }

            if (currentPassword.isEmpty() || newPassword1.isEmpty()) {
                showToast("All values are required.");
                return;
            }

            firebaseAuth.signInWithEmailAndPassword(email, currentPassword)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                firebaseAuth.getCurrentUser().updatePassword(newPassword1)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    showToast("Password changed successfully");
                                                    startActivity(new Intent(ChangePasswordActivity.this, SettingsActivity.class));
                                                } else {
                                                    showToast(task.getException().getMessage());
                                                }
                                            }
                                        });
                            } else {
                                showToast("Current password is incorrect. Try again.");
                            }
                        }
                    });
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
