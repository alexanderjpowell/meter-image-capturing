package com.slotmachine.ocr.mic;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.support.v7.app.ActionBarDrawerToggle;
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
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.FileProvider;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, View.OnTouchListener {

    public static final int REQUEST_TAKE_PHOTO_PROGRESSIVES = 0;
    public static final int MY_PERMISSIONS_REQUEST_CODE = 1;
    public static final int REQUEST_TAKE_PHOTO_MACHINE_ID = 2;

    private static final int REQ_CODE_SPEECH_INPUT = 100;

    public String mCurrentPhotoPath;
    public Button mButton;
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

    public Bitmap mBitmap;
    private DrawerLayout mDrawerLayout;

    private FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    private String activityLabel = "Scan Machines";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(activityLabel);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            return;
        }

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView)findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);
        navDrawerText1 = headerView.findViewById(R.id.navDrawerText1);
        navDrawerText2 = headerView.findViewById(R.id.navDrawerText2);
        String userName = firebaseAuth.getCurrentUser().getDisplayName();
        navDrawerText1.setText("Meter Image Capturing");
        navDrawerText2.setText("Welcome " + userName + "!");

        mButton = (Button)findViewById(R.id.mButton1);
        progressive1 = (TextInputEditText)findViewById(R.id.progressive1);
        progressive2 = (TextInputEditText)findViewById(R.id.progressive2);
        progressive3 = (TextInputEditText)findViewById(R.id.progressive3);
        progressive4 = (TextInputEditText)findViewById(R.id.progressive4);
        progressive5 = (TextInputEditText)findViewById(R.id.progressive5);
        progressive6 = (TextInputEditText)findViewById(R.id.progressive6);
        machineId = (TextInputEditText)findViewById(R.id.machineId);

        checkPermissions();
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

        //progressDialog = new ProgressDialog(this);
        //progressDialog.setMessage("Performing OCR");
        //progressDialog.show();
        progressDialog = new ProgressDialog(MainActivity.this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final int DRAWABLE_RIGHT = 2;
        switch(v.getId()){
            case R.id.progressive1:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive1.getRight() - progressive1.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (progressive1.getText().toString().equals("")) {
                            progressive = 1;
                            startVoiceInput(1);
                            return true;
                        } else {
                            progressive1.setText("");
                        }
                    }
                }
                break;
            case R.id.progressive2:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive2.getRight() - progressive2.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (progressive2.getText().toString().equals("")) {
                            progressive = 2;
                            startVoiceInput(2);
                            return true;
                        } else {
                            progressive2.setText("");
                        }
                    }
                }
                break;
            case R.id.progressive3:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive3.getRight() - progressive3.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (progressive3.getText().toString().equals("")) {
                            progressive = 3;
                            startVoiceInput(3);
                            return true;
                        } else {
                            progressive3.setText("");
                        }
                    }
                }
                break;
            case R.id.progressive4:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive4.getRight() - progressive4.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (progressive4.getText().toString().equals("")) {
                            progressive = 4;
                            startVoiceInput(4);
                            return true;
                        } else {
                            progressive4.setText("");
                        }
                    }
                }
                break;
            case R.id.progressive5:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive5.getRight() - progressive5.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (progressive5.getText().toString().equals("")) {
                            progressive = 5;
                            startVoiceInput(5);
                            return true;
                        } else {
                            progressive5.setText("");
                        }
                    }
                }
                break;
            case R.id.progressive6:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (progressive6.getRight() - progressive6.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (progressive6.getText().toString().equals("")) {
                            progressive = 6;
                            startVoiceInput(6);
                            return true;
                        } else {
                            progressive6.setText("");
                        }
                    }
                }
                break;
            case R.id.machineId:
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    if(event.getRawX() >= (machineId.getRight() - machineId.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        if (machineId.getText().toString().equals("")) {
                            progressive = 7;
                            startVoiceInput(7);
                            return true;
                        } else {
                            machineId.setText("");
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {

        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
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

                            //mTextView.setText("Processing...");
                            ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = exifToDegrees(rotation);
                            //showToast("Orientation: " + Integer.toString(rotationInDegrees));
                            Matrix matrix = new Matrix();
                            if (rotation != 0) { matrix.preRotate(rotationInDegrees); }
                            //setPic(matrix); // Used if need to display image to user
                            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            //newBitmap = convertToGrayscale(newBitmap);
                            processProgressivesOCR(newBitmap);


                        } else {
                            showToast("Bitmap is null");
                        }
                    }
                    break;
                }
                case REQUEST_TAKE_PHOTO_MACHINE_ID: {
                    if (resultCode == RESULT_OK) {
                        File file = new File(mCurrentPhotoPath);
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), Uri.fromFile(file));
                        if (bitmap != null) {

                            //mTextView.setText("Processing...");
                            ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                            int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                            int rotationInDegrees = exifToDegrees(rotation);
                            //showToast("Orientation: " + Integer.toString(rotationInDegrees));
                            Matrix matrix = new Matrix();
                            if (rotation != 0) { matrix.preRotate(rotationInDegrees); }
                            //setPic(matrix); // Used if need to display image to user
                            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            //newBitmap = convertToGrayscale(newBitmap);
                            processMachineOCR(newBitmap);


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
                            progressive1.setText(formatVoiceToSpeech(result.get(0)));
                        } else if (progressive == 2) {
                            progressive2.setText(formatVoiceToSpeech(result.get(0)));
                        } else if (progressive == 3) {
                            progressive3.setText(formatVoiceToSpeech(result.get(0)));
                        } else if (progressive == 4) {
                            progressive4.setText(formatVoiceToSpeech(result.get(0)));
                        } else if (progressive == 5) {
                            progressive5.setText(formatVoiceToSpeech(result.get(0)));
                        } else if (progressive == 6) {
                            progressive6.setText(formatVoiceToSpeech(result.get(0)));
                        } else if (progressive == 7) {
                            machineId.setText(formatVoiceToSpeech(result.get(0)));
                        }
                    }
                    break;
                }
            }
        } catch (Exception error) {
            error.printStackTrace();
            showToast("Error");
        }
    }

    private void resetProgressives() {
        String emptyProgressiveValue = "";
        machineId.setText(emptyProgressiveValue);
        progressive1.setText(emptyProgressiveValue);
        progressive2.setText(emptyProgressiveValue);
        progressive3.setText(emptyProgressiveValue);
        progressive4.setText(emptyProgressiveValue);
        progressive5.setText(emptyProgressiveValue);
        progressive6.setText(emptyProgressiveValue);
        machineId.clearFocus();
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

        progressDialog.setMessage("Processing...");
        progressDialog.show();

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
                                TextParser parser = new TextParser();
                                showToast("Cloud OCR Successful");
                                List<FirebaseVisionDocumentText.Block> blocks = firebaseVisionDocumentText.getBlocks();
                                if (blocks.size() == 0) {
                                    showToast("No text detected.  Try again.  ");
                                    return;
                                }
                                for (FirebaseVisionDocumentText.Block block : blocks) {
                                    List<FirebaseVisionDocumentText.Paragraph> paragraphs = block.getParagraphs();
                                    for (FirebaseVisionDocumentText.Paragraph paragraph : paragraphs) {
                                        List<FirebaseVisionDocumentText.Word> words = paragraph.getWords();
                                        for (FirebaseVisionDocumentText.Word word : words) {
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
                                StringBuilder builder = new StringBuilder();
                                for (String dollar : dollarValues) {
                                    builder.append(dollar + "\n");
                                }
                                //mTextView.setText(builder.toString());

                                Log.d("WORD: ", Integer.toString(filteredWords.size()));

                                for (FirebaseVisionDocumentText.Word word : filteredWords) {
                                    Log.d("WORD: ", word.getText() + " :: " + word.getBoundingBox().toString());
                                }

                                // Add to TextViews
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

        progressDialog.dismiss();
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

    public void scanMachineId(View view) {
        dispatchTakePictureIntent(REQUEST_TAKE_PHOTO_MACHINE_ID);
    }

    private void printRect(Rect rect) {
        String left = Integer.toString(rect.left);
        String right = Integer.toString(rect.left);
        String top = Integer.toString(rect.top);
        String bottom = Integer.toString(rect.bottom);
        Log.d("Bounding Box","Left: " + left + ", Right: " + right + ", Top: " + top + ", Bottom: " + bottom);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,   /* prefix    */
                ".jpg",   /* suffix    */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private boolean isAlpha(String name) {
        return name.matches("[a-zA-Z]+");
    }

    private boolean isDigits(String name) {
        return name.matches("[0-9]+");
    }

    public void submitOnClick(View view) {
        resetProgressives();
        showToast("Progressives submitted successfully");
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

    private String formatVoiceToSpeech(String text) {
        text = text.toLowerCase();
        String original = text;
        text = text.replaceAll("\\s",""); // Removes all spaces
        text = text.replaceAll("[^0-9]",""); // Removes all but digits
        if (original.contains("progressive")) {
            if (original.length() >= 3) {
                return text.substring(0, text.length() - 2) + "." + text.substring(text.length() - 2);
            }
        } else if (original.contains("machine")) {
            return text;
        }
        return text;
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

    public void sortProgressives(View view) {

        List<Double> values = new ArrayList<Double>();

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

        Collections.sort(values);
        Collections.reverse(values);
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
    }
}
