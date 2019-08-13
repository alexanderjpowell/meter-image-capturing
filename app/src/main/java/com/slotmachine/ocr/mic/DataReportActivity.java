package com.slotmachine.ocr.mic;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class DataReportActivity extends AppCompatActivity {// implements AdapterView.OnItemSelectedListener {

    private List<RowData> rowDataList;
    private RecyclerView recyclerView;
    private ReportDataAdapter mAdapter;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;
    private QueryDocumentSnapshot lastDocumentSnapshot;

    private SwipeRefreshLayout swipeRefreshLayout;

    private enum DateRange { HOUR, DAY, WEEK }
    private DateRange dateRange;

    int pastVisibleItems, visibleItemCount, totalItemCount;
    private boolean LOADING = true;

    int offset = 0;
    private int QUERY_LIMIT_SIZE = 10;

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

        dateRange = DateRange.DAY; // Default to last 24 hours
        offset = 86400; // ...

        database = FirebaseFirestore.getInstance();
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        executeQuery(dateRange);
                    }
                }
        );

        rowDataList = new ArrayList<>();
        recyclerView = findViewById(R.id.recycler_view);
        mAdapter = new ReportDataAdapter(DataReportActivity.this, rowDataList);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                RowData rowData = rowDataList.get(position);
                Intent intent = new Intent(DataReportActivity.this, EditScanActivity.class);
                intent.putExtra("MACHINE_ID", getMachineIdFromString(rowData.getMachineId()));
                intent.putExtra("PROGRESSIVE_1", removeDollarSignFromString(rowData.getProgressive1()));
                intent.putExtra("PROGRESSIVE_2", removeDollarSignFromString(rowData.getProgressive2()));
                intent.putExtra("PROGRESSIVE_3", removeDollarSignFromString(rowData.getProgressive3()));
                intent.putExtra("PROGRESSIVE_4", removeDollarSignFromString(rowData.getProgressive4()));
                intent.putExtra("PROGRESSIVE_5", removeDollarSignFromString(rowData.getProgressive5()));
                intent.putExtra("PROGRESSIVE_6", removeDollarSignFromString(rowData.getProgressive6()));
                intent.putExtra("NOTES", rowData.getNotes());
                intent.putExtra("DOCUMENT_ID", rowData.getDocumentId());
                startActivity(intent);
            }

            @Override
            public void onLongClick(View view, final int position) {
                final RowData rowData = rowDataList.get(position);
                AlertDialog alertDialog = new AlertDialog.Builder(DataReportActivity.this).create();
                alertDialog.setMessage("Are you sure you want to delete this scan?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int i) {
                                deleteScanFromDatabase(rowData.getDocumentId());
                                rowDataList.remove(position);
                                mAdapter.notifyItemRemoved(position);
                                mAdapter.notifyItemRangeChanged(position, rowDataList.size());
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
        }));

        // Load more results when bottom of page is reached
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    visibleItemCount = mLayoutManager.getChildCount();
                    totalItemCount = mLayoutManager.getItemCount();
                    pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (LOADING) {
                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            LOADING = false;
                            loadMoreRecords();
                        }
                    }
                }
            }
        });
        //

        executeQuery(dateRange);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        /*if (id == R.id.action_delete) {
            String data = "";
            List<RowData> listRowData = mAdapter.getRowDataList();
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
        }*/
        if (id == R.id.action_past_hour) {
            dateRange = DateRange.HOUR;
            offset = 3600;
            executeQuery(dateRange);
        } else if (id == R.id.action_past_day) {
            dateRange = DateRange.DAY;
            offset = 86400;
            executeQuery(dateRange);
        } else if (id == R.id.action_past_week) {
            dateRange = DateRange.WEEK;
            offset = 604800;
            executeQuery(dateRange);
        }
        return super.onOptionsItemSelected(item);
    }

    private int getOffsetFromDateRange(DateRange dateRange) {
        int ret = 86400;
        if (dateRange.equals(DateRange.HOUR))
            ret = 3600;
        else if (dateRange.equals(DateRange.DAY))
            ret = 86400;
        else if (dateRange.equals(DateRange.WEEK))
            ret = offset = 604800;
        return ret;
    }

    private void loadMoreRecords() {
        Date time = new Date(System.currentTimeMillis() - offset * 1000);
        CollectionReference collectionReference = database.collection("scans");
        Query query = collectionReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
                .whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastDocumentSnapshot)
                .limit(QUERY_LIMIT_SIZE);//5
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    updateData(task.getResult());
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
                LOADING = true;
            }
        });
    }

    private void deleteScanFromDatabase(String document_id) {
        database.collection("scans").document(document_id).delete();
    }

    private void executeQuery(DateRange dateRange) {
        Date time = new Date(System.currentTimeMillis() - offset * 1000);
        CollectionReference collectionReference = database.collection("scans");
        Query query = collectionReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
                .whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(QUERY_LIMIT_SIZE);//5000
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    prepareData(task.getResult());
                } else {
                    showToast("Unable to refresh.  Check your connection.");
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void prepareData(QuerySnapshot snapshot) {

        String machine_id, timestamp, user, progressive1, progressive2, progressive3, progressive4, progressive5, progressive6, notes;
        RowData rowData;
        rowDataList.clear(); // reset the current data list
        //rowDataListReport.clear();

        for (QueryDocumentSnapshot document : snapshot) {

            lastDocumentSnapshot = document;

            machine_id = document.get("machine_id").toString();
            timestamp = document.getTimestamp("timestamp").toDate().toString();
            user = (document.get("userName") == null) ? "User not specified" : document.get("userName").toString();
            progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
            progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
            progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
            progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
            progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
            progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
            notes = (document.get("notes") == null) ? "" : document.get("notes").toString().trim();

            rowData = new RowData(document.getId(),
                    "Machine ID: " + machine_id,
                    timestamp,
                    user,
                    progressive1,
                    progressive2,
                    progressive3,
                    progressive4,
                    progressive5,
                    progressive6,
                    notes,
                    false);

            rowDataList.add(rowData);
            //rowDataListReport.add(rowData);
        }

        mAdapter.notifyDataSetChanged();
    }

    private void updateData(QuerySnapshot snapshot) {

        String machine_id, timestamp, user, progressive1, progressive2, progressive3, progressive4, progressive5, progressive6, notes;
        RowData rowData;

        for (QueryDocumentSnapshot document : snapshot) {

            lastDocumentSnapshot = document;

            machine_id = document.get("machine_id").toString();
            timestamp = document.getTimestamp("timestamp").toDate().toString();
            user = (document.get("userName") == null) ? "User not specified" : document.get("userName").toString();
            progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
            progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
            progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
            progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
            progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
            progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
            notes = (document.get("notes") == null) ? "" : document.get("notes").toString().trim();

            rowData = new RowData(document.getId(),
                    "Machine ID: " + machine_id,
                    timestamp,
                    user,
                    progressive1,
                    progressive2,
                    progressive3,
                    progressive4,
                    progressive5,
                    progressive6,
                    notes,
                    false);

            rowDataList.add(rowData);
            //rowDataListReport.add(rowData);
        }

        mAdapter.notifyDataSetChanged();
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private String getMachineIdFromString(String text) {
        //Machine ID: 9792// <- Example of what text looks like
        text = text.replace("Machine ID: ", "");
        return text;
    }

    private String removeDollarSignFromString(String text) {
        text = text.replace("$", "");
        return text;
    }

    /*private String createCsvFile() {

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"Machine\",\"Progressive1\",\"Progressive2\",\"Progressive3\",\"Progressive4\",\"Progressive5\",\"Progressive6\", \"Notes\",\"Date\",\"User\"\n");
        for (RowData rowData : rowDataListReport) {
            stringBuilder.append("\"" + getMachineIdFromString(rowData.getMachineId()) + "\",");
            stringBuilder.append("\"" + rowData.getProgressive1() + "\",");
            stringBuilder.append("\"" + rowData.getProgressive2() + "\",");
            stringBuilder.append("\"" + rowData.getProgressive3() + "\",");
            stringBuilder.append("\"" + rowData.getProgressive4() + "\",");
            stringBuilder.append("\"" + rowData.getProgressive5() + "\",");
            stringBuilder.append("\"" + rowData.getProgressive6() + "\",");
            stringBuilder.append("\"" + rowData.getNotes() + "\",");
            stringBuilder.append("\"" + rowData.getDate() + "\",");
            stringBuilder.append("\"" + rowData.getUser() + "\"\n");
        }

        return stringBuilder.toString();
    }*/

    private String createCsvFile2(QuerySnapshot snapshot) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"Machine\",\"Progressive1\",\"Progressive2\",\"Progressive3\",\"Progressive4\",\"Progressive5\",\"Progressive6\", \"Notes\",\"Date\",\"User\"\n");
        for (QueryDocumentSnapshot document : snapshot) {
            String machine_id = document.get("machine_id").toString();
            String timestamp = document.getTimestamp("timestamp").toDate().toString();
            String user = (document.get("userName") == null) ? "User not specified" : document.get("userName").toString();
            String progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
            String progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
            String progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
            String progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
            String progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
            String progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
            String notes = (document.get("notes") == null) ? "" : document.get("notes").toString().trim();

            stringBuilder.append("\"" + machine_id + "\",");
            stringBuilder.append("\"" + progressive1 + "\",");
            stringBuilder.append("\"" + progressive2 + "\",");
            stringBuilder.append("\"" + progressive3 + "\",");
            stringBuilder.append("\"" + progressive4 + "\",");
            stringBuilder.append("\"" + progressive5 + "\",");
            stringBuilder.append("\"" + progressive6 + "\",");
            stringBuilder.append("\"" + notes + "\",");
            stringBuilder.append("\"" + timestamp + "\",");
            stringBuilder.append("\"" + user + "\"\n");
        }
        return stringBuilder.toString();
    }

    public void generateReport(View view) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        final String report_recipient_email = sharedPreferences.getString("email_recipient", "");
        final String[] emails = report_recipient_email.split(",");
        final File csvFile = new File(getFilesDir(), "report.csv");

        if (isExternalStorageWritable()) {
            Date time = new Date(System.currentTimeMillis() - offset * 1000);
            CollectionReference collectionReference = database.collection("scans");
            Query query = collectionReference.whereEqualTo("uid", firebaseAuth.getCurrentUser().getUid())
                    .whereGreaterThan("timestamp", time)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(5000);
            query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        String report_contents = createCsvFile2(task.getResult());
                        try {
                            FileOutputStream fos = new FileOutputStream(csvFile);
                            fos.write(report_contents.getBytes());
                            fos.close();

                            Uri uri = FileProvider.getUriForFile(DataReportActivity.this, "com.slotmachine.ocr.mic.fileprovider", csvFile);
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            if (!report_recipient_email.isEmpty()) {
                                intent.putExtra(Intent.EXTRA_EMAIL, emails);
                            }
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            intent.putExtra(Intent.EXTRA_STREAM, uri);
                            intent.setType("text/csv");

                            startActivity(Intent.createChooser(intent, "Share to"));
                        } catch (Exception ex) {
                            showToast(ex.getMessage());
                        }
                    } else {
                        showToast("Unable to generate report.  Check your connection.");
                    }
                }
            });
        } else {
            showToast("Cannot write to external storage");
        }
    }
}
