package com.slotmachine.ocr.mic;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import com.google.android.material.textfield.TextInputEditText;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditScanActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private TextInputEditText machineId;
    private TextInputEditText progressive1;
    private TextInputEditText progressive2;
    private TextInputEditText progressive3;
    private TextInputEditText progressive4;
    private TextInputEditText progressive5;
    private TextInputEditText progressive6;
    private TextInputEditText notes;

    private String machine_id;
    private String progressive_1;
    private String progressive_2;
    private String progressive_3;
    private String progressive_4;
    private String progressive_5;
    private String progressive_6;
    private String notes_text;
    private String document_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_scan);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            showToast("not logged in");
            Log.d("AUTHENTICATION", "not logged in");
            startActivity(new Intent(EditScanActivity.this, LoginActivity.class));
            finish();
            return;
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        database = FirebaseFirestore.getInstance();

        machineId = (TextInputEditText)findViewById(R.id.machineId);
        progressive1 = (TextInputEditText)findViewById(R.id.progressive1);
        progressive2 = (TextInputEditText)findViewById(R.id.progressive2);
        progressive3 = (TextInputEditText)findViewById(R.id.progressive3);
        progressive4 = (TextInputEditText)findViewById(R.id.progressive4);
        progressive5 = (TextInputEditText)findViewById(R.id.progressive5);
        progressive6 = (TextInputEditText)findViewById(R.id.progressive6);
        notes = (TextInputEditText)findViewById(R.id.notes);

        Intent intent = getIntent();
        machine_id = intent.getStringExtra("MACHINE_ID");
        progressive_1 = intent.getStringExtra("PROGRESSIVE_1");
        progressive_2 = intent.getStringExtra("PROGRESSIVE_2");
        progressive_3 = intent.getStringExtra("PROGRESSIVE_3");
        progressive_4 = intent.getStringExtra("PROGRESSIVE_4");
        progressive_5 = intent.getStringExtra("PROGRESSIVE_5");
        progressive_6 = intent.getStringExtra("PROGRESSIVE_6");
        notes_text = intent.getStringExtra("NOTES");
        document_id = intent.getStringExtra("DOCUMENT_ID");

        machineId.setText(machine_id);
        progressive1.setText(progressive_1);
        progressive2.setText(progressive_2);
        progressive3.setText(progressive_3);
        progressive4.setText(progressive_4);
        progressive5.setText(progressive_5);
        progressive6.setText(progressive_6);
        notes.setText(notes_text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_reports_action_bar, menu);
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(EditScanActivity.this, LoginActivity.class));
            finish();
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_save_changes) {
            saveToDatabase();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveToDatabase() {


        Map<String, Object> map = new HashMap<>();
        map.put("progressive1", progressive1.getText().toString());
        map.put("progressive2", progressive2.getText().toString());
        map.put("progressive3", progressive3.getText().toString());
        map.put("progressive4", progressive4.getText().toString());
        map.put("progressive5", progressive5.getText().toString());
        map.put("progressive6", progressive6.getText().toString());
        map.put("machine_id", machineId.getText().toString());
        map.put("timestamp", FieldValue.serverTimestamp());
        map.put("notes", notes.getText().toString());

        database.collection("scans")
                .document(document_id)
                .update(map)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                           @Override
                                           public void onComplete(@NonNull Task<Void> task) {
                                               if (task.isSuccessful()) {
                                                   startActivity(new Intent(EditScanActivity.this, DataReportActivity.class));
                                                   finish();
                                               } else {
                                                   showToast("No connection");
                                               }
                                           }
                                       }
                );
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

}
