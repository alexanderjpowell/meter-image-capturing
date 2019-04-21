package com.slotmachine.ocr.mic;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;

import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.media.ExifInterface;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import android.speech.RecognizerIntent;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    public TextInputEditText machineId;

    public TextView navDrawerText1;
    public TextView navDrawerText2;
    public Spinner spinner;

    public Bitmap mBitmap;
    private DrawerLayout mDrawerLayout;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    //private ProgressDialog progressDialog;

    private String ACTIVITY_LABEL = "User:";

    private Double minimumProgressiveValue;
    private Boolean autoSortProgressives = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(ACTIVITY_LABEL);

        // Ensure user is signed in
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }
        //

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set nav header color
        int color = Color.parseColor("#2196F3");
        NavigationView navView = findViewById(R.id.nav_view);
        View header = navView.getHeaderView(0);
        header.setBackgroundColor(color);

        //
        database = FirebaseFirestore.getInstance();
        //
        checkPermissions();

        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        //navDrawerText1 = headerView.findViewById(R.id.navDrawerText1);
        //navDrawerText2 = headerView.findViewById(R.id.navDrawerText2);
        //String userName = firebaseAuth.getCurrentUser().getDisplayName();
        //navDrawerText1.setText("Meter Image Capturing");
        //navDrawerText2.setText("Welcome " + userName + "!");

        progressive1 = (TextInputEditText)findViewById(R.id.progressive1);
        progressive2 = (TextInputEditText)findViewById(R.id.progressive2);
        progressive3 = (TextInputEditText)findViewById(R.id.progressive3);
        progressive4 = (TextInputEditText)findViewById(R.id.progressive4);
        progressive5 = (TextInputEditText)findViewById(R.id.progressive5);
        progressive6 = (TextInputEditText)findViewById(R.id.progressive6);
        machineId = (TextInputEditText)findViewById(R.id.machineId);
        submitButton = (Button)findViewById(R.id.submit_button);

        resetProgressives();

        progressive1.setOnTouchListener(this);
        progressive2.setOnTouchListener(this);
        progressive3.setOnTouchListener(this);
        progressive4.setOnTouchListener(this);
        progressive5.setOnTouchListener(this);
        progressive6.setOnTouchListener(this);
        machineId.setOnTouchListener(this);

        progressive1.addTextChangedListener(new GenericTextWatcher(progressive1));
        progressive2.addTextChangedListener(new GenericTextWatcher(progressive2));
        progressive3.addTextChangedListener(new GenericTextWatcher(progressive3));
        progressive4.addTextChangedListener(new GenericTextWatcher(progressive4));
        progressive5.addTextChangedListener(new GenericTextWatcher(progressive5));
        progressive6.addTextChangedListener(new GenericTextWatcher(progressive6));
        machineId.addTextChangedListener(new GenericTextWatcher(machineId));

        // Get minimum progressive amount
        database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().get("minimumProgressiveValue") != null) {
                                minimumProgressiveValue = Double.parseDouble(task.getResult().get("minimumProgressiveValue").toString());
                            } else {
                                minimumProgressiveValue = 0.0;
                            }

                            if (task.getResult().get("sortProgressives") != null) {
                                autoSortProgressives = (boolean)task.getResult().get("sortProgressives");
                            } else {
                                autoSortProgressives = true;
                            }
                        }
                    }
                });
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {

        final int DRAWABLE_RIGHT = 2;
        switch(v.getId()){
            case R.id.progressive1:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive1.getRight() - progressive1.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
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
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive2.getRight() - progressive2.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
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
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive3.getRight() - progressive3.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
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
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive4.getRight() - progressive4.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
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
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive5.getRight() - progressive5.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
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
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive6.getRight() - progressive6.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
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
            case R.id.machineId:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (machineId.getRight() - machineId.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 15)) {
                        if (machineId.getError() == null) {
                            if (machineId.getText().toString().equals("")) {
                                progressive = 7;
                                startVoiceInput(7);
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

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

        MenuItem item = menu.findItem(R.id.action_bar_spinner);
        spinner = (Spinner)MenuItemCompat.getActionView(item);
        final List<String> spinnerArray = new ArrayList<>();
        // Populate spinnerArray from database
        database.collection("users")
                .document(uid)
                .collection("displayNames")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                spinnerArray.add(document.get("displayName").toString());
                            }
                            if (spinnerArray.isEmpty()) {
                                spinnerArray.add("No users created");
                            }
                            Collections.sort(spinnerArray);
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, R.layout.spinner_item, spinnerArray);
                            adapter.setDropDownViewResource(R.layout.spinner_item);
                            spinner.setAdapter(adapter);

                            MyApplication app = (MyApplication)getApplication();
                            int userNameIndex = app.getUsernameIndex() == null ? 0 : app.getUsernameIndex();
                            spinner.setSelection(userNameIndex);
                        } else {
                            showToast("Error getting users");
                        }
                    }
                });

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

        //showToast("testing");

        //if (id == R.id.action_settings) {
            //showToast(spinner.getSelectedItem().toString());
            //return true;
        if (id == R.id.action_bar_spinner) {
            //showToast("spinner");
            showToast(spinner.getSelectedItem().toString());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        Intent intent;
        MyApplication app = (MyApplication)getApplication();
        app.setUsernameIndex(spinner.getSelectedItemPosition());

        if (id == R.id.nav_gallery) {
            intent = new Intent(MainActivity.this, DataReportActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_slideshow) {
            intent = new Intent(MainActivity.this, ManageUsersActivity.class);
            startActivity(intent);
        } else if (id == R.id.nav_settings) {
            intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }

        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        //showToast("onActivityResult");
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
                            //setPic(matrix); // Used if need to display image to user
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
                            machineId.setText(formatVoiceToSpeech(result.get(0), false));
                        }
                    }
                    break;
                }
                case REQUEST_SETTINGS_ACTIVITY: {
                    //showToast("coming from settings activity");
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
        progressive1.clearFocus();
        progressive2.clearFocus();
        progressive3.clearFocus();
        progressive4.clearFocus();
        progressive5.clearFocus();
        progressive6.clearFocus();
    }

    private void processMachineOCR(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                if (firebaseVisionText == null) {
                                    Log.d("ERROR", "firebaseVisionText is null");
                                    showToast("No text detected.  Try again.  ");
                                    return;
                                }
                                StringBuilder sb =  new StringBuilder();
                                String resultText = firebaseVisionText.getText();
                                for (FirebaseVisionText.TextBlock block: firebaseVisionText.getTextBlocks()) {
                                    //String blockText = block.getText();
                                    for (FirebaseVisionText.Line line: block.getLines()) {
                                        for (FirebaseVisionText.Element element: line.getElements()) {
                                            String text = element.getText().trim();
                                            //Float elementConfidence = element.getConfidence();
                                            if (isDigits(text) && (text.length() > 3)) {
                                                //sb.append(text);
                                                //sb.append("\n");
                                                machineId.setText(text);
                                            }
                                        }
                                    }
                                }
                                //showToast(sb.toString());
                                //machineId.setText(resultText);
                                //showToast(resultText);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        showToast("Error with OCR");
                                    }
                                });
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
                                        if (getNumberOfOccurences(paragraph.getText()) == 2) {
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
                                    List<String> newDollarValues = new ArrayList<String>() {};
                                    for (int i = 0; i < dollarValues.size(); i++) {
                                        if (Double.parseDouble(dollarValues.get(i)) >= minimumProgressiveValue) {
                                            newDollarValues.add(dollarValues.get(i));
                                        }
                                    }
                                    dollarValues = newDollarValues;
                                }
                                //StringBuilder builder = new StringBuilder();
                                //for (String dollar : dollarValues) {
                                //    builder.append(dollar + "\n");
                                //}
                                //mTextView.setText(builder.toString());

                                //Log.d("WORD: ", Integer.toString(filteredWords.size()));

                                //for (FirebaseVisionDocumentText.Word word : filteredWords) {
                                //    Log.d("WORD: ", word.getText() + " :: " + word.getBoundingBox().toString());
                                //}

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
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
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

    private int getNumberOfOccurences(String input) {
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
                && progressive6.getText().toString().trim().isEmpty());
    }

    public void submitOnClick(View view) {

        try {
            sortProgressives();

            // Get data points
            String machineIdText = machineId.getText().toString().trim();
            String progressiveText1 = progressive1.getText().toString().trim();
            String progressiveText2 = progressive2.getText().toString().trim();
            String progressiveText3 = progressive3.getText().toString().trim();
            String progressiveText4 = progressive4.getText().toString().trim();
            String progressiveText5 = progressive5.getText().toString().trim();
            String progressiveText6 = progressive6.getText().toString().trim();
            String emailText = firebaseAuth.getCurrentUser().getEmail().trim();
            String userId = firebaseAuth.getCurrentUser().getUid().trim();
            String userName = (spinner.getSelectedItem() == null) ? "No user selected" : spinner.getSelectedItem().toString();

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

            Map<String, Object> user = new HashMap<>();
            //user.put("name", displayNameText);
            user.put("email", emailText);
            user.put("uid", userId);
            user.put("progressive1", progressiveText1);
            user.put("progressive2", progressiveText2);
            user.put("progressive3", progressiveText3);
            user.put("progressive4", progressiveText4);
            user.put("progressive5", progressiveText5);
            user.put("progressive6", progressiveText6);
            user.put("machine_id", machineIdText);
            user.put("timestamp", FieldValue.serverTimestamp());
            user.put("userName", userName);
            user.put("notes", "");

            database.collection("scans").document().set(user);

            resetMachineId();
            resetProgressives();
            showToast("Progressives submitted successfully");
            hideKeyboard();
        } catch (Exception ex) {
            showToast("No connection");
        }
    }

    public void hideKeyboard() {
        LinearLayout layout = findViewById(R.id.mainLinearLayout);
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(layout.getWindowToken(), 0);
    }

    private void startVoiceInput(int progressive) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        if (progressive == 7)
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
        //showToast(text);
        text = text.toLowerCase();
        String original = text;
        StringBuilder newText = new StringBuilder();
        String[] words = text.split("\\s");
        for (int i = 0; i < words.length; i++) {
            //words[i] = convertString(words[i]);
            newText.append(convertString(words[i]));
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

        if (autoSortProgressives) {
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
            }
        }

        /*database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            if (task.getResult().get("sortProgressives") != null) {
                                boolean sort = (boolean)task.getResult().get("sortProgressives");
                                if (sort) {
                                }
                            } else {
                            }
                        } else {
                            showToast("No connection");
                        }
                    }
                });*/
    }
}
