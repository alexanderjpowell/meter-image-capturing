package com.slotmachine.ocr.mic;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.Menu;
import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;

import android.speech.RecognizerIntent;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnTouchListener {

    public static final int REQUEST_TAKE_PHOTO_PROGRESSIVES = 0;
    public static final int MY_PERMISSIONS_REQUEST_CODE = 1;
    public static final int REQUEST_TAKE_PHOTO_MACHINE_ID = 2;
    public static final int REQUEST_SETTINGS_ACTIVITY = 3;
    public static final int REQUEST_DATA_REPORT_ACTIVITY = 4;
    private static final int REQ_CODE_SPEECH_INPUT = 100;

    private static final String EMPTY_PROGRESSIVE_VALUE = "";

    public String mCurrentPhotoPath;
    public Button submitButton;
    public int progressive = 1;
    public TextInputEditText progressive1;
    public TextInputEditText progressive2;
    public TextInputEditText progressive3;
    public TextInputEditText progressive4;
    public TextInputEditText progressive5;
    public TextInputEditText progressive6;
    public TextInputEditText progressive7;
    public TextInputEditText progressive8;
    public TextInputEditText progressive9;
    public TextInputEditText progressive10;
    public TextInputEditText machineId;

    public TextInputLayout inputLayout0;
    public TextInputLayout inputLayout1;
    public TextInputLayout inputLayout2;
    public TextInputLayout inputLayout3;
    public TextInputLayout inputLayout4;
    public TextInputLayout inputLayout5;
    public TextInputLayout inputLayout6;
    public TextInputLayout inputLayout7;
    public TextInputLayout inputLayout8;
    public TextInputLayout inputLayout9;
    public TextInputLayout inputLayout10;

    private RelativeLayout relativeLayoutProgressive7;
    private RelativeLayout relativeLayoutProgressive8;
    private RelativeLayout relativeLayoutProgressive9;
    private RelativeLayout relativeLayoutProgressive10;

    //public Spinner spinner;
    private String username;

    //private DrawerLayout mDrawerLayout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private Double minimumProgressiveValue;
    private int NUMBER_OF_PROGRESSIVES;

    private Set<String> set = new HashSet<>();

    private Intent intent;

    private int REJECT_DUPLICATES_DURATION_MILLIS, REJECT_DUPLICATES_DURATION_HOURS;
    private boolean REJECT_DUPLICATES;

    private boolean DEBUG = false;

    private static String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure user is signed in
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "No user selected");

        setTitle("User: " + username);
        //

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        REJECT_DUPLICATES = sharedPreferences.getBoolean("reject_duplicates", false);
        REJECT_DUPLICATES_DURATION_MILLIS = sharedPreferences.getInt("reject_duplicates_duration", 0) * 3600 * 1000;
        REJECT_DUPLICATES_DURATION_HOURS = sharedPreferences.getInt("reject_duplicates_duration", 0);

        //
        relativeLayoutProgressive7 = findViewById(R.id.progressive7_relative_layout);
        relativeLayoutProgressive8 = findViewById(R.id.progressive8_relative_layout);
        relativeLayoutProgressive9 = findViewById(R.id.progressive9_relative_layout);
        relativeLayoutProgressive10 = findViewById(R.id.progressive10_relative_layout);

        NUMBER_OF_PROGRESSIVES = sharedPref.getInt("number_of_progressives", 6);
        //showToast(Integer.toString(NUMBER_OF_PROGRESSIVES));
        if (NUMBER_OF_PROGRESSIVES == 6) {
            relativeLayoutProgressive7.setVisibility(View.GONE);
            relativeLayoutProgressive8.setVisibility(View.GONE);
            relativeLayoutProgressive9.setVisibility(View.GONE);
            relativeLayoutProgressive10.setVisibility(View.GONE);
        } else if (NUMBER_OF_PROGRESSIVES == 7) {
            relativeLayoutProgressive7.setVisibility(View.VISIBLE);
            relativeLayoutProgressive8.setVisibility(View.GONE);
            relativeLayoutProgressive9.setVisibility(View.GONE);
            relativeLayoutProgressive10.setVisibility(View.GONE);
        } else if (NUMBER_OF_PROGRESSIVES == 8) {
            relativeLayoutProgressive7.setVisibility(View.VISIBLE);
            relativeLayoutProgressive8.setVisibility(View.VISIBLE);
            relativeLayoutProgressive9.setVisibility(View.GONE);
            relativeLayoutProgressive10.setVisibility(View.GONE);
        } else if (NUMBER_OF_PROGRESSIVES == 9) {
            relativeLayoutProgressive7.setVisibility(View.VISIBLE);
            relativeLayoutProgressive8.setVisibility(View.VISIBLE);
            relativeLayoutProgressive9.setVisibility(View.VISIBLE);
            relativeLayoutProgressive10.setVisibility(View.GONE);
        } else if (NUMBER_OF_PROGRESSIVES == 10) {
            relativeLayoutProgressive7.setVisibility(View.VISIBLE);
            relativeLayoutProgressive8.setVisibility(View.VISIBLE);
            relativeLayoutProgressive9.setVisibility(View.VISIBLE);
            relativeLayoutProgressive10.setVisibility(View.VISIBLE);
        }
        //

        //
        database = FirebaseFirestore.getInstance();
        //
        checkPermissions();
        //

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        headerView.setBackgroundColor(Color.parseColor("#2196F3"));

        progressive1 = findViewById(R.id.progressive1);
        progressive2 = findViewById(R.id.progressive2);
        progressive3 = findViewById(R.id.progressive3);
        progressive4 = findViewById(R.id.progressive4);
        progressive5 = findViewById(R.id.progressive5);
        progressive6 = findViewById(R.id.progressive6);
        progressive7 = findViewById(R.id.progressive7);
        progressive8 = findViewById(R.id.progressive8);
        progressive9 = findViewById(R.id.progressive9);
        progressive10 = findViewById(R.id.progressive10);
        machineId = findViewById(R.id.machineId);
        submitButton = findViewById(R.id.submit_button);

        inputLayout0 = findViewById(R.id.inputLayout0);
        inputLayout1 = findViewById(R.id.inputLayout1);
        inputLayout2 = findViewById(R.id.inputLayout2);
        inputLayout3 = findViewById(R.id.inputLayout3);
        inputLayout4 = findViewById(R.id.inputLayout4);
        inputLayout5 = findViewById(R.id.inputLayout5);
        inputLayout6 = findViewById(R.id.inputLayout6);
        inputLayout7 = findViewById(R.id.inputLayout7);
        inputLayout8 = findViewById(R.id.inputLayout8);
        inputLayout9 = findViewById(R.id.inputLayout9);
        inputLayout10 = findViewById(R.id.inputLayout10);

        resetProgressives();

        progressive1.setOnTouchListener(this);
        progressive2.setOnTouchListener(this);
        progressive3.setOnTouchListener(this);
        progressive4.setOnTouchListener(this);
        progressive5.setOnTouchListener(this);
        progressive6.setOnTouchListener(this);
        progressive7.setOnTouchListener(this);
        progressive8.setOnTouchListener(this);
        progressive9.setOnTouchListener(this);
        progressive10.setOnTouchListener(this);
        machineId.setOnTouchListener(this);

        progressive1.addTextChangedListener(new GenericTextWatcher(progressive1));
        progressive2.addTextChangedListener(new GenericTextWatcher(progressive2));
        progressive3.addTextChangedListener(new GenericTextWatcher(progressive3));
        progressive4.addTextChangedListener(new GenericTextWatcher(progressive4));
        progressive5.addTextChangedListener(new GenericTextWatcher(progressive5));
        progressive6.addTextChangedListener(new GenericTextWatcher(progressive6));
        //
        progressive7.addTextChangedListener(new GenericTextWatcher(progressive7));
        progressive8.addTextChangedListener(new GenericTextWatcher(progressive8));
        progressive9.addTextChangedListener(new GenericTextWatcher(progressive9));
        progressive10.addTextChangedListener(new GenericTextWatcher(progressive10));
        //
        machineId.addTextChangedListener(new GenericTextWatcher(machineId));

        // Populate machine id if coming from to do list activity
        intent = getIntent();
        String machine_id = intent.getStringExtra("machine_id");
        int numberOfProgressives = intent.getIntExtra("numberOfProgressives",0);
        //int numberOfProgressives = 10;
        labelEditTextsFromToDo(machine_id, numberOfProgressives);

        //String[] progressiveDescriptionTitles = intent.getStringArrayExtra("progressiveDescriptionTitles");
        List<String> progressiveDescriptionTitles = intent.getStringArrayListExtra("progressiveDescriptionTitles");
        if (progressiveDescriptionTitles != null) {
            labelEditTextsFromToDo2(progressiveDescriptionTitles);

            // Set additional progressive text boxes if needed
            if (progressiveDescriptionTitles.size() <= 6) {
                relativeLayoutProgressive7.setVisibility(View.GONE);
                relativeLayoutProgressive8.setVisibility(View.GONE);
                relativeLayoutProgressive9.setVisibility(View.GONE);
                relativeLayoutProgressive10.setVisibility(View.GONE);
            } else if (progressiveDescriptionTitles.size() == 7) {
                relativeLayoutProgressive7.setVisibility(View.VISIBLE);
                relativeLayoutProgressive8.setVisibility(View.GONE);
                relativeLayoutProgressive9.setVisibility(View.GONE);
                relativeLayoutProgressive10.setVisibility(View.GONE);
            } else if (progressiveDescriptionTitles.size() == 8) {
                relativeLayoutProgressive7.setVisibility(View.VISIBLE);
                relativeLayoutProgressive8.setVisibility(View.VISIBLE);
                relativeLayoutProgressive9.setVisibility(View.GONE);
                relativeLayoutProgressive10.setVisibility(View.GONE);
            } else if (progressiveDescriptionTitles.size() == 9) {
                relativeLayoutProgressive7.setVisibility(View.VISIBLE);
                relativeLayoutProgressive8.setVisibility(View.VISIBLE);
                relativeLayoutProgressive9.setVisibility(View.VISIBLE);
                relativeLayoutProgressive10.setVisibility(View.GONE);
            } else if (progressiveDescriptionTitles.size() == 10) {
                relativeLayoutProgressive7.setVisibility(View.VISIBLE);
                relativeLayoutProgressive8.setVisibility(View.VISIBLE);
                relativeLayoutProgressive9.setVisibility(View.VISIBLE);
                relativeLayoutProgressive10.setVisibility(View.VISIBLE);
            }
            //
        }
        // Also check if coming from login activity and send verification email if necessary
        boolean comingFromLogin = intent.getBooleanExtra("comingFromLogin", false);
        if (comingFromLogin) {
            //Map<String, Object> data = new HashMap<>();
            //data.put("email", firebaseAuth.getCurrentUser().getEmail());
            //database.collection("users").document(firebaseAuth.getCurrentUser().getUid()).set(data, SetOptions.merge());
            showDialogForAdminAccount();
        }
        if (comingFromLogin && !firebaseAuth.getCurrentUser().isEmailVerified()) {
                firebaseAuth.getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            if (!firebaseAuth.getCurrentUser().isEmailVerified()) {
                                //showToast("Verified: " + Boolean.toString(firebaseAuth.getCurrentUser().isEmailVerified()));
                                firebaseAuth.getCurrentUser().sendEmailVerification();
                            }
                        } else {
                            showToast("Unable to reload user");
                        }
                    }
                });
        }
        //

        // Set minimum progressive value
        //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String val = sharedPreferences.getString("minimum_value", "0");
        try {
            minimumProgressiveValue = Double.valueOf(val);
        } catch (Exception ex) {
            minimumProgressiveValue = 0.0;
        }

        /*boolean reject_duplicates = sharedPreferences.getBoolean("reject_duplicates", false);
        int reject_duplicates_duration = sharedPreferences.getInt("reject_duplicates_duration", 0);
        if (reject_duplicates) {
            populateDuplicatesSet(reject_duplicates_duration);
        }*/
    }

    private void showDialogForAdminAccount() {
        // Check if user is an admin account (if email address is a document id in admins collection)
        // If so, present a dialog box with only the option to sign out and no option to dismiss
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Admin Account");
        String message = "You are attempting to sign in with an admin account.  Please sign out and back in with a casino level account.";
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Sign Out",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                        // Sign out and go to sign in activity
                        dialog.dismiss();
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                    }
                });
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        //alertDialog.show();
        DocumentReference docRef = database.collection("admins").document(firebaseAuth.getCurrentUser().getEmail());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        alertDialog.show();
                        AuthUI.getInstance().signOut(getApplicationContext());
                                /*.addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            finish();
                                            startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                                        } else {
                                            if (task.getException() != null)
                                                Toast.makeText(getApplicationContext(), task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });*/
                    }
                }
            }
        });
        //
    }

    private void labelEditTextsFromToDo(String machine_id, int numberOfProgressives) {
        //numberOfProgressives = (numberOfProgressives > 6) ? 6 : numberOfProgressives;
        numberOfProgressives = (numberOfProgressives > 10) ? 10 : numberOfProgressives;
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled }, // enabled
                new int[] { -android.R.attr.state_enabled }, // disabled
                new int[] { -android.R.attr.state_checked }, // unchecked
                new int[] { android.R.attr.state_pressed }  // pressed
        };
        int[] colors = new int[] {
                Color.GREEN,
                Color.GREEN,
                Color.GREEN,
                Color.GREEN
        };
        ColorStateList colorStateList = new ColorStateList(states, colors);
        machineId.setText(machine_id);
        TextInputLayout[] array = { inputLayout1, inputLayout2, inputLayout3, inputLayout4, inputLayout5, inputLayout6, inputLayout7, inputLayout8, inputLayout9, inputLayout10 };
        for (int i = 0; i < numberOfProgressives; i++) {
            array[i].setDefaultHintTextColor(colorStateList);
        }
    }

    //private void labelEditTextsFromToDo2(String[] progressiveDescriptionTitles) {
    private void labelEditTextsFromToDo2(List<String> progressiveDescriptionTitles) {
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_enabled }, // enabled
                new int[] { -android.R.attr.state_enabled }, // disabled
                new int[] { -android.R.attr.state_checked }, // unchecked
                new int[] { android.R.attr.state_pressed }  // pressed
        };
        int[] colors = new int[] {
                Color.GREEN,
                Color.GREEN,
                Color.GREEN,
                Color.GREEN
        };
        ColorStateList colorStateList = new ColorStateList(states, colors);
        TextInputLayout[] array = { inputLayout1, inputLayout2, inputLayout3, inputLayout4, inputLayout5, inputLayout6, inputLayout7, inputLayout8, inputLayout9, inputLayout10 };
        for (int i = 0; i < progressiveDescriptionTitles.size(); i++) {
            if (progressiveDescriptionTitles.get(i) != null) {
                array[i].setHint(progressiveDescriptionTitles.get(i));
                array[i].setDefaultHintTextColor(colorStateList);
            }
        }
    }

    /*private void populateDuplicatesSet(int duration) {
        set.clear();
        //int offset = 86400; // 24 * 60 * 60
        int offset = duration * 60 * 60;
        //showToast(Integer.toString(duration));
        Date time = new Date(System.currentTimeMillis() - offset * 1000);
        CollectionReference collectionReference = database.collection("scans");
        Query query = collectionReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
                .whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1000);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        set.add(document.get("machine_id").toString().trim());
                    }
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
            }
        });
    }*/

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        final int DRAWABLE_RIGHT = 2;
        switch (v.getId()) {
            case R.id.progressive1:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive1.getRight() - progressive1.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive1.getError() == null) {
                            if (progressive1.getText().toString().equals("")) {
                                progressive = 1;
                                startVoiceInput(1);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive1.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive2:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive2.getRight() - progressive2.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive2.getError() == null) {
                            if (progressive2.getText().toString().equals("")) {
                                progressive = 2;
                                startVoiceInput(2);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive2.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive3:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive3.getRight() - progressive3.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive3.getError() == null) {
                            if (progressive3.getText().toString().equals("")) {
                                progressive = 3;
                                startVoiceInput(3);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive3.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive4:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive4.getRight() - progressive4.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive4.getError() == null) {
                            if (progressive4.getText().toString().equals("")) {
                                progressive = 4;
                                startVoiceInput(4);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive4.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive5:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive5.getRight() - progressive5.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive5.getError() == null) {
                            if (progressive5.getText().toString().equals("")) {
                                progressive = 5;
                                startVoiceInput(5);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive5.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive6:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive6.getRight() - progressive6.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive6.getError() == null) {
                            if (progressive6.getText().toString().equals("")) {
                                progressive = 6;
                                startVoiceInput(6);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive6.setText("");
                            }
                        }
                    }
                }
                break;
            //
            case R.id.progressive7:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive7.getRight() - progressive7.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive7.getError() == null) {
                            if (progressive7.getText().toString().equals("")) {
                                progressive = 7;
                                startVoiceInput(7);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive7.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive8:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive8.getRight() - progressive8.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive8.getError() == null) {
                            if (progressive8.getText().toString().equals("")) {
                                progressive = 8;
                                startVoiceInput(8);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive8.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive9:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive9.getRight() - progressive9.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive9.getError() == null) {
                            if (progressive9.getText().toString().equals("")) {
                                progressive = 9;
                                startVoiceInput(9);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive9.setText("");
                            }
                        }
                    }
                }
                break;
            case R.id.progressive10:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (progressive10.getRight() - progressive10.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (progressive10.getError() == null) {
                            if (progressive10.getText().toString().equals("")) {
                                progressive = 10;
                                startVoiceInput(10);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                progressive10.setText("");
                            }
                        }
                    }
                }
                break;
            //
            case R.id.machineId:
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (machineId.getRight() - machineId.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (machineId.getError() == null) {
                            if (machineId.getText().toString().equals("")) {
                                progressive = 0;
                                startVoiceInput(0);
                                return true;
                            } else {
                                event.setAction(MotionEvent.ACTION_CANCEL);
                                machineId.setText("");
                            }
                        }
                    }
                }
                break;
        }
        return false;
    }

    private void checkPermissions() {
        this.requestPermissions(new String[] {Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.INTERNET},
                MY_PERMISSIONS_REQUEST_CODE);
    }

    /*@Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return false;
        }
        String uid = firebaseAuth.getCurrentUser().getUid();

        return true;
    }

    private void dispatchTakePictureIntent(Integer request) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
                showToast(ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.slotmachine.ocr.mic.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, request);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        /*if (id == R.id.action_bar_spinner) {
            showToast(spinner.getSelectedItem().toString());
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Intent intent;

        if (id == R.id.nav_gallery) {
            intent = new Intent(MainActivity.this, DataReportActivity.class);
            startActivity(intent);
        } else if (id == R.id.to_do_list) {
            intent = new Intent(MainActivity.this, TodoListActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_slideshow) {
            intent = new Intent(MainActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        //} else if (id == R.id.nav_settings) {
        //    intent = new Intent(MainActivity.this, SettingsActivity.class);
        //    startActivity(intent);
        } else if (id == R.id.nav_settings) {

            if (DEBUG) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            } else {

                // Show pop up dialog
                final EditText passwordEditText = new EditText(MainActivity.this);
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordEditText.requestFocus();
                passwordEditText.setHint("Password");

                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setView(passwordEditText, 100, 70, 100, 0);
                alertDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                alertDialog.setTitle("Re-authenticate");
                alertDialog.setMessage("To use MiC in admin mode please provide the password for your account");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SUBMIT",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                String password = passwordEditText.getText().toString().trim();
                                if (password.equals("")) {
                                    return;
                                }
                                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                                user.reauthenticate(credential)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    final Intent intent1;
                                                    intent1 = new Intent(MainActivity.this, SettingsActivity.class);
                                                    startActivity(intent1);
                                                } else {
                                                    showToast("Incorrect password");
                                                }
                                                //Log.d(TAG, "User re-authenticated.");
                                            }
                                        });
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        try {
            switch (requestCode) {
                case REQUEST_TAKE_PHOTO_PROGRESSIVES: {
                    resetProgressives();
                    if (resultCode == RESULT_OK) {
                        File file = new File(mCurrentPhotoPath);
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), Uri.fromFile(file));
                        if (bitmap != null) {
                            ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = exifToDegrees(rotation);
                            Matrix matrix = new Matrix();
                            if (rotation != 0) { matrix.preRotate(rotationInDegrees); }
                            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            processProgressivesOCR(newBitmap);
                            if (file.exists()) {
                                boolean deleted = file.delete();
                            }
                        } else {
                            showToast("Bitmap is null");
                        }
                    }
                    break;
                }
                case REQ_CODE_SPEECH_INPUT: {
                    if (resultCode == RESULT_OK && null != intent) {
                        ArrayList<String> result = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (progressive == 1) {
                            progressive1.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 2) {
                            progressive2.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 3) {
                            progressive3.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 4) {
                            progressive4.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 5) {
                            progressive5.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 6) {
                            progressive6.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 7) {
                            progressive7.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 8) {
                            progressive8.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 9) {
                            progressive9.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 10) {
                            progressive10.setText(formatVoiceToSpeech(result.get(0), true));
                        } else if (progressive == 0) {
                            machineId.setText(formatVoiceToSpeech(result.get(0), false));
                        }
                    }
                    break;
                }
                case REQUEST_SETTINGS_ACTIVITY: {
                    break;
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            Log.d("ERROR", error.getMessage());
            showToast("Error");
        }
    }

    private void resetMachineId() {
        machineId.setText("");
        machineId.clearFocus();
    }

    private void resetProgressives() {
        progressive1.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive2.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive3.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive4.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive5.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive6.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive7.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive8.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive9.setText(EMPTY_PROGRESSIVE_VALUE);
        progressive10.setText(EMPTY_PROGRESSIVE_VALUE);

        progressive1.clearFocus();
        progressive2.clearFocus();
        progressive3.clearFocus();
        progressive4.clearFocus();
        progressive5.clearFocus();
        progressive6.clearFocus();
        progressive7.clearFocus();
        progressive8.clearFocus();
        progressive9.clearFocus();
        progressive10.clearFocus();
    }

    private void processProgressivesOCR(Bitmap bitmap) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance().getCloudDocumentTextRecognizer();
        Task<FirebaseVisionDocumentText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                            @Override
                            public void onSuccess(FirebaseVisionDocumentText firebaseVisionDocumentText) {
                                if (firebaseVisionDocumentText == null) {
                                    Log.d("ERROR", "firebaseVisionDocumentText is null");
                                    showToast("No text detected.  Try again.  ");
                                    return;
                                }
                                StringBuilder sb = new StringBuilder();
                                List<FirebaseVisionDocumentText.Word> filteredWords = new ArrayList<FirebaseVisionDocumentText.Word>();
                                List<Rect> wordDimensions = new ArrayList<Rect>();
                                String machineCode = machineId.getText().toString();
                                List<FirebaseVisionDocumentText.Block> blocks = firebaseVisionDocumentText.getBlocks();
                                if (blocks.size() == 0) {
                                    showToast("No text detected. Try again. ");
                                    return;
                                }
                                for (FirebaseVisionDocumentText.Block block : blocks) {
                                    List<FirebaseVisionDocumentText.Paragraph> paragraphs = block.getParagraphs();
                                    for (FirebaseVisionDocumentText.Paragraph paragraph : paragraphs) {
                                        List<FirebaseVisionDocumentText.Word> words = paragraph.getWords();
                                        Log.d("PARAGRAPH", paragraph.getText());
                                        if (getNumberOfOccurrences(paragraph.getText()) == 2) {
                                            int firstIndex = paragraph.getText().indexOf('#');
                                            int secondIndex = paragraph.getText().indexOf('#', firstIndex + 1);
                                            machineCode = paragraph.getText().substring(firstIndex+1, secondIndex).trim();
                                        }
                                        for (FirebaseVisionDocumentText.Word word : words) {
                                            //Log.d("WORDS", word.getText());
                                            if (!isAlpha(word.getText())) {
                                                sb.append(word.getText().trim());
                                                filteredWords.add(word);
                                                wordDimensions.add(word.getBoundingBox());
                                                printRect(word.getBoundingBox());
                                            }
                                        }
                                    }
                                }

                                Log.d("StringBuilder: ", sb.toString());

                                List<String> dollarValues = TextParser.parse(filteredWords);
                                if (minimumProgressiveValue != null) {
                                    List<String> newDollarValues = new ArrayList<>();
                                    for (int i = 0; i < dollarValues.size(); i++) {
                                        if (Double.parseDouble(dollarValues.get(i)) >= minimumProgressiveValue) {
                                            newDollarValues.add(dollarValues.get(i));
                                        }
                                    }
                                    dollarValues = newDollarValues;
                                }

                                // Add to TextViews
                                machineId.setText(machineCode);

                                for (int i = 0; i < dollarValues.size(); i++) {
                                    if (i == 0) {
                                        progressive1.setText(dollarValues.get(i));
                                    }
                                    if (i == 1) {
                                        progressive2.setText(dollarValues.get(i));
                                    }
                                    if (i == 2) {
                                        progressive3.setText(dollarValues.get(i));
                                    }
                                    if (i == 3) {
                                        progressive4.setText(dollarValues.get(i));
                                    }
                                    if (i == 4) {
                                        progressive5.setText(dollarValues.get(i));
                                    }
                                    if (i == 5) {
                                        progressive6.setText(dollarValues.get(i));
                                    }
                                    if (i == 6) {
                                        progressive7.setText(dollarValues.get(i));
                                    }
                                    if (i == 7) {
                                        progressive8.setText(dollarValues.get(i));
                                    }
                                    if (i == 8) {
                                        progressive9.setText(dollarValues.get(i));
                                    }
                                    if (i == 9) {
                                        progressive10.setText(dollarValues.get(i));
                                    }
                                }
                                //
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        showToast("Error with cloud OCR");
                                    }
                                });

        //progressDialog.dismiss();
    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

    public void scanProgressives(View view) {
        dispatchTakePictureIntent(REQUEST_TAKE_PHOTO_PROGRESSIVES);
    }

    /*public void scanMachineId(View view) {
        dispatchTakePictureIntent(REQUEST_TAKE_PHOTO_MACHINE_ID);
    }*/

    private void printRect(Rect rect) {
        String left = Integer.toString(rect.left);
        String right = Integer.toString(rect.left);
        String top = Integer.toString(rect.top);
        String bottom = Integer.toString(rect.bottom);
        Log.d("Bounding Box","Left: " + left + ", Right: " + right + ", Top: " + top + ", Bottom: " + bottom);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "tempMICImage";
        File storageDir = getFilesDir();
        //File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,   /* prefix    */
                ".jpg",   /* suffix    */
                storageDir      /* directory */
        );

        // Add .nomedia file to storageDir - this prevents photos being automatically saved to image gallery
        File nomedia = new File(storageDir, ".nomedia");
        if (!nomedia.exists()) {
            boolean created = nomedia.createNewFile();
            /*if (created)
                showToast(".nomedia successfully created");
            else
                showToast("failed to create .nomedia");*/
        }
        //

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private int getNumberOfOccurrences(String input) {
        int ret = 0;
        for (int i = 0; i < input.length(); i++) {
            if (input.charAt(i) == '#') {
                ret++;
            }
        }
        return ret;
    }

    private boolean isAlpha(String name) {
        return name.matches("[a-zA-Z]+");
    }

    private boolean isDigits(String name) {
        return name.matches("[0-9]+");
    }

    private boolean allProgressivesEmpty() {
        return (progressive1.getText().toString().trim().isEmpty()
                && progressive2.getText().toString().trim().isEmpty()
                && progressive3.getText().toString().trim().isEmpty()
                && progressive4.getText().toString().trim().isEmpty()
                && progressive5.getText().toString().trim().isEmpty()
                && progressive6.getText().toString().trim().isEmpty()
                && progressive7.getText().toString().trim().isEmpty()
                && progressive8.getText().toString().trim().isEmpty()
                && progressive9.getText().toString().trim().isEmpty()
                && progressive10.getText().toString().trim().isEmpty());
    }

    public void submitOnClick(View view) {

        try {
            sortProgressives();

            // Get data points
            final String machineIdText = machineId.getText().toString().trim();
            final String progressiveText1 = progressive1.getText().toString().trim();
            final String progressiveText2 = progressive2.getText().toString().trim();
            final String progressiveText3 = progressive3.getText().toString().trim();
            final String progressiveText4 = progressive4.getText().toString().trim();
            final String progressiveText5 = progressive5.getText().toString().trim();
            final String progressiveText6 = progressive6.getText().toString().trim();
            final String progressiveText7 = progressive7.getText().toString().trim();
            final String progressiveText8 = progressive8.getText().toString().trim();
            final String progressiveText9 = progressive9.getText().toString().trim();
            final String progressiveText10 = progressive10.getText().toString().trim();

            //
            String location = getIntent().getStringExtra("location");
            final String locationText = location == null ? "" : location;
            //

            final String emailText = firebaseAuth.getCurrentUser().getEmail().trim();
            final String userId = firebaseAuth.getCurrentUser().getUid().trim();
            final String userName = username;

            // First, make sure machine id isn't blank
            if (machineIdText.isEmpty()) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setMessage("Please add machine ID");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
                return;
            }
            // Make sure at least one progressive has been entered
            if (allProgressivesEmpty()) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setMessage("Please enter at lease one progressive value");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
                return;
            }

            // Make sure a user has been selected
            if (username.equals("No user selected")) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setMessage("Please select a user from the manage users tab");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
                return;
            }
            //

            if (REJECT_DUPLICATES) {
                // Optimize to only search the last n hours
                Date time = new Date(System.currentTimeMillis() - REJECT_DUPLICATES_DURATION_MILLIS);
                Query query = database.collection("users")
                        .document(firebaseAuth.getUid())
                        .collection("scans")
                        .whereEqualTo("machine_id", machineIdText)
                        .whereGreaterThan("timestamp", time)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1);
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();
                            if (documents.size() == 1) {
                                Timestamp timestamp = (Timestamp)documents.get(0).get("timestamp");
                                long delta = Math.abs((timestamp.getSeconds() * 1000) - System.currentTimeMillis());
                                if (delta <= REJECT_DUPLICATES_DURATION_MILLIS) {
                                    //showToast("DUPLICATE");
                                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                                    //alertDialog.setMessage("This machine has already been scanned in the past 24 hours.");
                                    String message = String.format(Locale.US, "This machine has already been scanned in the past %d hour(s).", REJECT_DUPLICATES_DURATION_HOURS);
                                    alertDialog.setMessage(message);
                                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int i) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SUBMIT ANYWAY",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int i) {
                                                    insertToDatabase(emailText, userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, machineIdText, FieldValue.serverTimestamp(), userName, "", locationText);
                                                    resetMachineId();
                                                    resetProgressives();
                                                    showToast("Progressive(s) submitted successfully");
                                                    hideKeyboard();
                                                    dialog.dismiss();
                                                }
                                            });
                                    alertDialog.show();
                                } else {
                                    //showToast("ORIGINAL - 1");
                                    insertToDatabase(emailText, userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, machineIdText, FieldValue.serverTimestamp(), userName, "", locationText);
                                    resetMachineId();
                                    resetProgressives();
                                    showToast("Progressive(s) submitted successfully");
                                    hideKeyboard();
                                }
                            } else {
                                //showToast("ORIGINAL - 2");
                                insertToDatabase(emailText, userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, machineIdText, FieldValue.serverTimestamp(), userName, "", locationText);
                                resetMachineId();
                                resetProgressives();
                                showToast("Progressive(s) submitted successfully");
                                hideKeyboard();
                            }
                        } else {
                            showToast(task.getException().getMessage());
                            //Log.d("DEBUG", task.getException().getMessage());
                        }
                    }
                });
                //
            } else {
                insertToDatabase(emailText, userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, machineIdText, FieldValue.serverTimestamp(), userName, "", locationText);
                resetMachineId();
                resetProgressives();
                showToast("Progressive(s) submitted successfully");
                hideKeyboard();
            }

            // Remove element from uploadArray
            if (intent.hasExtra("hashMap")) {
                HashMap<String, Object> hashMap = (HashMap<String, Object>)intent.getSerializableExtra("hashMap");
                DocumentReference documentReference = database.collection("formUploads").document(userId);
                documentReference.update("uploadArray", FieldValue.arrayRemove(hashMap))
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "DocumentSnapshot successfully updated!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error updating document", e);
                                showToast("Error updating to do list. " + e.getMessage());
                            }
                        });
            }
            //

            // if coming from to do activity - go back and
            if (intent.hasExtra("machine_id")) {
                //super.onBackPressed();
                setResult(RESULT_OK, getIntent());
                this.onBackPressed();
                finish();
            }
            //

            // Check if same machine number has been scanned in last 24 hours
            // maybe compile a collection (set) of all unique machine numbers in the oncreate so this only has to be done once
            // then run cross check based on that and display a popup if necessary.
            /*SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean reject_duplicates = sharedPreferences.getBoolean("reject_duplicates", false);
            int reject_duplicates_duration = sharedPreferences.getInt("reject_duplicates_duration", 0);
            if (reject_duplicates && set.size() == 0) {
                populateDuplicatesSet(reject_duplicates_duration);
            }
            if (reject_duplicates) {
                if (set.contains(machineIdText)) {
                    AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                    alertDialog.setMessage("This machine has already been scanned in the past 24 hours.");
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int i) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SUBMIT ANYWAY",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int i) {

                                    //
                                    insertToDatabase(emailText, userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, machineIdText, FieldValue.serverTimestamp(), userName, "", locationText);
                                    //

                                    resetMachineId();
                                    resetProgressives();
                                    showToast("Progressive(s) submitted successfully");
                                    hideKeyboard();
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                    return;
                }
            }*/
            //

            /*insertToDatabase(emailText, userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, machineIdText, FieldValue.serverTimestamp(), userName, "", locationText);

            // Remove element from uploadArray
            if (intent.hasExtra("hashMap")) {
                HashMap<String, Object> hashMap = (HashMap<String, Object>)intent.getSerializableExtra("hashMap");
                DocumentReference documentReference = database.collection("formUploads").document(userId);
                documentReference.update("uploadArray", FieldValue.arrayRemove(hashMap))
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "DocumentSnapshot successfully updated!");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error updating document", e);
                                showToast("Error updating to do list. " + e.getMessage());
                            }
                        });
            }
            //

            // if coming from to do activity - go back and
            if (intent.hasExtra("machine_id")) {
                //super.onBackPressed();
                setResult(RESULT_OK, getIntent());
                this.onBackPressed();
                finish();
            }*/
            //

            //resetMachineId();
            //resetProgressives();
            //showToast("Progressive(s) submitted successfully");
            //hideKeyboard();
        } catch (Exception ex) {
            showToast(ex.getMessage());
        }
    }

    private void insertToDatabase(String email,
                                  String uid,
                                  String progressive1,
                                  String progressive2,
                                  String progressive3,
                                  String progressive4,
                                  String progressive5,
                                  String progressive6,
                                  String progressive7,
                                  String progressive8,
                                  String progressive9,
                                  String progressive10,
                                  String machine_id,
                                  FieldValue timestamp,
                                  String userName,
                                  String notes,
                                  String location) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("uid", uid);
        user.put("progressive1", progressive1);
        user.put("progressive2", progressive2);
        user.put("progressive3", progressive3);
        user.put("progressive4", progressive4);
        user.put("progressive5", progressive5);
        user.put("progressive6", progressive6);
        user.put("progressive7", progressive7);
        user.put("progressive8", progressive8);
        user.put("progressive9", progressive9);
        user.put("progressive10", progressive10);
        user.put("machine_id", machine_id);
        user.put("timestamp", timestamp);
        user.put("userName", userName);
        user.put("notes", notes);
        user.put("location", location);

        DocumentReference dr = database.collection("scans").document();
        dr.set(user);
        String docId = dr.getId();

        // New collection
        user.remove("uid");
        user.remove("email");
        DocumentReference dr2 = database.collection("users").document(uid).collection("scans").document(docId);
        dr2.set(user);
    }

    public void hideKeyboard() {
        try {
            LinearLayout layout = findViewById(R.id.mainLinearLayout);
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void startVoiceInput(int progressive) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        if (progressive == 0)
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What is the slot machine ID number?");
        else
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What is the current progressive value?");

        intent.putExtra("progressive", progressive);
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            showToast("Google and Carrier Services required for voice recognition");
        }
    }

    private String formatVoiceToSpeech(String text, boolean isProgressive) {
        text = text.toLowerCase();
        String original = text;
        StringBuilder newText = new StringBuilder();
        String[] words = text.split("\\s");
        for (String word : words) {
            newText.append(convertString(word));
        }
        text = newText.toString();

        text = text.replaceAll("\\s",""); // Removes all spaces
        text = text.replaceAll("[^0-9]",""); // Removes all but digits
        if (original.contains("progressive") || isProgressive) {
            if (text.length() >= 3) {
                return text.substring(0, text.length() - 2) + "." + text.substring(text.length() - 2);
            }
        } else if (original.contains("machine")) {
            return text;
        }
        return text;
    }

    private String convertString(String number) {
        if (number.trim().equals("one")) {
            return "1";
        } else if (number.trim().equals("two")) {
            return "2";
        } else if (number.trim().equals("three")) {
            return "3";
        } else if (number.trim().equals("four")) {
            return "4";
        } else if (number.trim().equals("five")) {
            return "5";
        } else if (number.trim().equals("six")) {
            return "6";
        } else if (number.trim().equals("seven")) {
            return "7";
        } else if (number.trim().equals("eight")) {
            return "8";
        } else if (number.trim().equals("nine")) {
            return "9";
        }
        return number.trim(); // return original string if no match
    }

    private class GenericTextWatcher implements TextWatcher {
        private TextInputEditText textInputEditText;
        private GenericTextWatcher(TextInputEditText textInputEditText) {
            this.textInputEditText = textInputEditText;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
        public void afterTextChanged(Editable editable) {}

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            if (textInputEditText.getText().toString().equals("")) {
                textInputEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_mic, 0);
            } else {
                textInputEditText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_cancel_black_24dp, 0);
            }
        }
    }

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public void sortProgressives() {

        final List<Double> values = new ArrayList<Double>();

        if (isDouble(progressive1.getText().toString())) {
            values.add(Double.parseDouble(progressive1.getText().toString()));
        }
        if (isDouble(progressive2.getText().toString())) {
            values.add(Double.parseDouble(progressive2.getText().toString()));
        }
        if (isDouble(progressive3.getText().toString())) {
            values.add(Double.parseDouble(progressive3.getText().toString()));
        }
        if (isDouble(progressive4.getText().toString())) {
            values.add(Double.parseDouble(progressive4.getText().toString()));
        }
        if (isDouble(progressive5.getText().toString())) {
            values.add(Double.parseDouble(progressive5.getText().toString()));
        }
        if (isDouble(progressive6.getText().toString())) {
            values.add(Double.parseDouble(progressive6.getText().toString()));
        }
        if (isDouble(progressive7.getText().toString())) {
            values.add(Double.parseDouble(progressive7.getText().toString()));
        }
        if (isDouble(progressive8.getText().toString())) {
            values.add(Double.parseDouble(progressive8.getText().toString()));
        }
        if (isDouble(progressive9.getText().toString())) {
            values.add(Double.parseDouble(progressive9.getText().toString()));
        }
        if (isDouble(progressive10.getText().toString())) {
            values.add(Double.parseDouble(progressive10.getText().toString()));
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean sort = sharedPreferences.getBoolean("auto_sort_progressives_preference", true);

        if (sort) {
            Collections.sort(values);
            Collections.reverse(values);
        }
        DecimalFormat df = new DecimalFormat("0.00");

        resetProgressives();

        for (int i = 0; i < values.size(); i++) {
            if (i == 0) {
                progressive1.setText(df.format(values.get(i)));
            } else if (i == 1) {
                progressive2.setText(df.format(values.get(i)));
            } else if (i == 2) {
                progressive3.setText(df.format(values.get(i)));
            } else if (i == 3) {
                progressive4.setText(df.format(values.get(i)));
            } else if (i == 4) {
                progressive5.setText(df.format(values.get(i)));
            } else if (i == 5) {
                progressive6.setText(df.format(values.get(i)));
            } else if (i == 6) {
                progressive7.setText(df.format(values.get(i)));
            } else if (i == 7) {
                progressive8.setText(df.format(values.get(i)));
            } else if (i == 8) {
                progressive9.setText(df.format(values.get(i)));
            } else if (i == 9) {
                progressive10.setText(df.format(values.get(i)));
            }
        }

        /*if (NUMBER_OF_PROGRESSIVES == 6) {
            progressive7.setText("");
            progressive8.setText("");
            progressive9.setText("");
            progressive10.setText("");
        } else if (NUMBER_OF_PROGRESSIVES == 7) {
            progressive8.setText("");
            progressive9.setText("");
            progressive10.setText("");
        } else if (NUMBER_OF_PROGRESSIVES == 8) {
            progressive9.setText("");
            progressive10.setText("");
        } else if (NUMBER_OF_PROGRESSIVES == 9) {
            progressive10.setText("");
        }*/
    }
}
