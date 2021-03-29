package com.slotmachine.ocr.mic;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import android.preference.PreferenceManager;
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

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.BadgeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Task;
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

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DraggableRecyclerAdapter.StartDragListener {

    public static final int REQUEST_TAKE_PHOTO_PROGRESSIVES = 0;
    public static final int MY_PERMISSIONS_REQUEST_CODE = 1;
    public static final int REQUEST_TAKE_PHOTO_MACHINE_ID = 2;
    public static final int REQUEST_SETTINGS_ACTIVITY = 3;
    public static final int REQUEST_DATA_REPORT_ACTIVITY = 4;
    private static final int REQ_CODE_SPEECH_INPUT = 100;

    public String mCurrentPhotoPath;
    public int progressive = 1;

    private String username;
    private String notes;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private Double minimumProgressiveValue;

    private List<String> progressiveDescriptions = null;

    private Intent intent;
    private Toolbar toolbar;

    private int REJECT_DUPLICATES_DURATION_MILLIS, REJECT_DUPLICATES_DURATION_HOURS;
    private boolean REJECT_DUPLICATES;

    private boolean DEBUG = false; // When debug is enabled a password isn't necessary to enter settings

    ItemTouchHelper touchHelper;
    DraggableRecyclerAdapter adapter;
    BadgeDrawable badge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        username = sharedPref.getString("username", "No user selected");

        setTitle("User: " + username);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        REJECT_DUPLICATES = sharedPreferences.getBoolean("reject_duplicates", false);
        REJECT_DUPLICATES_DURATION_MILLIS = sharedPreferences.getInt("reject_duplicates_duration", 0) * 3600 * 1000;
        REJECT_DUPLICATES_DURATION_HOURS = sharedPreferences.getInt("reject_duplicates_duration", 0);

        database = FirebaseFirestore.getInstance();
        checkPermissions();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        headerView.setBackgroundColor(Color.parseColor("#2196F3"));

        resetProgressives();

        intent = getIntent();
        // Also check if coming from login activity and send verification email if necessary
        boolean comingFromLogin = intent.getBooleanExtra("comingFromLogin", false);
        if (comingFromLogin) {
            showDialogForAdminAccount();
        }
        //

        // Set minimum progressive value
        String val = sharedPreferences.getString("minimum_value", "0");
        try {
            minimumProgressiveValue = Double.valueOf(val);
        } catch (Exception ex) {
            minimumProgressiveValue = 0.0;
        }

        progressiveDescriptions = intent.getStringArrayListExtra("progressiveDescriptionTitles");
        RecyclerView recyclerView = findViewById(R.id.drag_recycler);
        adapter = new DraggableRecyclerAdapter(6, progressiveDescriptions,this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper.Callback callback = new ItemMoveCallback(adapter);
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);

        String machine_id = intent.getStringExtra("machine_id");
        if (machine_id != null) {
            adapter.setMachineId(machine_id);
        }
    }

    @Override
    public void onVoiceRequest(int code) {
        startVoiceInput(code);
    }

    @Override
    public void onSubmitButtonClick() {
        dispatchTakePictureIntent(REQUEST_TAKE_PHOTO_PROGRESSIVES);
    }

    @Override
    public void onSubmitScan() {
        if (allProgressivesEmpty()) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setMessage("Are you sure you want to enter all blanks for this machine?");
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Submit",
                    (dialog, i) -> {
                        dialog.dismiss();
                        submitOnClick();
                    });
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel",
                    (dialog, i) -> dialog.dismiss());
            alertDialog.show();
        } else {
            submitOnClick();
        }
    }

    @Override
    public void requestDrag(RecyclerView.ViewHolder viewHolder) {
        touchHelper.startDrag(viewHolder);
    }


    private void showDialogForAdminAccount() {
        // Check if user is an admin account (if email address is a document id in admins collection)
        // If so, present a dialog box with only the option to sign out and no option to dismiss
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Admin Account");
        String message = "You are attempting to sign in with an admin account.  Please sign out and back in with a casino level account.";
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Sign Out",
                (dialog, i) -> {
                    dialog.dismiss();
                    startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                });
        alertDialog.setCancelable(false);
        alertDialog.setCanceledOnTouchOutside(false);
        DocumentReference docRef = database.collection("admins").document(firebaseAuth.getCurrentUser().getEmail());
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    alertDialog.show();
                    AuthUI.getInstance().signOut(getApplicationContext());
                }
            }
        });
    }

    private void checkPermissions() {
        this.requestPermissions(new String[] {Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.INTERNET},
                MY_PERMISSIONS_REQUEST_CODE);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        badge = BadgeDrawable.create(this);
        badge.setVisible(false);
        BadgeUtils.attachBadgeDrawable(badge, toolbar, R.id.item_add_notes);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.item_add_notes) {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            View mView = getLayoutInflater().inflate(R.layout.dialog_edittext_layout, null);
            TextInputEditText notesEditText = mView.findViewById(R.id.notes_edit_text);
            notesEditText.setText(notes);
            dialog.setTitle("Notes")
                .setView(mView)
                .setPositiveButton("Save", (dialogInterface, i) -> {
                    notes = notesEditText.getText().toString();
                    badge.setVisible(!notes.trim().isEmpty());
                })
                .setNegativeButton("Cancel", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })
                .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
                        (dialog, i) -> {
                            String password = passwordEditText.getText().toString().trim();
                            if (password.equals("")) {
                                return;
                            }
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
                            user.reauthenticate(credential)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            final Intent intent1;
                                            intent1 = new Intent(MainActivity.this, SettingsActivity.class);
                                            startActivity(intent1);
                                        } else {
                                            showToast("Incorrect password");
                                        }
                                    });
                            dialog.dismiss();
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                        (dialog, i) -> dialog.dismiss());
                alertDialog.show();
            }
        } else if (id == R.id.nav_sign_out) {
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(task -> {
                        startActivity(new Intent(MainActivity.this, LoginActivity.class));
                        finish();
                    });
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
                                file.delete();
                            }
                        } else {
                            showToast("Bitmap is null");
                        }
                    }
                    break;
                }
