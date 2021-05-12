package com.slotmachine.ocr.mic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.SimpleDateFormat;

public class DataReportActivity extends AppCompatActivity {

    private List<RowData> rowDataList;
    private RecyclerView recyclerView;
    private ReportDataAdapter mAdapter;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;
    private QueryDocumentSnapshot lastDocumentSnapshot;

    private SwipeRefreshLayout swipeRefreshLayout;

    private ProgressBar progressBar;
    private MaterialSearchView searchView;

    private enum DateRange { HOUR, DAY, WEEK }
    private DateRange dateRange;

    int pastVisibleItems, visibleItemCount, totalItemCount;
    private boolean LOADING = true;

    int offset = 0;
    private int QUERY_LIMIT_SIZE = 10;
    private int NUMBER_OF_PROGRESSIVES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_report);

        Toolbar mTopToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(DataReportActivity.this, LoginActivity.class));
            finish();
            return;
        }

        dateRange = DateRange.DAY; // Default to last 24 hours
        offset = 86400; // ...

        database = FirebaseFirestore.getInstance();
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(
                () -> {
                    // This method performs the actual data-refresh operation.
                    // The method calls setRefreshing(false) when it's finished.
                    executeQuery(dateRange);
                }
        );

        progressBar = findViewById(R.id.progress_bar);

        rowDataList = new ArrayList<>();
        recyclerView = findViewById(R.id.recycler_view);
        mAdapter = new ReportDataAdapter(rowDataList);

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
                intent.putExtra("MACHINE_ID", rowData.getMachineId());
                intent.putExtra("PROGRESSIVE_1", removeDollarSignFromString(rowData.getProgressive1()));
                intent.putExtra("PROGRESSIVE_2", removeDollarSignFromString(rowData.getProgressive2()));
                intent.putExtra("PROGRESSIVE_3", removeDollarSignFromString(rowData.getProgressive3()));
                intent.putExtra("PROGRESSIVE_4", removeDollarSignFromString(rowData.getProgressive4()));
                intent.putExtra("PROGRESSIVE_5", removeDollarSignFromString(rowData.getProgressive5()));
                intent.putExtra("PROGRESSIVE_6", removeDollarSignFromString(rowData.getProgressive6()));
                intent.putExtra("PROGRESSIVE_7", removeDollarSignFromString(rowData.getProgressive7()));
                intent.putExtra("PROGRESSIVE_8", removeDollarSignFromString(rowData.getProgressive8()));
                intent.putExtra("PROGRESSIVE_9", removeDollarSignFromString(rowData.getProgressive9()));
                intent.putExtra("PROGRESSIVE_10", removeDollarSignFromString(rowData.getProgressive10()));
                intent.putExtra("NOTES", rowData.getNotes());
                intent.putExtra("DOCUMENT_ID", rowData.getDocumentId());
                intent.putExtra("position", position);
                intent.putExtra("data", (Serializable)rowData);
                startActivityForResult(intent, 666);
            }

            @Override
            public void onLongClick(View view, final int position) {
                final RowData rowData = rowDataList.get(position);
                AlertDialog alertDialog = new AlertDialog.Builder(DataReportActivity.this).create();
                alertDialog.setMessage("Are you sure you want to delete this scan?");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
                        (dialog, i) -> {
                            deleteScanFromDatabase(rowData.getDocumentId());
                            rowDataList.remove(position);
                            mAdapter.notifyItemRemoved(position);
                            mAdapter.notifyItemRangeChanged(position, rowDataList.size());
                            dialog.dismiss();
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL",
                        (dialog, i) -> dialog.dismiss());
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 666 && resultCode == RESULT_OK) {
            RowData rowData = (RowData)data.getSerializableExtra("updated_scan");
            int pos = data.getIntExtra("position", 0);
            mAdapter.setItem(pos, rowData);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.reports_action_bar, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = findViewById(R.id.search_view);
        searchView.setMenuItem(searchItem);

        final List<RowData> backupData = new ArrayList<>();

        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.trim().isEmpty()) {
                    progressBar.setVisibility(View.VISIBLE);
                    CollectionReference usersCollection = database.collection("users")
                            .document(firebaseAuth.getCurrentUser().getUid())
                            .collection("scans");
                    Query scansQuery = usersCollection
                            .whereGreaterThanOrEqualTo("machine_id", newText.trim())
                            .whereLessThanOrEqualTo("machine_id", newText.trim() + "\uF7FF")
                            .orderBy("machine_id")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .limit(10);
                    scansQuery.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            prepareData(task.getResult(), false);
                        } else {
                            showToast("Unable to refresh.  Check your connection.");
                        }
                        progressBar.setVisibility(View.GONE);
                    });
                }
                return false;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                if (backupData.size() == 0) {
                    backupData.addAll(rowDataList);
                }
            }

            @Override
            public void onSearchViewClosed() {
                rowDataList.clear();
                rowDataList.addAll(backupData);
                mAdapter.notifyDataSetChanged();
            }
        });

        return true;
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
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

    private void loadMoreRecords() {
        Date time = new Date(System.currentTimeMillis() - offset * 1000);

        CollectionReference usersCollection = database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("scans");

        Query query = usersCollection
                .whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastDocumentSnapshot)
                .limit(QUERY_LIMIT_SIZE);

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                prepareData(task.getResult(), true);
            } else {
                showToast("Unable to refresh.  Check your connection.");
            }
            LOADING = true;
        });
    }

    private void deleteScanFromDatabase(String document_id) {
        // Old sub collection
        database.collection("scans").document(document_id).delete();

        // New sub collection
        database.collection("users").document(firebaseAuth.getCurrentUser().getUid()).collection("scans").document(document_id).delete();
    }

    private void executeQuery(DateRange dateRange) {
        Date time = new Date(System.currentTimeMillis() - offset * 1000);
        CollectionReference usersCollection = database.collection("users")
                .document(firebaseAuth.getCurrentUser().getUid())
                .collection("scans");
        Query query = usersCollection.whereGreaterThan("timestamp", time)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(QUERY_LIMIT_SIZE);
        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                prepareData(task.getResult(), false);
            } else {
                showToast("Unable to refresh.  Check your connection.");
            }
            swipeRefreshLayout.setRefreshing(false);
        });
    }

    private void prepareData(QuerySnapshot snapshot, boolean update) {

        String machine_id, user, notes;
        Timestamp timestamp;
        String progressive1, progressive2, progressive3, progressive4, progressive5, progressive6, progressive7, progressive8, progressive9, progressive10;
        progressive1 = progressive2 = progressive3 = progressive4 = progressive5 = progressive6 = progressive7 = progressive8 = progressive9 = progressive10 = "";
        String location = "";
        RowData rowData;
        if (!update) {
            rowDataList.clear(); // reset the current data list
        }

        for (QueryDocumentSnapshot document : snapshot) {

            lastDocumentSnapshot = document;

            Map<String, Object> map = document.getData();
            if (map.containsKey("progressive1")) {
                progressive1 = (String)map.get("progressive1");
            }
            if (map.containsKey("progressive2")) {
                progressive2 = (String)map.get("progressive2");
            }
            if (map.containsKey("progressive3")) {
                progressive3 = (String)map.get("progressive3");
            }
            if (map.containsKey("progressive4")) {
                progressive4 = (String)map.get("progressive4");
            }
            if (map.containsKey("progressive5")) {
                progressive5 = (String)map.get("progressive5");
            }
            if (map.containsKey("progressive6")) {
                progressive6 = (String)map.get("progressive6");
            }
            if (map.containsKey("progressive7")) {
                progressive7 = (String)map.get("progressive7");
            }
            if (map.containsKey("progressive8")) {
                progressive8 = (String)map.get("progressive8");
            }
            if (map.containsKey("progressive9")) {
                progressive9 = (String)map.get("progressive9");
            }
            if (map.containsKey("progressive10")) {
                progressive10 = (String)map.get("progressive10");
            }

            if (map.containsKey("location")) {
                location = (String)map.get("location");
            }

            Date date = new Date();
            machine_id = document.get("machine_id").toString();
            timestamp = document.getTimestamp("timestamp");
            if (timestamp != null) {
                date = timestamp.toDate();
            }
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.US);
            String formattedTimestamp = simpleDateFormat.format(date);
            user = (document.get("userName") == null) ? "User not specified" : document.get("userName").toString();
            notes = (document.get("notes") == null) ? "" : document.get("notes").toString().trim();

            rowData = new RowData(document.getId(),
                    machine_id,
                    formattedTimestamp,
                    user,
                    progressive1,
                    progressive2,
                    progressive3,
                    progressive4,
                    progressive5,
                    progressive6,
                    progressive7,
                    progressive8,
                    progressive9,
                    progressive10,
                    location,
                    notes,
                    false);

            rowDataList.add(rowData);
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

    private String removeDollarSignFromString(String text) {
        text = text.replace("$", "");
        return text;
    }

    private String createCsvFile2(QuerySnapshot snapshot) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\"Machine ID\",\"Progressive Index\",\"Progressive Value\",\"Base\",\"Timestamp\",\"User\",\"Notes\",\"Location\"\n");

        for (QueryDocumentSnapshot document : snapshot) {
            Map<String, Object> data = document.getData();

            String machine_id = data.containsKey("machine_id") ? (String)data.get("machine_id") : "";
            String timestamp = data.containsKey("timestamp") ? ((Timestamp)data.get("timestamp")).toDate().toString() : "";
            String user = data.containsKey("userName") ? (String)data.get("userName") : "";
            String notes = data.containsKey("notes") ? (String)data.get("notes") : "";
            notes = notes.replaceAll("\"", "");
            String location = data.containsKey("location") ? (String)data.get("location") : "";

            String[] progressives = {
                    data.containsKey("progressive1") ? (String)data.get("progressive1") : "",
                    data.containsKey("progressive2") ? (String)data.get("progressive2") : "",
                    data.containsKey("progressive3") ? (String)data.get("progressive3") : "",
                    data.containsKey("progressive4") ? (String)data.get("progressive4") : "",
                    data.containsKey("progressive5") ? (String)data.get("progressive5") : "",
                    data.containsKey("progressive6") ? (String)data.get("progressive6") : "",
                    data.containsKey("progressive7") ? (String)data.get("progressive7") : "",
                    data.containsKey("progressive8") ? (String)data.get("progressive8") : "",
                    data.containsKey("progressive9") ? (String)data.get("progressive9") : "",
                    data.containsKey("progressive10") ? (String)data.get("progressive10") : "",
            };
            String[] bases = {
                    data.containsKey("base1") ? (String)data.get("base1") : "",
                    data.containsKey("base2") ? (String)data.get("base2") : "",
                    data.containsKey("base3") ? (String)data.get("base3") : "",
                    data.containsKey("base4") ? (String)data.get("base4") : "",
                    data.containsKey("base5") ? (String)data.get("base5") : "",
                    data.containsKey("base6") ? (String)data.get("base6") : "",
                    data.containsKey("base7") ? (String)data.get("base7") : "",
                    data.containsKey("base8") ? (String)data.get("base8") : "",
                    data.containsKey("base9") ? (String)data.get("base9") : "",
                    data.containsKey("base10") ? (String)data.get("base10") : "",
            };

            for (int i = 0; i < 10; i++) {
                if (!progressives[i].trim().isEmpty()) {
                    stringBuilder.append("\"").append(machine_id).append("\",").append("\"").append(i+1).append("\",").append("\"").append(progressives[i]).append("\",").append("\"").append(bases[i]).append("\",").append("\"").append(timestamp).append("\",").append("\"").append(user).append("\",").append("\"").append(notes).append("\",").append("\"").append(location).append("\"\n");
                }
            }
        }

        return stringBuilder.toString();
    }

    private String createCsvFile(QuerySnapshot snapshot) {
        StringBuilder stringBuilder = new StringBuilder();
        if (NUMBER_OF_PROGRESSIVES == 6) {
            stringBuilder.append("\"Machine\",\"Progressive1\",\"Progressive2\",\"Progressive3\",\"Progressive4\",\"Progressive5\",\"Progressive6\",\"Location\",\"Notes\",\"Date\",\"User\"\n");
        } else if (NUMBER_OF_PROGRESSIVES == 7) {
            stringBuilder.append("\"Machine\",\"Progressive1\",\"Progressive2\",\"Progressive3\",\"Progressive4\",\"Progressive5\",\"Progressive6\",\"Progressive7\",\"Location\",\"Notes\",\"Date\",\"User\"\n");
        } else if (NUMBER_OF_PROGRESSIVES == 8) {
            stringBuilder.append("\"Machine\",\"Progressive1\",\"Progressive2\",\"Progressive3\",\"Progressive4\",\"Progressive5\",\"Progressive6\",\"Progressive7\",\"Progressive8\",\"Location\",\"Notes\",\"Date\",\"User\"\n");
        } else if (NUMBER_OF_PROGRESSIVES == 9) {
            stringBuilder.append("\"Machine\",\"Progressive1\",\"Progressive2\",\"Progressive3\",\"Progressive4\",\"Progressive5\",\"Progressive6\",\"Progressive7\",\"Progressive8\",\"Progressive9\",\"Location\",\"Notes\",\"Date\",\"User\"\n");
        } else if (NUMBER_OF_PROGRESSIVES == 10) {
            stringBuilder.append("\"Machine\",\"Progressive1\",\"Progressive2\",\"Progressive3\",\"Progressive4\",\"Progressive5\",\"Progressive6\",\"Progressive7\",\"Progressive8\",\"Progressive9\",\"Progressive10\",\"Location\",\"Notes\",\"Date\",\"User\"\n");
        }

        String progressive1, progressive2, progressive3, progressive4, progressive5, progressive6, progressive7, progressive8, progressive9, progressive10;
        progressive1 = progressive2 = progressive3 = progressive4 = progressive5 = progressive6 = progressive7 = progressive8 = progressive9 = progressive10 = "";

        for (QueryDocumentSnapshot document : snapshot) {
            String machine_id = document.get("machine_id").toString();
            String timestamp = document.getTimestamp("timestamp").toDate().toString();
            String user = (document.get("userName") == null) ? "User not specified" : document.get("userName").toString();
            if (NUMBER_OF_PROGRESSIVES == 6) {
                progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
                progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
                progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
                progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
                progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
                progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
            } else if (NUMBER_OF_PROGRESSIVES == 7) {
                progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
                progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
                progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
                progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
                progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
                progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
                progressive7 = document.get("progressive7").toString().trim().isEmpty() ? "" : "$" + document.get("progressive7").toString().trim();
            } else if (NUMBER_OF_PROGRESSIVES == 8) {
                progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
                progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
                progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
                progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
                progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
                progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
                progressive7 = document.get("progressive7").toString().trim().isEmpty() ? "" : "$" + document.get("progressive7").toString().trim();
                progressive8 = document.get("progressive8").toString().trim().isEmpty() ? "" : "$" + document.get("progressive8").toString().trim();
            } else if (NUMBER_OF_PROGRESSIVES == 9) {
                progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
                progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
                progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
                progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
                progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
                progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
                progressive7 = document.get("progressive7").toString().trim().isEmpty() ? "" : "$" + document.get("progressive7").toString().trim();
                progressive8 = document.get("progressive8").toString().trim().isEmpty() ? "" : "$" + document.get("progressive8").toString().trim();
                progressive9 = document.get("progressive9").toString().trim().isEmpty() ? "" : "$" + document.get("progressive9").toString().trim();
            } else if (NUMBER_OF_PROGRESSIVES == 10) {
                progressive1 = document.get("progressive1").toString().trim().isEmpty() ? "" : "$" + document.get("progressive1").toString().trim();
                progressive2 = document.get("progressive2").toString().trim().isEmpty() ? "" : "$" + document.get("progressive2").toString().trim();
                progressive3 = document.get("progressive3").toString().trim().isEmpty() ? "" : "$" + document.get("progressive3").toString().trim();
                progressive4 = document.get("progressive4").toString().trim().isEmpty() ? "" : "$" + document.get("progressive4").toString().trim();
                progressive5 = document.get("progressive5").toString().trim().isEmpty() ? "" : "$" + document.get("progressive5").toString().trim();
                progressive6 = document.get("progressive6").toString().trim().isEmpty() ? "" : "$" + document.get("progressive6").toString().trim();
                progressive7 = document.get("progressive7").toString().trim().isEmpty() ? "" : "$" + document.get("progressive7").toString().trim();
                progressive8 = document.get("progressive8").toString().trim().isEmpty() ? "" : "$" + document.get("progressive8").toString().trim();
                progressive9 = document.get("progressive9").toString().trim().isEmpty() ? "" : "$" + document.get("progressive9").toString().trim();
                progressive10 = document.get("progressive10").toString().trim().isEmpty() ? "" : "$" + document.get("progressive10").toString().trim();
            }
            //
            //
            String notes = (document.get("notes") == null) ? "" : document.get("notes").toString().trim();
            String location = (document.get("location") == null) ? "" : document.get("location").toString().trim();

            stringBuilder.append("\"" + machine_id + "\",");
            if (NUMBER_OF_PROGRESSIVES == 6) {
                stringBuilder.append("\"" + progressive1 + "\",");
                stringBuilder.append("\"" + progressive2 + "\",");
                stringBuilder.append("\"" + progressive3 + "\",");
                stringBuilder.append("\"" + progressive4 + "\",");
                stringBuilder.append("\"" + progressive5 + "\",");
                stringBuilder.append("\"" + progressive6 + "\",");
            } else if (NUMBER_OF_PROGRESSIVES == 7) {
                stringBuilder.append("\"" + progressive1 + "\",");
                stringBuilder.append("\"" + progressive2 + "\",");
                stringBuilder.append("\"" + progressive3 + "\",");
                stringBuilder.append("\"" + progressive4 + "\",");
                stringBuilder.append("\"" + progressive5 + "\",");
                stringBuilder.append("\"" + progressive6 + "\",");
                stringBuilder.append("\"" + progressive7 + "\",");
            } else if (NUMBER_OF_PROGRESSIVES == 8) {
                stringBuilder.append("\"" + progressive1 + "\",");
                stringBuilder.append("\"" + progressive2 + "\",");
                stringBuilder.append("\"" + progressive3 + "\",");
                stringBuilder.append("\"" + progressive4 + "\",");
                stringBuilder.append("\"" + progressive5 + "\",");
                stringBuilder.append("\"" + progressive6 + "\",");
                stringBuilder.append("\"" + progressive7 + "\",");
                stringBuilder.append("\"" + progressive8 + "\",");
            } else if (NUMBER_OF_PROGRESSIVES == 9) {
                stringBuilder.append("\"" + progressive1 + "\",");
                stringBuilder.append("\"" + progressive2 + "\",");
                stringBuilder.append("\"" + progressive3 + "\",");
                stringBuilder.append("\"" + progressive4 + "\",");
                stringBuilder.append("\"" + progressive5 + "\",");
                stringBuilder.append("\"" + progressive6 + "\",");
                stringBuilder.append("\"" + progressive7 + "\",");
                stringBuilder.append("\"" + progressive8 + "\",");
                stringBuilder.append("\"" + progressive9 + "\",");
            } else if (NUMBER_OF_PROGRESSIVES == 10) {
                stringBuilder.append("\"" + progressive1 + "\",");
                stringBuilder.append("\"" + progressive2 + "\",");
                stringBuilder.append("\"" + progressive3 + "\",");
                stringBuilder.append("\"" + progressive4 + "\",");
                stringBuilder.append("\"" + progressive5 + "\",");
                stringBuilder.append("\"" + progressive6 + "\",");
                stringBuilder.append("\"" + progressive7 + "\",");
                stringBuilder.append("\"" + progressive8 + "\",");
                stringBuilder.append("\"" + progressive9 + "\",");
                stringBuilder.append("\"" + progressive10 + "\",");
            }
            stringBuilder.append("\"" + location + "\",");
            stringBuilder.append("\"" + notes + "\",");
            stringBuilder.append("\"" + timestamp + "\",");
            stringBuilder.append("\"" + user + "\"\n");
        }
        return stringBuilder.toString();
    }

    private String createReportTitle() {
        DateFormat dateFormat = new SimpleDateFormat("MMMM_dd,_yyyy", Locale.US);
        Date date = new Date();
        return dateFormat.format(date) + "_Report.csv";
    }

    public void generateReport(View view) {

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        final String report_recipient_emails = sharedPreferences.getString("email_recipient", "");
        NUMBER_OF_PROGRESSIVES = sharedPreferences.getInt("number_of_progressives", 6);

        final String[] emails = report_recipient_emails.split(",");
        final File csvFile = new File(getFilesDir(), createReportTitle());

        if (isExternalStorageWritable()) {
            Date time = new Date(System.currentTimeMillis() - offset * 1000);
            CollectionReference usersCollection = database.collection("users")
                    .document(firebaseAuth.getCurrentUser().getUid())
                    .collection("scans");
            Query query = usersCollection
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
                            if (!report_recipient_emails.isEmpty()) {
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
