package com.slotmachine.ocr.mic;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.functions.FirebaseFunctions;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.slotmachine.ocr.mic.model.ToDoListItem;
import com.slotmachine.ocr.mic.viewmodel.ToDoListViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class TodoListActivity extends AppCompatActivity implements OnAdapterItemClickListener {

    private List<ToDoListItem> toDoDataList;
    private List<ToDoListItem> toDoDataListAll;
    private RecyclerView recyclerView;
    private ToDoListDataAdapter mAdapter;
    private String mFirebaseUserId;

    private ConstraintLayout constraintLayout;
    private TextView statusTextview;

    private FirebaseFirestore database;

    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private MaterialSearchView searchView;

    private ToDoListViewModel toDoListViewModel;

    private FirebaseFunctions mFunctions;
    private String latestCollectionId;
    private DocumentSnapshot snapshotCursor;
    private boolean LOADING = true;
    private String orderBy;
    private final int QUERY_BATCH_SIZE = 100;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent resultIntent = result.getData();
                    if (resultIntent != null && resultIntent.hasExtra("position")) {
                        int position = resultIntent.getIntExtra("position", 0);
                        ToDoListItem item = (ToDoListItem)resultIntent.getSerializableExtra("toDoItem");
                        toDoDataList.remove(position);
                        mAdapter.notifyItemRemoved(position);
                        // Also remove from backuplist
                        toDoDataListAll.remove(item);

                        String docId = item.getDocumentId();
                        if (docId != null) {
                            toDoListViewModel.setItemAsScanned(mFirebaseUserId, docId);
                        }

                        if (toDoDataListAll.isEmpty()) {
                            setEmptyView();

                            // Call firebase function to set all docs back to unscanned
                            Map<String, Object> data = new HashMap<>();
                            data.put("doc_id", latestCollectionId);
                            mFunctions.getHttpsCallable("resetUploadFile").call(data);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null || firebaseAuth.getUid() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        mFirebaseUserId = firebaseAuth.getUid();

        mFunctions = FirebaseFunctions.getInstance();

        orderBy = UserSettings.getToDoListOrderByField(this);

        progressBar = findViewById(R.id.progress_bar);
        constraintLayout = findViewById(R.id.constraint_layout);
        statusTextview = findViewById(R.id.status_textview);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> queryDatabase(orderBy));

        database = FirebaseFirestore.getInstance();

        recyclerView = findViewById(R.id.recycler_view);
        toDoDataList = new ArrayList<>();
        toDoDataListAll = new ArrayList<>();
        mAdapter = new ToDoListDataAdapter(TodoListActivity.this, toDoDataList, this);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        toDoListViewModel = new ViewModelProvider(this).get(ToDoListViewModel.class);
        getToDoMachines();

        RecyclerView.OnScrollListener onScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull @NotNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    int visibleItemCount = mLayoutManager.getChildCount();
                    int totalItemCount = mLayoutManager.getItemCount();
                    int pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();

                    if (LOADING && (pastVisibleItems + visibleItemCount >= totalItemCount)) {
                        LOADING = false;
                        loadMoreDocs();
                    }
                }
            }
        };

        recyclerView.addOnScrollListener(onScrollListener);
    }

    public void loadMoreDocs() {
        if (snapshotCursor != null) {
            database.collection("toDoFileData")
                    .document(mFirebaseUserId)
                    .collection("files")
                    .document(latestCollectionId)
                    .collection("machines")
                    .whereEqualTo("isScanned", false)
                    .orderBy(orderBy)
                    .startAfter(snapshotCursor)
                    .limit(QUERY_BATCH_SIZE)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.size() > 0) {
                            snapshotCursor = queryDocumentSnapshots.getDocuments()
                                    .get(queryDocumentSnapshots.size() - 1);
                        } else {
                            snapshotCursor = null;
                        }
                        List<ToDoListItem> tmp = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            tmp.add(doc.toObject(ToDoListItem.class));
                        }
                        toDoDataList.addAll(tmp);
                        toDoDataListAll.addAll(tmp);
                        mAdapter.notifyDataSetChanged();
                        LOADING = true;
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show();
                        Timber.e(e);
                    });
        }
    }

    @Override
    public void onItemClick(int position) {
        try {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("position", position);
            intent.putExtra("toDoItem", toDoDataList.get(position));
            activityResultLauncher.launch(intent);
        } catch (Exception ex) {
            Toast.makeText(this, "Error retrieving machine data", Toast.LENGTH_LONG).show();
        }
    }

    private void getToDoMachines() {
        progressBar.setVisibility(View.VISIBLE);

        toDoListViewModel.getLatestUploadCollectionId(mFirebaseUserId).observe(this, doc -> {
            if (doc == null) {
                constraintLayout.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
                statusTextview.setText("No file has been upload for your account");
            } else {
                latestCollectionId = doc.getId();
                boolean initialized = doc.get("initializedScanning") != null ? (Boolean) doc.get("initializedScanning") : false;
                if (initialized) {
                    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setMessage("You are about to begin processing your account's to do checklist");
                    alertDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE, "Go back",
                            (dialog, i) -> onBackPressed());
                    alertDialog.setButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE, "OK",
                            (dialog, i) -> dialog.dismiss());
                    alertDialog.show();
                }
                queryDatabase(orderBy);
            }
            progressBar.setVisibility(View.GONE);
        });
    }

    private void queryDatabase(String orderBy) {
        database.collection("toDoFileData")
                .document(mFirebaseUserId)
                .collection("files")
                .document(latestCollectionId)
                .collection("machines")
                .whereEqualTo("isScanned", false)
                .orderBy(orderBy)
                .limit(QUERY_BATCH_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.size() > 0) {
                        snapshotCursor = queryDocumentSnapshots.getDocuments()
                                .get(queryDocumentSnapshots.size() - 1);
                    } else {
                        snapshotCursor = null;
                        setEmptyView();
                    }
                    List<ToDoListItem> tmp = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        tmp.add(doc.toObject(ToDoListItem.class));
                    }
                    toDoDataList.clear();
                    toDoDataList.addAll(tmp);

                    toDoDataListAll.clear();
                    toDoDataListAll.addAll(tmp);

                    mAdapter.notifyDataSetChanged();
                    recyclerView.smoothScrollToPosition(0);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Connection error", Toast.LENGTH_SHORT).show();
                    Timber.e(e);
                })
                .addOnCompleteListener(task -> swipeRefreshLayout.setRefreshing(false));
    }

    private void setEmptyView() {
        constraintLayout.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        statusTextview.setText("You have finished all assigned scans");
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.to_do_list_action_bar, menu);
        if (orderBy.equals(UserSettings.ORDER_BY_UPLOAD_FILE_POSITION)) {
            menu.findItem(R.id.sort_file_position).setChecked(true);
        } else if (orderBy.equals(UserSettings.ORDER_BY_MACHINE_ID)) {
            menu.findItem(R.id.sort_machine_id).setChecked(true);
        } else {
            menu.findItem(R.id.sort_location).setChecked(true);
        }

        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = findViewById(R.id.search_view);
        searchView.setMenuItem(searchItem);
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.isEmpty()) {
                    doSearch(newText);
                }
                return false;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
                // Get backup
                toDoDataListAll.clear();
                toDoDataListAll.addAll(toDoDataList);
            }

            @Override
            public void onSearchViewClosed() {
                // Reset adapter
                toDoDataList.clear();
                toDoDataList.addAll(toDoDataListAll);
                mAdapter.notifyDataSetChanged();
            }
        });

        return true;
    }

    private void doSearch(String searchPattern) {

        toDoListViewModel.searchForToDoItemsById(mFirebaseUserId,
                latestCollectionId,
                searchPattern).observe(this, docs -> {
                    toDoDataList.clear();
                    toDoDataList.addAll(docs);
                    mAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sort_file_position:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    orderBy = UserSettings.ORDER_BY_UPLOAD_FILE_POSITION;
                    UserSettings.setToDoListOrderByField(this, 0);
                    queryDatabase(orderBy);
                    return true;
                }
            case R.id.sort_machine_id:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    orderBy = UserSettings.ORDER_BY_MACHINE_ID;
                    UserSettings.setToDoListOrderByField(this, 1);
                    queryDatabase(orderBy);
                    return true;
                }
            case R.id.sort_location:
                if (!item.isChecked()) {
                    item.setChecked(true);
                    orderBy = UserSettings.ORDER_BY_LOCATION;
                    UserSettings.setToDoListOrderByField(this, 2);
                    queryDatabase(orderBy);
                    return true;
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}