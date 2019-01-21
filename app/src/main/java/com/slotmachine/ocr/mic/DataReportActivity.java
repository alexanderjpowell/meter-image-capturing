package com.slotmachine.ocr.mic;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class DataReportActivity extends AppCompatActivity {// implements AdapterView.OnItemSelectedListener {

    private List<RowData> rowDataList;
    private RecyclerView recyclerView;
    private ReportDataAdapter mAdapter;
    //private Spinner spinner;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_report);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Ensure user is signed in
        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(DataReportActivity.this, LoginActivity.class));
            return;
        }

        database = FirebaseFirestore.getInstance();

        rowDataList = new ArrayList<>();

        recyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mAdapter = new ReportDataAdapter(DataReportActivity.this, rowDataList);

        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                RowData rowData = rowDataList.get(position);
                showToast(rowData.getMachineId() + " is selected");
                startActivity(new Intent(DataReportActivity.this, EditScanActivity.class));
            }

            @Override
            public void onLongClick(View view, int position) {
                RowData rowData = rowDataList.get(position);
                showToast("Long Click on " + rowData.getMachineId());
            }
        }));

        // Read from database
        CollectionReference scansReference = database.collection("scans");
        Query query = scansReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid()).orderBy("timestamp", Query.Direction.DESCENDING).limit(100); // order and limit here
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    prepareData(task.getResult());
                } else {
                    Log.d("DBREADER", "Error getting documents: ", task.getException());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            // remove item(s) from recycler view

            String data = "";
            List<RowData> listRowData = mAdapter.getRowDataList();
            /*List<Integer> indicesToRemove = new ArrayList<>();
            for (int i = 0; i < listRowData.size(); i++) {
                RowData rowData2 = listRowData.get(i);
                if (rowData2.isSelected()) {
                    data = data + "\n" + rowData2.getMachineId() + ", Index: " + Integer.toString(i);
                    indicesToRemove.add(i);
                    mAdapter.removeItem(i);
                }
            }*/

            Iterator<RowData> listRowDataIterator = listRowData.iterator();
            int count = 0;
            while (listRowDataIterator.hasNext()) {
                RowData r = listRowDataIterator.next();
                RowData rowData2 = listRowData.get(count);
                if (rowData2.isSelected()) {
                    data = data + "\n" + r.getMachineId() + ", Index: " + Integer.toString(count);
                    listRowDataIterator.remove();
                }
                count++;
            }


            showToast(data);
        } else if (id == R.id.action_past_hour) {
            executeQuery("HOUR");
        } else if (id == R.id.action_past_day) {
            executeQuery("DAY");
        } else if (id == R.id.action_past_week) {
            executeQuery("WEEK");
        }
        return super.onOptionsItemSelected(item);
    }

    private void executeQuery(String dateRange) {
        int offset = 0;
        if (dateRange.equals("HOUR"))
            offset = 3600;
        else if (dateRange.equals("DAY"))
            offset = 86400;
        else if (dateRange.equals("WEEK"))
            offset = 604800;
        Date time = new Date(System.currentTimeMillis() - offset * 1000);
        CollectionReference collectionReference = database.collection("scans");
        Query query = collectionReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
                .whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100);
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    prepareData(task.getResult());
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
            }
        });
    }

    private void prepareData(QuerySnapshot snapshot) {

        String machine_id, timestamp, progressive1, progressive2, progressive3, progressive4, progressive5, progressive6;
        RowData rowData;
        rowDataList.clear();

        for (QueryDocumentSnapshot document : snapshot) {

            machine_id = document.get("machine_id").toString();
            timestamp = document.get("timestamp").toString();
            progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
            progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
            progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
            progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
            progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
            progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();

            rowData = new RowData("Machine ID: " + machine_id,
                    timestamp,
                    progressive1,
                    progressive2,
                    progressive3,
                    progressive4,
                    progressive5,
                    progressive6,
                    false);
            rowDataList.add(rowData);
        }

        mAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void showSnackBar(View view, String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
    }

    public void generateReport(View view) {
        showSnackBar(view, "Report generated");
    }
}
