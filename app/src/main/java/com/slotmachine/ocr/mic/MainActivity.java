package com.slotmachine.ocr.mic;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
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

import com.google.android.gms.tasks.OnCompleteListener;
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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.slotmachine.ocr.mic.viewmodel.MainActivityViewModel;
import com.slotmachine.ocr.mic.model.ToDoListItem;
//import com.slotmachine.ocr.mic.viewmodel.MainActivityViewModel;

import android.speech.RecognizerIntent;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, DraggableRecyclerAdapter.StartDragListener {

    private static final int REQUEST_TAKE_PHOTO_PROGRESSIVES = 100;
    private static final int MY_PERMISSIONS_REQUEST_CODE = 200;
    private static final int REQUEST_SETTINGS_ACTIVITY = 300;
    private static final int REQ_CODE_SPEECH_INPUT = 400;

    public String mCurrentPhotoPath;
    public int progressive = 1;

    private String username;
    private String notes;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private MainActivityViewModel mainActivityViewModel;

    private Double minimumProgressiveValue;

    private List<String> progressiveDescriptions = null;

    private Intent intent;
    private Toolbar toolbar;
    private ProgressDialog progressDialog;

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

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Checking for potential duplicates...");

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

        // Buttons
        findViewById(R.id.button_scan).setOnClickListener(v -> onScan());
        findViewById(R.id.button_submit).setOnClickListener(v -> onSubmit());
        //

        ToDoListItem item = (ToDoListItem) intent.getSerializableExtra("toDoItem");
        String machineId = null;
        if (item != null) {
            machineId = item.getMachineId();
            progressiveDescriptions = item.getDescriptions();
        }

//        progressiveDescriptions = intent.getStringArrayListExtra("progressiveDescriptionTitles");
        RecyclerView recyclerView = findViewById(R.id.drag_recycler);
        adapter = new DraggableRecyclerAdapter(machineId == null, this, UserSettings.getNumberOfProgressives(this), progressiveDescriptions,this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ItemTouchHelper.Callback callback = new ItemMoveCallback(adapter);
        touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(adapter);

        mainActivityViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);


        if (machineId != null) {
            adapter.setMachineId(machineId);
            String hint = sharedPref.getString("progressive_hint_text_from_todo", "description");
            if (hint.equals("previous")) {
                initPrevScanObserver(machineId);
            }
        }
    }

    private void initPrevScanObserver(String machine_id) {
        mainActivityViewModel.getPrevDayValues(firebaseAuth.getUid(), machine_id).observe(this, values -> adapter.setPrevItems(values));
    }

    @Override
    public void onVoiceRequest(int code) {
        progressive = code;
        startVoiceInput(code);
    }

    public void onScan() {
        dispatchTakePictureIntent();
    }