//                case REQ_CODE_SPEECH_INPUT: {
//                    if (resultCode == RESULT_OK && null != intent) {
//                        ArrayList<String> result = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//                        if (progressive == 1) {
//                            progressive1.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 2) {
//                            progressive2.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 3) {
//                            progressive3.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 4) {
//                            progressive4.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 5) {
//                            progressive5.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 6) {
//                            progressive6.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 7) {
//                            progressive7.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 8) {
//                            progressive8.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 9) {
//                            progressive9.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 10) {
//                            progressive10.setText(formatVoiceToSpeech(result.get(0), true));
//                        } else if (progressive == 0) {
//                            machineId.setText(formatVoiceToSpeech(result.get(0), false));
//                        }
//                    }
//                    break;
//                }
                case REQUEST_SETTINGS_ACTIVITY: {
                    break;
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            showToast("Error");
        }
    }

    private void resetMachineId() {
//        machineId.setText("");
//        machineId.clearFocus();
    }

    private void resetNotes() {
//        notesEditText.setText("");
//        notesEditText.clearFocus();
    }

    private void resetProgressives() {
//        progressive1.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive2.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive3.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive4.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive5.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive6.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive7.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive8.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive9.setText(EMPTY_PROGRESSIVE_VALUE);
//        progressive10.setText(EMPTY_PROGRESSIVE_VALUE);
//
//        progressive1.clearFocus();
//        progressive2.clearFocus();
//        progressive3.clearFocus();
//        progressive4.clearFocus();
//        progressive5.clearFocus();
//        progressive6.clearFocus();
//        progressive7.clearFocus();
//        progressive8.clearFocus();
//        progressive9.clearFocus();
//        progressive10.clearFocus();
    }

    private void processProgressivesOCR(Bitmap bitmap) {

        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance().getCloudDocumentTextRecognizer();
        Task<FirebaseVisionDocumentText> result =
                detector.processImage(image)
                        .addOnSuccessListener(firebaseVisionDocumentText -> {
                            if (firebaseVisionDocumentText == null) {
                                showToast("No text detected.  Try again.  ");
                                return;
                            }
                            StringBuilder sb = new StringBuilder();
                            List<FirebaseVisionDocumentText.Word> filteredWords = new ArrayList<FirebaseVisionDocumentText.Word>();
                            List<Rect> wordDimensions = new ArrayList<Rect>();
                            //String machineCode = machineId.getText().toString();
                            String machineCode = "1234";
                            List<FirebaseVisionDocumentText.Block> blocks = firebaseVisionDocumentText.getBlocks();
                            if (blocks.size() == 0) {
                                showToast("No text detected. Try again. ");
                                return;
                            }
                            for (FirebaseVisionDocumentText.Block block : blocks) {
                                List<FirebaseVisionDocumentText.Paragraph> paragraphs = block.getParagraphs();
                                for (FirebaseVisionDocumentText.Paragraph paragraph : paragraphs) {
                                    List<FirebaseVisionDocumentText.Word> words = paragraph.getWords();
                                    if (getNumberOfOccurrences(paragraph.getText()) == 2) {
                                        int firstIndex = paragraph.getText().indexOf('#');
                                        int secondIndex = paragraph.getText().indexOf('#', firstIndex + 1);
                                        machineCode = paragraph.getText().substring(firstIndex+1, secondIndex).trim();
                                    }
                                    for (FirebaseVisionDocumentText.Word word : words) {
                                        if (!isAlpha(word.getText())) {
                                            sb.append(word.getText().trim());
                                            filteredWords.add(word);
                                            wordDimensions.add(word.getBoundingBox());
                                            //printRect(word.getBoundingBox());
                                        }
                                    }
                                }
                            }

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

                            adapter.setItems(dollarValues);
                            // Add to TextViews
//                                machineId.setText(machineCode);
//
//                                for (int i = 0; i < dollarValues.size(); i++) {
//                                    if (i == 0) {
//                                        progressive1.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 1) {
//                                        progressive2.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 2) {
//                                        progressive3.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 3) {
//                                        progressive4.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 4) {
//                                        progressive5.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 5) {
//                                        progressive6.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 6) {
//                                        progressive7.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 7) {
//                                        progressive8.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 8) {
//                                        progressive9.setText(dollarValues.get(i));
//                                    }
//                                    if (i == 9) {
//                                        progressive10.setText(dollarValues.get(i));
//                                    }
//                                }
                            //
                        })
                        .addOnFailureListener(e -> showToast("Error with cloud OCR"));

    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

//    public void scanProgressives(View view) {
//        dispatchTakePictureIntent(REQUEST_TAKE_PHOTO_PROGRESSIVES);
//    }

//    private void printRect(Rect rect) {
//        String left = Integer.toString(rect.left);
//        String right = Integer.toString(rect.left);
//        String top = Integer.toString(rect.top);
//        String bottom = Integer.toString(rect.bottom);
//        Log.d("Bounding Box","Left: " + left + ", Right: " + right + ", Top: " + top + ", Bottom: " + bottom);
//    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "tempMICImage";
        File storageDir = getFilesDir();
        File image = File.createTempFile(
                imageFileName,   /* prefix    */
                ".jpg",   /* suffix    */
                storageDir      /* directory */
        );

        // Add .nomedia file to storageDir - this prevents photos being automatically saved to image gallery
        File nomedia = new File(storageDir, ".nomedia");
        if (!nomedia.exists()) {
            nomedia.createNewFile();
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
        for (int i = 1; i < adapter.getData().size(); i++) {
            if (!adapter.getData().get(i).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void submitOnClick() {

        try {
            //sortProgressives();

            List<String> data = adapter.getData();
            int len = data.size();
            if (len < 11) {
                for (int i = 0; i < 11 - len; i++) {
                    data.add("");
                }
            }

            final String machineIdText = data.get(0);
            final String progressiveText1 = data.get(1);
            final String progressiveText2 = data.get(2);
            final String progressiveText3 = data.get(3);
            final String progressiveText4 = data.get(4);
            final String progressiveText5 = data.get(5);
            final String progressiveText6 = data.get(6);
            final String progressiveText7 = data.get(7);
            final String progressiveText8 = data.get(8);
            final String progressiveText9 = data.get(9);
            final String progressiveText10 = data.get(10);
            final String notesText = notes;

            //
            String location = getIntent().getStringExtra("location");
            final String locationText = location == null ? "" : location;
            //

            // Descriptions
            final ArrayList<String> descriptionValuesArray = new ArrayList<>();
            for (int i = 0; i < 10; i++) { // Populate with 10 null entries
                descriptionValuesArray.add(null);
            }
            if (getIntent().hasExtra("progressiveDescriptionTitles")) {
                ArrayList<String> descriptions = getIntent().getStringArrayListExtra("progressiveDescriptionTitles");
                for (int i = 0; i < descriptions.size(); i++) {
                    if (descriptions.get(i) != null && !descriptions.get(i).isEmpty()) {
                        descriptionValuesArray.add(i, descriptions.get(i));
                    }
                }
            }
            // Bases
            final ArrayList<String> baseValuesArray = new ArrayList<>();
            for (int i = 0; i < 10; i++) { // Populate with 10 null entries
                baseValuesArray.add(null);
            }
            if (getIntent().hasExtra("baseValuesArray")) {
                ArrayList<String> bases = getIntent().getStringArrayListExtra("baseValuesArray");
                for (int i = 0; i < bases.size(); i++) {
                    if (bases.get(i) != null && !bases.get(i).isEmpty()) {
                        baseValuesArray.add(i, bases.get(i));
                    }
                }
            }
            // Increments
            final ArrayList<String> incrementValuesArray = new ArrayList<>();
            for (int i = 0; i < 10; i++) { // Populate with 10 null entries
                incrementValuesArray.add(null);
            }
            if (getIntent().hasExtra("incrementValuesArray")) {
                ArrayList<String> increments = getIntent().getStringArrayListExtra("incrementValuesArray");
                for (int i = 0; i < increments.size(); i++) {
                    if (increments.get(i) != null && !increments.get(i).isEmpty()) {
                        incrementValuesArray.add(i, increments.get(i));
                    }
                }
            }
            //

            final String userId = firebaseAuth.getCurrentUser().getUid().trim();
            final String userName = username;

            // First, make sure machine id isn't blank
            if (machineIdText.isEmpty()) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setMessage("Please add machine ID");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        (dialog, i) -> dialog.dismiss());
                alertDialog.show();
                return;
            }

            // Make sure a user has been selected
            if (username.equals("No user selected")) {
                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setMessage("Please select a user from the manage users tab");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                        (dialog, i) -> dialog.dismiss());
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
                query.get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        if (documents.size() == 1) {
                            Timestamp timestamp = (Timestamp)documents.get(0).get("timestamp");
                            long delta = Math.abs((timestamp.getSeconds() * 1000) - System.currentTimeMillis());
                            if (delta <= REJECT_DUPLICATES_DURATION_MILLIS) {
                                AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                                String message = String.format(Locale.US, "This machine has already been scanned in the past %d hour(s).", REJECT_DUPLICATES_DURATION_HOURS);
                                alertDialog.setMessage(message);
                                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                                        (dialog, i) -> dialog.dismiss());
                                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SUBMIT ANYWAY",
                                        (dialog, i) -> {
                                            insertToDatabase(userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, descriptionValuesArray.get(0), descriptionValuesArray.get(1), descriptionValuesArray.get(2), descriptionValuesArray.get(3), descriptionValuesArray.get(4), descriptionValuesArray.get(5), descriptionValuesArray.get(6), descriptionValuesArray.get(7), descriptionValuesArray.get(8), descriptionValuesArray.get(9), baseValuesArray.get(0), baseValuesArray.get(1), baseValuesArray.get(2), baseValuesArray.get(3), baseValuesArray.get(4), baseValuesArray.get(5), baseValuesArray.get(6), baseValuesArray.get(7), baseValuesArray.get(8), baseValuesArray.get(9), incrementValuesArray.get(0), incrementValuesArray.get(1), incrementValuesArray.get(2), incrementValuesArray.get(3), incrementValuesArray.get(4), incrementValuesArray.get(5), incrementValuesArray.get(6), incrementValuesArray.get(7), incrementValuesArray.get(8), incrementValuesArray.get(9), machineIdText, FieldValue.serverTimestamp(), userName, notesText, locationText);
                                            resetMachineId();
                                            resetProgressives();
                                            resetNotes();
                                            showToast("Progressive(s) submitted successfully");
                                            hideKeyboard();
                                            dialog.dismiss();
                                        });
                                alertDialog.show();
                            } else {
                                insertToDatabase(userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, descriptionValuesArray.get(0), descriptionValuesArray.get(1), descriptionValuesArray.get(2), descriptionValuesArray.get(3), descriptionValuesArray.get(4), descriptionValuesArray.get(5), descriptionValuesArray.get(6), descriptionValuesArray.get(7), descriptionValuesArray.get(8), descriptionValuesArray.get(9), baseValuesArray.get(0), baseValuesArray.get(1), baseValuesArray.get(2), baseValuesArray.get(3), baseValuesArray.get(4), baseValuesArray.get(5), baseValuesArray.get(6), baseValuesArray.get(7), baseValuesArray.get(8), baseValuesArray.get(9), incrementValuesArray.get(0), incrementValuesArray.get(1), incrementValuesArray.get(2), incrementValuesArray.get(3), incrementValuesArray.get(4), incrementValuesArray.get(5), incrementValuesArray.get(6), incrementValuesArray.get(7), incrementValuesArray.get(8), incrementValuesArray.get(9), machineIdText, FieldValue.serverTimestamp(), userName, notesText, locationText);
                                resetMachineId();
                                resetProgressives();
                                resetNotes();
                                showToast("Progressive(s) submitted successfully");
                                hideKeyboard();
                            }
                        } else {
                            insertToDatabase(userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, descriptionValuesArray.get(0), descriptionValuesArray.get(1), descriptionValuesArray.get(2), descriptionValuesArray.get(3), descriptionValuesArray.get(4), descriptionValuesArray.get(5), descriptionValuesArray.get(6), descriptionValuesArray.get(7), descriptionValuesArray.get(8), descriptionValuesArray.get(9), baseValuesArray.get(0), baseValuesArray.get(1), baseValuesArray.get(2), baseValuesArray.get(3), baseValuesArray.get(4), baseValuesArray.get(5), baseValuesArray.get(6), baseValuesArray.get(7), baseValuesArray.get(8), baseValuesArray.get(9), incrementValuesArray.get(0), incrementValuesArray.get(1), incrementValuesArray.get(2), incrementValuesArray.get(3), incrementValuesArray.get(4), incrementValuesArray.get(5), incrementValuesArray.get(6), incrementValuesArray.get(7), incrementValuesArray.get(8), incrementValuesArray.get(9),  machineIdText, FieldValue.serverTimestamp(), userName, notesText, locationText);
                            resetMachineId();
                            resetProgressives();
                            resetNotes();
                            showToast("Progressive(s) submitted successfully");
                            hideKeyboard();
                        }
                    } else {
                        showToast(task.getException().getMessage());
                    }
                });
                //
            } else {
                insertToDatabase(userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, descriptionValuesArray.get(0), descriptionValuesArray.get(1), descriptionValuesArray.get(2), descriptionValuesArray.get(3), descriptionValuesArray.get(4), descriptionValuesArray.get(5), descriptionValuesArray.get(6), descriptionValuesArray.get(7), descriptionValuesArray.get(8), descriptionValuesArray.get(9), baseValuesArray.get(0), baseValuesArray.get(1), baseValuesArray.get(2), baseValuesArray.get(3), baseValuesArray.get(4), baseValuesArray.get(5), baseValuesArray.get(6), baseValuesArray.get(7), baseValuesArray.get(8), baseValuesArray.get(9), incrementValuesArray.get(0), incrementValuesArray.get(1), incrementValuesArray.get(2), incrementValuesArray.get(3), incrementValuesArray.get(4), incrementValuesArray.get(5), incrementValuesArray.get(6), incrementValuesArray.get(7), incrementValuesArray.get(8), incrementValuesArray.get(9),  machineIdText, FieldValue.serverTimestamp(), userName, notesText, locationText);
                resetMachineId();
                resetProgressives();
                resetNotes();
                showToast("Progressive(s) submitted successfully");
                hideKeyboard();
            }

            // Reset Notes
            notes = "";

            // Remove element from uploadArray
            if (intent.hasExtra("hashMap")) {
                HashMap<String, Object> hashMap = (HashMap<String, Object>)intent.getSerializableExtra("hashMap");
                DocumentReference documentReference = database.collection("formUploads").document(userId);
                documentReference.update("uploadArray", FieldValue.arrayRemove(hashMap))
                        .addOnSuccessListener(aVoid -> Timber.d("DocumentSnapshot successfully updated!"))
                        .addOnFailureListener(e -> showToast("Error updating to do list. " + e.getMessage()));
            }
            //

            // if coming from to do activity - go back and
            if (intent.hasExtra("machine_id")) {
                setResult(RESULT_OK, getIntent());
                this.onBackPressed();
                finish();
            }
            //
        } catch (Exception ex) {
            Timber.e(ex);
            showToast(ex.getMessage());
        }
    }

    private void insertToDatabase(String uid,
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
                                  String description1,
                                  String description2,
                                  String description3,
                                  String description4,
                                  String description5,
                                  String description6,
                                  String description7,
                                  String description8,
                                  String description9,
                                  String description10,
                                  String base1,
                                  String base2,
                                  String base3,
                                  String base4,
                                  String base5,
                                  String base6,
                                  String base7,
                                  String base8,
                                  String base9,
                                  String base10,
                                  String increment1,
                                  String increment2,
                                  String increment3,
                                  String increment4,
                                  String increment5,
                                  String increment6,
                                  String increment7,
                                  String increment8,
                                  String increment9,
                                  String increment10,
                                  String machine_id,
                                  FieldValue timestamp,
                                  String userName,
                                  String notes,
                                  String location) {
        Map<String, Object> user = new HashMap<>();
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
        //
        List<String> descriptions = new ArrayList<>();
        descriptions.add(description1);
        descriptions.add(description2);
        descriptions.add(description3);
        descriptions.add(description4);
        descriptions.add(description5);
        descriptions.add(description6);
        descriptions.add(description7);
        descriptions.add(description8);
        descriptions.add(description9);
        descriptions.add(description10);
        user.put("descriptions", descriptions);
        //
        if (base1 != null) { user.put("base1", base1); }
        if (base2 != null) { user.put("base2", base2); }
        if (base3 != null) { user.put("base3", base3); }
        if (base4 != null) { user.put("base4", base4); }
        if (base5 != null) { user.put("base5", base5); }
        if (base6 != null) { user.put("base6", base6); }
        if (base7 != null) { user.put("base7", base7); }
        if (base8 != null) { user.put("base8", base8); }
        if (base9 != null) { user.put("base9", base9); }
        if (base10 != null) { user.put("base10", base10); }
        //
        if (increment1 != null) { user.put("increment1", increment1); }
        if (increment2 != null) { user.put("increment2", increment2); }
        if (increment3 != null) { user.put("increment3", increment3); }
        if (increment4 != null) { user.put("increment4", increment4); }
        if (increment5 != null) { user.put("increment5", increment5); }
        if (increment6 != null) { user.put("increment6", increment6); }
        if (increment7 != null) { user.put("increment7", increment7); }
        if (increment8 != null) { user.put("increment8", increment8); }
        if (increment9 != null) { user.put("increment9", increment9); }
        if (increment10 != null) { user.put("increment10", increment10); }
        user.put("machine_id", machine_id);
        user.put("timestamp", timestamp);
        user.put("userName", userName);
        user.put("notes", notes);
        user.put("location", location);

        database.collection("users").document(uid).collection("scans").document().set(user);
    }

    public void hideKeyboard() {
        try {
            LinearLayout layout = findViewById(R.id.coordinator_layout);
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
            showToast("Voice recognition not supported on your device");
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

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

//    public void sortProgressives() {
//
//        final List<Double> values = new ArrayList<>();
//
//        if (isDouble(progressive1.getText().toString())) {
//            values.add(Double.parseDouble(progressive1.getText().toString()));
//        }
//        if (isDouble(progressive2.getText().toString())) {
//            values.add(Double.parseDouble(progressive2.getText().toString()));
//        }
//        if (isDouble(progressive3.getText().toString())) {
//            values.add(Double.parseDouble(progressive3.getText().toString()));
//        }
//        if (isDouble(progressive4.getText().toString())) {
//            values.add(Double.parseDouble(progressive4.getText().toString()));
//        }
//        if (isDouble(progressive5.getText().toString())) {
//            values.add(Double.parseDouble(progressive5.getText().toString()));
//        }
//        if (isDouble(progressive6.getText().toString())) {
//            values.add(Double.parseDouble(progressive6.getText().toString()));
//        }
//        if (isDouble(progressive7.getText().toString())) {
//            values.add(Double.parseDouble(progressive7.getText().toString()));
//        }
//        if (isDouble(progressive8.getText().toString())) {
//            values.add(Double.parseDouble(progressive8.getText().toString()));
//        }
//        if (isDouble(progressive9.getText().toString())) {
//            values.add(Double.parseDouble(progressive9.getText().toString()));
//        }
//        if (isDouble(progressive10.getText().toString())) {
//            values.add(Double.parseDouble(progressive10.getText().toString()));
//        }
//
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean sort = sharedPreferences.getBoolean("auto_sort_progressives_preference", true);
//
//        if (sort) {
//            Collections.sort(values);
//            Collections.reverse(values);
//        }
//        DecimalFormat df = new DecimalFormat("0.00");
//
//        resetProgressives();
//
//        for (int i = 0; i < values.size(); i++) {
//            if (i == 0) {
//                progressive1.setText(df.format(values.get(i)));
//            } else if (i == 1) {
//                progressive2.setText(df.format(values.get(i)));
//            } else if (i == 2) {
//                progressive3.setText(df.format(values.get(i)));
//            } else if (i == 3) {
//                progressive4.setText(df.format(values.get(i)));
//            } else if (i == 4) {
//                progressive5.setText(df.format(values.get(i)));
//            } else if (i == 5) {
//                progressive6.setText(df.format(values.get(i)));
//            } else if (i == 6) {
//                progressive7.setText(df.format(values.get(i)));
//            } else if (i == 7) {
//                progressive8.setText(df.format(values.get(i)));
//            } else if (i == 8) {
//                progressive9.setText(df.format(values.get(i)));
//            } else if (i == 9) {
//                progressive10.setText(df.format(values.get(i)));
//            }
//        }
//    }
}
