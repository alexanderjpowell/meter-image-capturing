package com.slotmachine.ocr.mic;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
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
    private EditText machineId;
    private EditText progressive1;
    private EditText progressive2;
    private EditText progressive3;
    private EditText progressive4;
    private EditText progressive5;
    private EditText progressive6;
    private EditText progressive7;
    private EditText progressive8;
    private EditText progressive9;
    private EditText progressive10;
    private EditText notes;
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

        machineId = findViewById(R.id.machineId);
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
        notes = findViewById(R.id.notes);

        Intent intent = getIntent();
        String machine_id = intent.getStringExtra("MACHINE_ID");
        String progressive_1 = intent.getStringExtra("PROGRESSIVE_1");
        String progressive_2 = intent.getStringExtra("PROGRESSIVE_2");
        String progressive_3 = intent.getStringExtra("PROGRESSIVE_3");
        String progressive_4 = intent.getStringExtra("PROGRESSIVE_4");
        String progressive_5 = intent.getStringExtra("PROGRESSIVE_5");
        String progressive_6 = intent.getStringExtra("PROGRESSIVE_6");
        String progressive_7 = intent.getStringExtra("PROGRESSIVE_7");
        String progressive_8 = intent.getStringExtra("PROGRESSIVE_8");
        String progressive_9 = intent.getStringExtra("PROGRESSIVE_9");
        String progressive_10 = intent.getStringExtra("PROGRESSIVE_10");
        String notes_text = intent.getStringExtra("NOTES");
        document_id = intent.getStringExtra("DOCUMENT_ID");

        machineId.setText(machine_id);
        progressive1.setText(progressive_1);
        progressive2.setText(progressive_2);
        progressive3.setText(progressive_3);
        progressive4.setText(progressive_4);
        progressive5.setText(progressive_5);
        progressive6.setText(progressive_6);
        progressive7.setText(progressive_7);
        progressive8.setText(progressive_8);
        progressive9.setText(progressive_9);
        progressive10.setText(progressive_10);
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
        map.put("progressive7", progressive7.getText().toString());
        map.put("progressive8", progressive8.getText().toString());
        map.put("progressive9", progressive9.getText().toString());
        map.put("progressive10", progressive10.getText().toString());
        map.put("machine_id", machineId.getText().toString());
        map.put("timestamp", FieldValue.serverTimestamp());
        map.put("notes", notes.getText().toString());

        database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("scans")
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
                });
        //
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

}
