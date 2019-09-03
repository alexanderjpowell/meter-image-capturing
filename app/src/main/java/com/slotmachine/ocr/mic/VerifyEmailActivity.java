package com.slotmachine.ocr.mic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class VerifyEmailActivity extends AppCompatActivity implements View.OnClickListener {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_email);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser == null) {
            startActivity(new Intent(VerifyEmailActivity.this, LoginActivity.class));
            return;
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.verifyEmailButton) {
            String url = "http://www.example.com/verify?uid=" + firebaseUser.getUid();
            ActionCodeSettings actionCodeSettings = ActionCodeSettings.newBuilder()
                    .setUrl(url)
                    .setAndroidPackageName("com.slotmachine.ocr.mic", false, null)
                    .build();
            firebaseUser.sendEmailVerification(actionCodeSettings)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                            } else {

                            }
                        }
                    });
        }
    }
}
