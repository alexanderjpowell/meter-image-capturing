package com.slotmachine.ocr.mic;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodoListActivity extends AppCompatActivity {

    public static final int SUBMIT_PROGRESSIVE_RECORD = 0;

    private List<ToDoListData> toDoDataList;
    private List<ToDoListData> toDoDataListAll;
    private RecyclerView recyclerView;
    private ToDoListDataAdapter mAdapter;
    private enum EmptyState { NO_FILE_UPLOAD, ALL_COMPLETED, NONE_COMPLETED, NORMAL }

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore database;

    private SwipeRefreshLayout swipeRefreshLayout;

    private DocumentReference uploadFormDataDocumentReference;

    private TextView empty_state_text_view;
    private TextView empty_state_completed_text_view;
    private TextView empty_state_uncompleted_text_view;
    private ProgressBar progressBar;

    private MaterialSearchView searchView;

    private SharedPreferences sharedPref;
    private int SORT_BY_FIELD;

    private String TAG = "TodoListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_list);

        Toolbar mTopToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        if (firebaseAuth.getCurrentUser() == null) {
            startActivity(new Intent(TodoListActivity.this, LoginActivity.class));
            finish();
            return;
        }

        sharedPref = getApplicationContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SORT_BY_FIELD = sharedPref.getInt("SORT_BY_FIELD", 0); // 0 is original, 1 machine_id, 2 is location

        empty_state_text_view = findViewById(R.id.empty_state_no_file_text_view);
        empty_state_completed_text_view = findViewById(R.id.empty_state_completed_text_view);
        empty_state_uncompleted_text_view = findViewById(R.id.empty_state_uncompleted_text_view);
        progressBar = findViewById(R.id.progress_bar);

        database = FirebaseFirestore.getInstance();
        uploadFormDataDocumentReference = database.collection("formUploads")
                .document(firebaseAuth.getCurrentUser().getUid());

        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        // This method performs the actual data-refresh operation.
                        // The method calls setRefreshing(false) when it's finished.
                        populateRecyclerView("");
                    }
                }
        );

        recyclerView = findViewById(R.id.recycler_view_to_do_list);
        toDoDataList = new ArrayList<>();
        toDoDataListAll = new ArrayList<>();
        mAdapter = new ToDoListDataAdapter(TodoListActivity.this, toDoDataList);

        final LinearLayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        recyclerView.setAdapter(mAdapter);

        recyclerView.addOnItemTouchListener(new RecyclerTouchListener(getApplicationContext(), recyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                //
                String machineId = toDoDataList.get(position).getMachineId();
                int positionAll = getListPositionFromMachineId(machineId);
                //
                Intent intent = new Intent(TodoListActivity.this, MainActivity.class);
                intent.putExtra("machine_id", machineId);
                intent.putExtra("numberOfProgressives", toDoDataList.get(position).getDescriptionsLength());
                intent.putExtra("location", toDoDataList.get(position).getLocation());
                intent.putExtra("position", positionAll);
                ArrayList<String> progressiveDescriptionTitlesList = toDoDataList.get(position).getProgressiveDescriptionsList();
                intent.putStringArrayListExtra("progressiveDescriptionTitles", progressiveDescriptionTitlesList);
                //ArrayList<String> resetValuesList = toDoDataList.get(position).getResetValuesList();
                //if (resetValuesList != null) {
                    //intent.putStringArrayListExtra("resetValuesArray", toDoDataList.get(position).getResetValuesList());
                //}
                ArrayList<String> baseValuesList = toDoDataList.get(position).getBaseValuesList();
                if (baseValuesList != null) {
                    intent.putStringArrayListExtra("baseValuesArray", toDoDataList.get(position).getBaseValuesList());
                }
                ArrayList<String> incrementValuesList = toDoDataList.get(position).getIncrementValuesList();
                if (incrementValuesList != null) {
                    intent.putStringArrayListExtra("incrementValuesArray", toDoDataList.get(position).getIncrementValuesList());
                }
                intent.putExtra("hashMap", (HashMap)toDoDataList.get(position).getMap());
                startActivityForResult(intent, SUBMIT_PROGRESSIVE_RECORD);
            }

            @Override
            public void onLongClick(View view, final int position) {
                // TODO
            }
        }));

        populateRecyclerView("");
    }

    private int getListPositionFromMachineId(String id) {
        for (int i = 0; i < toDoDataListAll.size(); i++) {
            if (toDoDataListAll.get(i).getMachineId().equals(id)) {
                return i;
            }
        }
        return 0;
    }

    private void populateRecyclerView(final String machineIdQuery) {
        recyclerView.setVisibility(View.GONE);
        empty_state_text_view.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        toDoDataListAll.clear();
        toDoDataList.clear();
        uploadFormDataDocumentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if ((document != null) && document.exists()) {
                        if (document.contains("uploadArray")) {

                            List<Map<String, Object>> mapList = (List<Map<String, Object>>)document.get("uploadArray");

                            for (int i = 0; i < mapList.size(); i++) {
                                Map<String, Object> map = mapList.get(i);
                                ToDoListData row = new ToDoListData(i, map);
                                if (machineIdQuery.isEmpty()) {
                                    toDoDataList.add(row);
                                } else {
                                    if (row.getMachineId().equals(machineIdQuery)) {
                                        toDoDataList.add(row);
                                    }
                                }
                            }
                            //
                            if (SORT_BY_FIELD == 0) {
                                Collections.sort(toDoDataList, ToDoListData.indexComparator);
                            } else if (SORT_BY_FIELD == 1) {
                                Collections.sort(toDoDataList, ToDoListData.machineIdComparator);
                            } else if (SORT_BY_FIELD == 2) {
                                Collections.sort(toDoDataList, ToDoListData.locationComparator);
                            }
                            toDoDataListAll.clear();
                            toDoDataListAll.addAll(toDoDataList);
                            mAdapter.notifyDataSetChanged();
                            //
                            toggleEmptyStateDisplays(EmptyState.NORMAL);
                        }
                        mAdapter.notifyDataSetChanged();
                    } else {
                        toggleEmptyStateDisplays(EmptyState.NO_FILE_UPLOAD);
                        Log.d(TAG, "No such document");
                    }
                } else {
                    showToast(task.getException().getMessage());
                    Log.d(TAG, "get failed with ", task.getException());
                }

                // Finish things up
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void toggleEmptyStateDisplays(EmptyState state) {
        if (state.equals(EmptyState.NO_FILE_UPLOAD)) {
            recyclerView.setVisibility(View.GONE);
            empty_state_text_view.setVisibility(View.VISIBLE);
            empty_state_completed_text_view.setVisibility(View.GONE);
            empty_state_uncompleted_text_view.setVisibility(View.GONE);
        } else if (state.equals(EmptyState.ALL_COMPLETED)) {
            recyclerView.setVisibility(View.GONE);
            empty_state_text_view.setVisibility(View.GONE);
            empty_state_completed_text_view.setVisibility(View.VISIBLE);
            empty_state_uncompleted_text_view.setVisibility(View.GONE);
        } else if (state.equals(EmptyState.NONE_COMPLETED)) {
            recyclerView.setVisibility(View.GONE);
            empty_state_text_view.setVisibility(View.GONE);
            empty_state_completed_text_view.setVisibility(View.GONE);
            empty_state_uncompleted_text_view.setVisibility(View.VISIBLE);
        } else if (state.equals(EmptyState.NORMAL)) {
            recyclerView.setVisibility(View.VISIBLE);
            empty_state_text_view.setVisibility(View.GONE);
            empty_state_completed_text_view.setVisibility(View.GONE);
            empty_state_uncompleted_text_view.setVisibility(View.GONE);
        }
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
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == RESULT_OK) {
            if (requestCode == SUBMIT_PROGRESSIVE_RECORD) {
                //showToast(Integer.toString(toDoDataListAll.size()));
                int recyclerViewPosition = intent.getIntExtra("position", 0);
                //previousMachineId = intent.getStringExtra("machine_id");

                if (searchView.hasFocus()) {
                    searchView.clearFocus();
                    searchView.closeSearch();
                }

                //showToast(Integer.toString(recyclerViewPosition));
                recyclerView.scrollToPosition(recyclerViewPosition);
                removeRowFromRecyclerView(recyclerViewPosition);
            }
        }
    }

    private void removeRowFromRecyclerView(int position) {
        toDoDataList.remove(position);
        mAdapter.notifyDataSetChanged();
        syncDataLists();
    }

    private void syncDataLists() {
        toDoDataListAll.clear();
        toDoDataListAll.addAll(toDoDataList);
    }

    /*private void removeMachineIdFromRecyclerView(String machineId) {
        int pos = getDataListPositionFromMachineId(machineId);
        toDoDataList.remove(pos);
        mAdapter.notifyDataSetChanged();
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.to_do_list_action_bar, menu);

        //
        MenuItem searchItem = menu.findItem(R.id.action_search);
        //MenuItem sortMachineIdItem = menu.findItem(R.id.sort_machine_id);
        //MenuItem sortLocationItem = menu.findItem(R.id.sort_location);



        searchView = findViewById(R.id.search_view);
        //final EditText editText = searchView.findViewById(com.miguelcatalan.materialsearchview.R.id.searchTextView);
        //editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        searchView.setMenuItem(searchItem);
        //
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //Do some magic
                //Toast.makeText(getApplicationContext(), "onQueryTextSubmit", Toast.LENGTH_SHORT).show();
                //doSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!newText.trim().isEmpty()) {
                    doSearch(newText);
                } else {
                    revertRecyclerViewToNormal();
                }
                return false;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {
            }

            @Override
            public void onSearchViewClosed() {
            }
        });

        return true;
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            if (query != null) {
                searchView.setQuery(query.replaceAll("\\s+", ""), false);
            }
        }
        super.onNewIntent(intent);
    }

    private void revertRecyclerViewToNormal() {
        toDoDataList.clear();
        toDoDataList.addAll(toDoDataListAll);
        mAdapter.notifyDataSetChanged();
    }

    private void doSearch(String searchPattern) {
        List<ToDoListData> filteredList = new ArrayList<>();
        for (ToDoListData i : toDoDataListAll) {
            if (i.getMachineId().startsWith(searchPattern.trim())) {
                filteredList.add(i);
            }
        }
        toDoDataList.clear();
        toDoDataList.addAll(filteredList);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        SharedPreferences.Editor editor = sharedPref.edit();
        switch (item.getItemId()) {
            case R.id.sort_upload_file:
                //
                editor.putInt("SORT_BY_FIELD", 0);
                editor.apply();
                //
                Collections.sort(toDoDataList, ToDoListData.indexComparator);
                toDoDataListAll.clear();
                toDoDataListAll.addAll(toDoDataList);
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.sort_machine_id:
                //
                editor.putInt("SORT_BY_FIELD", 1);
                editor.apply();
                //
                Collections.sort(toDoDataList, ToDoListData.machineIdComparator);
                toDoDataListAll.clear();
                toDoDataListAll.addAll(toDoDataList);
                mAdapter.notifyDataSetChanged();
                return true;
            case R.id.sort_location:
                //
                editor.putInt("SORT_BY_FIELD", 2);
                editor.apply();
                //
                Collections.sort(toDoDataList, ToDoListData.locationComparator);
                toDoDataListAll.clear();
                toDoDataListAll.addAll(toDoDataList);
                mAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
}
