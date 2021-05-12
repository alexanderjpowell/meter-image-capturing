package com.slotmachine.ocr.mic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.os.Vibrator;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import timber.log.Timber;

public class PinCodeActivity extends AppCompatActivity {

    private PFCodeView mCodeView;
    private View mDeleteButton;
    private View mFingerprintButton;
    private TextView title_text_view;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;
    private boolean authModeEnabled = false;
    private boolean reenterPin;
    private boolean changePin = false;
    private String username;
    private String pinCode;
    private String newPinCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_code);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(PinCodeActivity.this, LoginActivity.class));
            return;
        }
        database = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        authModeEnabled = intent.getBooleanExtra("authMode", false);
        pinCode = intent.getStringExtra("pinCode");
        username = intent.getStringExtra("username");
        changePin = intent.getBooleanExtra("changePin", false);
        authModeEnabled = true;
        reenterPin = false;

        title_text_view = findViewById(R.id.title_text_view);
        if (changePin) {
            title_text_view.setText(R.string.enter_new_pin_title_pf);
        } else {
            title_text_view.setText(R.string.lock_screen_title_pf);
        }
        mCodeView = findViewById(R.id.code_view);

        mCodeView.setCodeLength(4); // Only allow 4 digit pin codes for now
        TextView button_0 = findViewById(R.id.button_0);
        TextView button_1 = findViewById(R.id.button_1);
        TextView button_2 = findViewById(R.id.button_2);
        TextView button_3 = findViewById(R.id.button_3);
        TextView button_4 = findViewById(R.id.button_4);
        TextView button_5 = findViewById(R.id.button_5);
        TextView button_6 = findViewById(R.id.button_6);
        TextView button_7 = findViewById(R.id.button_7);
        TextView button_8 = findViewById(R.id.button_8);
        TextView button_9 = findViewById(R.id.button_9);

        button_0.setOnClickListener(mOnKeyClickListener);
        button_1.setOnClickListener(mOnKeyClickListener);
        button_2.setOnClickListener(mOnKeyClickListener);
        button_3.setOnClickListener(mOnKeyClickListener);
        button_4.setOnClickListener(mOnKeyClickListener);
        button_5.setOnClickListener(mOnKeyClickListener);
        button_6.setOnClickListener(mOnKeyClickListener);
        button_7.setOnClickListener(mOnKeyClickListener);
        button_8.setOnClickListener(mOnKeyClickListener);
        button_9.setOnClickListener(mOnKeyClickListener);

        mDeleteButton = findViewById(R.id.button_delete);
        mFingerprintButton = findViewById(R.id.button_finger_print);

        mDeleteButton.setOnClickListener(mOnDeleteButtonClickListener);
        mDeleteButton.setOnLongClickListener(mOnDeleteButtonOnLongClickListener);
    }

    private final View.OnClickListener mOnKeyClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final String string = ((TextView) view).getText().toString();
            if (string.length() != 1) {
                return;
            }
            final int codeLength = mCodeView.input(string);
            configureRightButton(codeLength);

            if (changePin && (codeLength == 4)) {
                if (!reenterPin) { // first pin submission
                    newPinCode = mCodeView.getCode();
                    mCodeView.clearCode();
                    title_text_view.setText(R.string.reenter_pin_title_pf);
                    reenterPin = true;
                } else {
                    if (mCodeView.getCode().equals(newPinCode)) { // Correctly duplicated pin
                        // Change pin in database
                        updatePinInDatabase(newPinCode, username);
                        //
                        startActivity(new Intent(getApplicationContext(), ManageUsersActivity.class));
                    } else {
                        mCodeView.clearCode();
                        title_text_view.setText(R.string.pin_mismatch_title_pf);
                        doVibrate();
                    }
                }
                //Intent intent = new Intent(getApplicationContext(), PinCodeActivity.class);
                //startActivity(intent);
                return;
            }

            // Correct Pin Entered
            if (mCodeView.getCode().equals(pinCode)) {
                //Start new activity
                updateCurrentUser(username);
                startActivity(new Intent(getApplicationContext(), ManageUsersActivity.class));
            }

            // Wrong Pin Entered
            if ((codeLength == 4) && !mCodeView.getCode().equals(pinCode)) {
                final Animation animShake = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shake_pf);
                mCodeView.startAnimation(animShake);
                mCodeView.clearCode();
                title_text_view.setText(R.string.wrong_pin_title_pf);
                doVibrate();
            }
        }
    };

    private void updatePinInDatabase(String pin, String username) {
        DocumentReference documentReference = database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("displayNames")
                .document(username);

        documentReference.update("pinCode", pin)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        showToast("Pin successfully updated");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast("Failed to update pin, please check your connection");
                    }
                });
    }

    private void updateCurrentUser(String username) {
        Context context = getApplicationContext();
        SharedPreferences sharedPref = context.getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("username", username);
        editor.apply();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void configureRightButton(int codeLength) {
        if (authModeEnabled) {
            if (codeLength > 0) {
                mDeleteButton.setVisibility(View.VISIBLE);
            } else {
                mDeleteButton.setVisibility(View.GONE);
            }
            return;
        }

        if (codeLength > 0) {
            mFingerprintButton.setVisibility(View.GONE);
            mDeleteButton.setVisibility(View.VISIBLE);
            mDeleteButton.setEnabled(true);
            return;
        }

        mFingerprintButton.setVisibility(View.GONE);
        mDeleteButton.setVisibility(View.VISIBLE);
        mDeleteButton.setEnabled(false);
    }

    private final View.OnClickListener mOnDeleteButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int codeLength = mCodeView.delete();
            configureRightButton(codeLength);
        }
    };

    private final View.OnLongClickListener mOnDeleteButtonOnLongClickListener
            = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            mCodeView.clearCode();
            configureRightButton(0);
            return true;
        }
    };

    private void doVibrate() {
        try {
            int VIBRATE_LENGTH_MILLISECONDS = 500;
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_LENGTH_MILLISECONDS, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(VIBRATE_LENGTH_MILLISECONDS);
                }
            }
        } catch (Exception ex) {
            Timber.d("Error performing vibration");
        }
    }
}
