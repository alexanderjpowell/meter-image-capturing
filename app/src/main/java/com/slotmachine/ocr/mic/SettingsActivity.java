package com.slotmachine.ocr.mic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.design.button.MaterialButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth firebaseAuth;

    private MaterialButton signOutButton;
    private MaterialButton changePasswordButton;
    private MaterialButton deleteAccountButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();

        signOutButton = (MaterialButton)findViewById(R.id.signOutButton);
        changePasswordButton = (MaterialButton)findViewById(R.id.changePasswordButton);
        deleteAccountButton = (MaterialButton)findViewById(R.id.deleteAccountButton);

        signOutButton.setOnClickListener(this);
        changePasswordButton.setOnClickListener(this);
        deleteAccountButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {

        if (view == signOutButton) {

            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                finish();
                                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                            } else {
                                showToast(task.getException().getMessage());
                            }
                        }
                    });

        } else if (view == deleteAccountButton) {

            new AlertDialog.Builder(this)
                    .setTitle("Delete Account")
                    .setMessage("Are you sure you want to delete your account? This operation cannot be undone.")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        finish();
                                        startActivity(new Intent(SettingsActivity.this, SignUpActivity.class));
                                    } else {
                                        showToast(task.getException().getMessage());
                                    }
                                }
                            });
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();

        } else if (view == changePasswordButton) {
            startActivity(new Intent(SettingsActivity.this, ChangePasswordActivity.class));
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
