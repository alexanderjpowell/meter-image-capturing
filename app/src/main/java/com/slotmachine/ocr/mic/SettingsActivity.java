package com.slotmachine.ocr.mic;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.annotation.NonNull;
import com.google.android.material.button.MaterialButton;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener{

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private Switch sortProgressivesSwitch;
    private MaterialButton signOutButton;
    private MaterialButton changePasswordButton;
    private MaterialButton deleteAccountButton;
    private MaterialButton chooseMinValue;
    private MaterialButton changeEmailButton;

    private String minimumProgressiveValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new MySettingsFragment())
                .commit();

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
            finish();
            return;
        }
        database = FirebaseFirestore.getInstance();

        //SharedPreferences sharedPref = this.getSharedPreferences("notifications", Context.MODE_PRIVATE);
        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        //boolean bool = sharedPreferences.getBoolean("notifications", true);
    }

    /*@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
            finish();
            return;
        }
        database = FirebaseFirestore.getInstance();

        sortProgressivesSwitch = (Switch)findViewById(R.id.sortProgressivesSwitch);
        signOutButton = (MaterialButton)findViewById(R.id.signOutButton);
        changePasswordButton = (MaterialButton)findViewById(R.id.changePasswordButton);
        deleteAccountButton = (MaterialButton)findViewById(R.id.deleteAccountButton);
        chooseMinValue = (MaterialButton)findViewById(R.id.chooseMinValue);
        //changeEmailButton = (MaterialButton)findViewById(R.id.changeEmailButton);
        //emailTextView = (TextView)findViewById(R.id.emailTextView);


        sortProgressivesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (sortProgressivesSwitch.isChecked())
                    setSortProgressives(true);
                else
                    setSortProgressives(false);
            }
        });

        signOutButton.setOnClickListener(this);
        changePasswordButton.setOnClickListener(this);
        deleteAccountButton.setOnClickListener(this);
        chooseMinValue.setOnClickListener(this);
        //changeEmailButton.setOnClickListener(this);

        database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {

                            if (task.getResult().get("minimumProgressiveValue") != null) {
                                minimumProgressiveValue = formatDollarValue(task.getResult().get("minimumProgressiveValue").toString());
                            } else {
                                minimumProgressiveValue = "0";
                            }
                            chooseMinValue.setText("Min Value - $" + minimumProgressiveValue);

                            if (task.getResult().get("sortProgressives") != null) {
                                boolean sort = (boolean)task.getResult().get("sortProgressives");
                                if (sort)
                                    sortProgressivesSwitch.setChecked(true);
                                else
                                    sortProgressivesSwitch.setChecked(false);
                            } else {
                                setSortProgressives(true);
                                sortProgressivesSwitch.setChecked(true);
                            }
                        } else {
                            showToast("No connection");
                        }
                    }
                });

        //emailTextView.setText("email: " + firebaseAuth.getCurrentUser().getEmail());


    }*/

    private void setSortProgressives(boolean isSort) {
        Map<String, Boolean> user = new HashMap<>();
        user.put("sortProgressives", isSort);
        database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .set(user);
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
                                        startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
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
        } else if (view == chooseMinValue) {
            final EditText input = new EditText(SettingsActivity.this);
            input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.requestFocus();
            input.setHint("Enter a value");
            androidx.appcompat.app.AlertDialog alertDialog = new androidx.appcompat.app.AlertDialog.Builder(SettingsActivity.this).create();
            alertDialog.setView(input, 100, 70, 100, 0);
            alertDialog.setMessage("What is the minimum value you'd like recorded?");
            alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            alertDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "ADD",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int i) {
                            String minValue = input.getText().toString().trim();
                            if (minValue.equals("")) {
                                return;
                            }
                            if (isDouble(minValue)) {
                                chooseMinValue.setText("Min Value - $" + minValue);

                                Map<String, Object> user = new HashMap<>();
                                user.put("minimumProgressiveValue", Double.parseDouble(minValue));
                                database.collection("users")
                                        .document(firebaseAuth.getCurrentUser().getUid())
                                        .set(user);
                            }
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        } else if (view == changeEmailButton) {
            //showToast("clicked");

            FirebaseUser user = firebaseAuth.getCurrentUser();
            //user.updateEmail("<email>")
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(SettingsActivity.this, MainActivity.class));
        finish();
    }

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private String formatDollarValue(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '.') break;
            sb.append(text.charAt(i));
        }
        return sb.toString();
    }
}