//    public void onSubmit() {
////        Intent i = new Intent();
////        i.putExtra("val", 5);
//        setResult(RESULT_OK, getIntent());
//
//        finish();
//    }
    public void onSubmit() {
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

            try {
                View v = getCurrentFocus();
                if (v instanceof EditText) {
                    v.clearFocus();
                }
            } catch (Exception ex) {
                Timber.e("error clearing focus, %s", ex.getMessage());
            }
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

    private void dispatchTakePictureIntent() {
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
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO_PROGRESSIVES);
            }
        } else {
            showToast("Unable to open camera");
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
                case REQ_CODE_SPEECH_INPUT: {
                    if (resultCode == RESULT_OK && null != intent) {
                        ArrayList<String> result = intent.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        adapter.setItem(progressive, formatVoiceToSpeech(result.get(0), progressive != 0));
                    }
                    break;
                }
                case REQUEST_SETTINGS_ACTIVITY: {
                    break;
                }
            }
        } catch (Exception error) {
            Timber.e(error);
            showToast("Error");
        }
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
                            //String machineCode = "1234";
                            List<FirebaseVisionDocumentText.Block> blocks = firebaseVisionDocumentText.getBlocks();
                            if (blocks.size() == 0) {
                                showToast("No text detected. Try again. ");
                                return;
                            }
                            for (FirebaseVisionDocumentText.Block block : blocks) {
                                List<FirebaseVisionDocumentText.Paragraph> paragraphs = block.getParagraphs();
                                for (FirebaseVisionDocumentText.Paragraph paragraph : paragraphs) {
                                    List<FirebaseVisionDocumentText.Word> words = paragraph.getWords();
                                    //if (getNumberOfOccurrences(paragraph.getText()) == 2) {
                                        //int firstIndex = paragraph.getText().indexOf('#');
                                        //int secondIndex = paragraph.getText().indexOf('#', firstIndex + 1);
                                        //machineCode = paragraph.getText().substring(firstIndex+1, secondIndex).trim();
                                    //}
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
                        })
                        .addOnFailureListener(e -> showToast("Error with cloud OCR"));

    }

    private static int exifToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) { return 90; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {  return 180; }
        else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {  return 270; }
        return 0;
    }

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
            List<String> data = adapter.getData();
            int len = data.size();
            if (len < 11) {
                for (int i = 0; i < 11 - len; i++) {
                    data.add("");
                }
            }

            List<String> formatted = sortProgressives(data.get(1),
                    data.get(2),
                    data.get(3),
                    data.get(4),
                    data.get(5),
                    data.get(6),
                    data.get(7),
                    data.get(8),
                    data.get(9),
                    data.get(10));

            final String machineIdText = data.get(0);
            final String progressiveText1 = formatted.get(0);
            final String progressiveText2 = formatted.get(1);
            final String progressiveText3 = formatted.get(2);
            final String progressiveText4 = formatted.get(3);
            final String progressiveText5 = formatted.get(4);
            final String progressiveText6 = formatted.get(5);
            final String progressiveText7 = formatted.get(6);
            final String progressiveText8 = formatted.get(7);
            final String progressiveText9 = formatted.get(8);
            final String progressiveText10 = formatted.get(9);
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

            //
            if (REJECT_DUPLICATES) { // Check if possible duplicate
                progressDialog.show();
                Date time = new Date(System.currentTimeMillis() - REJECT_DUPLICATES_DURATION_MILLIS);
                Query query = database.collection("users")
                        .document(firebaseAuth.getUid())
                        .collection("scans")
                        .whereEqualTo("machine_id", machineIdText)
                        .whereGreaterThan("timestamp", time)
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(1);
                query.get().addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> documents = task.getResult().getDocuments();
                        if (documents.size() == 1) {
                            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                            String message = String.format(Locale.US, "This machine has already been scanned in the past %d hour(s).", REJECT_DUPLICATES_DURATION_HOURS);
                            alertDialog.setMessage(message);
                            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                                    (dialog, i) -> dialog.dismiss());
                            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SUBMIT ANYWAY",
                                    (dialog, i) -> {
                                        insertToDatabase(userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, descriptionValuesArray.get(0), descriptionValuesArray.get(1), descriptionValuesArray.get(2), descriptionValuesArray.get(3), descriptionValuesArray.get(4), descriptionValuesArray.get(5), descriptionValuesArray.get(6), descriptionValuesArray.get(7), descriptionValuesArray.get(8), descriptionValuesArray.get(9), baseValuesArray.get(0), baseValuesArray.get(1), baseValuesArray.get(2), baseValuesArray.get(3), baseValuesArray.get(4), baseValuesArray.get(5), baseValuesArray.get(6), baseValuesArray.get(7), baseValuesArray.get(8), baseValuesArray.get(9), incrementValuesArray.get(0), incrementValuesArray.get(1), incrementValuesArray.get(2), incrementValuesArray.get(3), incrementValuesArray.get(4), incrementValuesArray.get(5), incrementValuesArray.get(6), incrementValuesArray.get(7), incrementValuesArray.get(8), incrementValuesArray.get(9), machineIdText, FieldValue.serverTimestamp(), userName, notesText, locationText);
                                        adapter.resetItems();
                                        showToast("Progressive(s) submitted successfully");
                                        hideKeyboard();
                                        dialog.dismiss();
                                        notes = "";
                                        //removeFromUploadArray();
                                        navigateBackToUploadFile();
                                    });
                            if (!isFinishing()) {
                                alertDialog.show();
                            } else {
                                Timber.e("trying to show dialog after destroy");
                            }
                        } else { // No Dups Found
                            insertToDatabase(userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, descriptionValuesArray.get(0), descriptionValuesArray.get(1), descriptionValuesArray.get(2), descriptionValuesArray.get(3), descriptionValuesArray.get(4), descriptionValuesArray.get(5), descriptionValuesArray.get(6), descriptionValuesArray.get(7), descriptionValuesArray.get(8), descriptionValuesArray.get(9), baseValuesArray.get(0), baseValuesArray.get(1), baseValuesArray.get(2), baseValuesArray.get(3), baseValuesArray.get(4), baseValuesArray.get(5), baseValuesArray.get(6), baseValuesArray.get(7), baseValuesArray.get(8), baseValuesArray.get(9), incrementValuesArray.get(0), incrementValuesArray.get(1), incrementValuesArray.get(2), incrementValuesArray.get(3), incrementValuesArray.get(4), incrementValuesArray.get(5), incrementValuesArray.get(6), incrementValuesArray.get(7), incrementValuesArray.get(8), incrementValuesArray.get(9),  machineIdText, FieldValue.serverTimestamp(), userName, notesText, locationText);
                            adapter.resetItems();
                            showToast("Progressive(s) submitted successfully");
                            hideKeyboard();
                            notes = "";
                            //removeFromUploadArray();
                            navigateBackToUploadFile();
                        }
                    } else {
                        showToast("Error submitting data");
                    }
                });
            } else { // Proceed to save scan
                insertToDatabase(userId, progressiveText1, progressiveText2, progressiveText3, progressiveText4, progressiveText5, progressiveText6, progressiveText7, progressiveText8, progressiveText9, progressiveText10, descriptionValuesArray.get(0), descriptionValuesArray.get(1), descriptionValuesArray.get(2), descriptionValuesArray.get(3), descriptionValuesArray.get(4), descriptionValuesArray.get(5), descriptionValuesArray.get(6), descriptionValuesArray.get(7), descriptionValuesArray.get(8), descriptionValuesArray.get(9), baseValuesArray.get(0), baseValuesArray.get(1), baseValuesArray.get(2), baseValuesArray.get(3), baseValuesArray.get(4), baseValuesArray.get(5), baseValuesArray.get(6), baseValuesArray.get(7), baseValuesArray.get(8), baseValuesArray.get(9), incrementValuesArray.get(0), incrementValuesArray.get(1), incrementValuesArray.get(2), incrementValuesArray.get(3), incrementValuesArray.get(4), incrementValuesArray.get(5), incrementValuesArray.get(6), incrementValuesArray.get(7), incrementValuesArray.get(8), incrementValuesArray.get(9),  machineIdText, FieldValue.serverTimestamp(), userName, notesText, locationText);
                adapter.resetItems();
                showToast("Progressive(s) submitted successfully");
                hideKeyboard();
                notes = "";
                //removeFromUploadArray();
                navigateBackToUploadFile();
            }
        } catch (Exception ex) {
            Timber.e(ex);
            showToast(ex.getMessage());
        }
    }

