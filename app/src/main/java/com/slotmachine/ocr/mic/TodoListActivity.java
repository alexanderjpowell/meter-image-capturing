package com.slotmachine.ocr.mic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.slotmachine.ocr.mic.adapter.DemoCollectionAdapter;
import com.slotmachine.ocr.mic.model.ToDoListItem;
import com.slotmachine.ocr.mic.viewmodel.ToDoListViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
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

    private MaterialSearchView searchView;

    private ToDoListViewModel toDoListViewModel;

    private FirebaseFunctions mFunctions;
    private String latestCollectionId;
    private DocumentSnapshot snapshotCursor;
    private boolean LOADING = true;
    private String orderBy;

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
                            constraintLayout.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            statusTextview.setText("You have finished all assigned scans");

                            // Call firebase function to set all docs back to unscanned
                            Map<String, Object> data = new HashMap<>();
                            data.put("doc_id", latestCollectionId);
                            mFunctions.getHttpsCallable("resetUploadFile").call(data);
                        }

//                        if (searchView.isSearchOpen()) {
//                            searchView.closeSearch();
//                        }
                    }

//                        int recyclerViewPosition = result.getData().getIntExtra("position", 0);
//                        if (searchView.hasFocus()) {
//                            searchView.clearFocus();
//                            searchView.closeSearch();
//                        }
//                        recyclerView.scrollToPosition(recyclerViewPosition);
//                        removeRowFromRecyclerView(recyclerViewPosition);
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

//        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
//            @Override
//            public void onClick(View view, int position) {
//                //
//                //
//                String machineId = toDoDataList.get(position).getMachine_id();
//                int positionAll = getListPositionFromMachineId(machineId);
//                //
//                Intent intent = new Intent(TodoListActivity.this, MainActivity.class);
//                intent.putExtra("machine_id", machineId);
//                intent.putExtra("numberOfProgressives", toDoDataList.get(position).getDescriptions().size());
//                intent.putExtra("location", toDoDataList.get(position).getLocation());
//                intent.putExtra("position", positionAll);
//                ArrayList<String> progressiveDescriptionTitlesList = toDoDataList.get(position).getProgressiveDescriptionsList();
//                intent.putStringArrayListExtra("progressiveDescriptionTitles", progressiveDescriptionTitlesList);
//                List<String> baseValuesList = toDoDataList.get(position).getBases();
//                if (baseValuesList != null) {
//                    intent.putStringArrayListExtra("baseValuesArray", (ArrayList<String>) toDoDataList.get(position).getBases());
//                }
//                List<String> incrementValuesList = toDoDataList.get(position).getIncrements();
//                if (incrementValuesList != null) {
//                    intent.putStringArrayListExtra("incrementValuesArray", (ArrayList<String>) toDoDataList.get(position).getIncrements());
//                }
//                intent.putExtra("hashMap", (HashMap)toDoDataList.get(position).getMap());
////                startActivityForResult(intent, SUBMIT_PROGRESSIVE_RECORD);
//
//                //
//                ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
//                        new ActivityResultContracts.StartActivityForResult(),
//                        result -> {
//                            if (result.getResultCode() == Activity.RESULT_OK) {
//                                int recyclerViewPosition = result.getData().getIntExtra("position", 0);
//                                if (searchView.hasFocus()) {
//                                    searchView.clearFocus();
//                                    searchView.closeSearch();
//                                }
//                                recyclerView.scrollToPosition(recyclerViewPosition);
//                                removeRowFromRecyclerView(recyclerViewPosition);
//                            }
//                        }
//                );
//
//                activityResultLauncher.launch(intent);
//            }
//
//            @Override
//            public void onLongClick(View view, final int position) {
//            }
//        }));

//        populateRecyclerView("");
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
                    .limit(10)
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
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("position", position);
        intent.putExtra("toDoItem", toDoDataList.get(position));
        activityResultLauncher.launch(intent);
    }

    private void getToDoMachines() {
//        toDoListViewModel.getAllUnscannedToDoItems(firebaseAuth.getUid()).observe(this, docs -> {
//            toDoDataList.clear();
//            toDoDataList.addAll(docs);
//
//            toDoDataListAll.clear();
//            toDoDataListAll.addAll(docs);
//
//            mAdapter.notifyDataSetChanged();
//        });

//        toDoListViewModel.userHasUploadFile(firebaseAuth.getUid()).observe(this, exists -> {
//            if (exists) {
//
//            }
//        });

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
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    snapshotCursor = queryDocumentSnapshots.getDocuments()
                            .get(queryDocumentSnapshots.size() - 1);
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
                });
    }