//    private void removeFromUploadArray() {
//        if (intent.hasExtra("hashMap")) {
//            HashMap<String, Object> hashMap = (HashMap<String, Object>)intent.getSerializableExtra("hashMap");
//            DocumentReference documentReference = database.collection("formUploads").document(firebaseAuth.getUid());
//            documentReference.update("uploadArray", FieldValue.arrayRemove(hashMap))
//                    .addOnSuccessListener(aVoid -> Timber.d("DocumentSnapshot successfully updated!"))
//                    .addOnFailureListener(e -> showToast("Error updating to do list. " + e.getMessage()));
//        }
//    }

    private void navigateBackToUploadFile() {
        if (intent.hasExtra("toDoItem")) {
            setResult(RESULT_OK, getIntent());
            finish();
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
            Timber.e("hideKeyboard failed: %s", ex.getMessage());
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

    private boolean isDouble(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public List<String> sortProgressives(String p1,
                                         String p2,
                                         String p3,
                                         String p4,
                                         String p5,
                                         String p6,
                                         String p7,
                                         String p8,
                                         String p9,
                                         String p10) {

        final List<Double> values = new ArrayList<>();
        List<String> ret = new ArrayList<>();

        if (isDouble(p1)) {
            values.add(Double.parseDouble(p1));
        }
        if (isDouble(p2)) {
            values.add(Double.parseDouble(p2));
        }
        if (isDouble(p3)) {
            values.add(Double.parseDouble(p3));
        }
        if (isDouble(p4)) {
            values.add(Double.parseDouble(p4));
        }
        if (isDouble(p5)) {
            values.add(Double.parseDouble(p5));
        }
        if (isDouble(p6)) {
            values.add(Double.parseDouble(p6));
        }
        if (isDouble(p7)) {
            values.add(Double.parseDouble(p7));
        }
        if (isDouble(p8)) {
            values.add(Double.parseDouble(p8));
        }
        if (isDouble(p9)) {
            values.add(Double.parseDouble(p9));
        }
        if (isDouble(p10)) {
            values.add(Double.parseDouble(p10));
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean sort = sharedPreferences.getBoolean("auto_sort_progressives_preference", true);

        if (sort) {
            Collections.sort(values);
            Collections.reverse(values);
        }
        DecimalFormat df = new DecimalFormat("0.00");

        for (int i = 0; i < 10; i++) {
            if (i < values.size()) {
                ret.add(df.format(values.get(i)));
            } else {
                ret.add("");
            }
        }
        return ret;
    }
}