//    private void populateRecyclerView(final String machineIdQuery) {
//        progressBar.setVisibility(View.VISIBLE);
//        toDoDataListAll.clear();
//        toDoDataList.clear();
//        uploadFormDataDocumentReference.get().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                DocumentSnapshot document = task.getResult();
//                if ((document != null) && document.exists() && document.contains("uploadArray")) {
//                    List<Map<String, Object>> mapList = (List<Map<String, Object>>) document.get("uploadArray");
//
//                    for (int i = 0; i < mapList.size(); i++) {
//                        Map<String, Object> map = mapList.get(i);
//                        ToDoListItem row = new ToDoListItem(i, map);
//                        if (machineIdQuery.isEmpty()) {
//                            toDoDataList.add(row);
//                        } else {
//                            if (row.getMachineId().equals(machineIdQuery)) {
//                                toDoDataList.add(row);
//                            }
//                        }
//                    }
//                    if (SORT_BY_FIELD == 0) {
//                        Collections.sort(toDoDataList, ToDoListData.indexComparator);
//                    } else if (SORT_BY_FIELD == 1) {
//                        Collections.sort(toDoDataList, ToDoListData.machineIdComparator);
//                    } else if (SORT_BY_FIELD == 2) {
//                        Collections.sort(toDoDataList, ToDoListData.locationComparator);
//                    }
//                    toDoDataListAll.clear();
//                    toDoDataListAll.addAll(toDoDataList);
//                    mAdapter.notifyDataSetChanged();
//                }
//            } else {
//                showToast(task.getException().getMessage());
//            }
//
//            // Finish things up
//            progressBar.setVisibility(View.GONE);
//            swipeRefreshLayout.setRefreshing(false);
//        });
//    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
//        super.onActivityResult(requestCode, resultCode, intent);
//        if (resultCode == RESULT_OK) {
//            if (requestCode == SUBMIT_PROGRESSIVE_RECORD) {
//                int recyclerViewPosition = intent.getIntExtra("position", 0);
//                if (searchView.hasFocus()) {
//                    searchView.clearFocus();
//                    searchView.closeSearch();
//                }
//                recyclerView.scrollToPosition(recyclerViewPosition);
//                removeRowFromRecyclerView(recyclerViewPosition);
//            }
//        }
//    }

//    private void removeRowFromRecyclerView(int position) {
//        toDoDataList.remove(position);
//        mAdapter.notifyDataSetChanged();
//        syncDataLists();
//    }

//    private void syncDataLists() {
//        toDoDataListAll.clear();
//        toDoDataListAll.addAll(toDoDataList);
//    }

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

//    @Override
//    public void onNewIntent(Intent intent) {
//        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
//            String query = intent.getStringExtra(SearchManager.QUERY);
//            if (query != null) {
//                searchView.setQuery(query.replaceAll("\\s+", ""), false);
//            }
//        }
//        super.onNewIntent(intent);
//    }

//    private void revertRecyclerViewToNormal() {
//        toDoDataList.clear();
//        toDoDataList.addAll(toDoDataListAll);
//        mAdapter.notifyDataSetChanged();
//    }

    private void doSearch(String searchPattern) {

        toDoListViewModel.searchForToDoItemsById(mFirebaseUserId,
                latestCollectionId,
                searchPattern).observe(this, docs -> {
                    toDoDataList.clear();
                    toDoDataList.addAll(docs);
                    mAdapter.notifyDataSetChanged();
        });

//        List<ToDoListItem> filteredList = new ArrayList<>();
//        for (ToDoListItem i : toDoDataListAll) {
//            if (i.getMachine_id().startsWith(searchPattern.trim())) {
//                filteredList.add(i);
//            }
//        }
//        toDoDataList.clear();
//        toDoDataList.addAll(filteredList);
//        mAdapter.notifyDataSetChanged();
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